<template>
  <main class="simulation-room">
    <header class="simulation-room__header">
      <div class="room-brand"><span><ChatDotRound /></span><div><strong>综合模拟面试</strong><small>文字面试 · 全程连续</small></div></div>
      <div class="simulation-room__phase"><span>{{ stageLabel }}</span><i><b :style="{ width: `${progress}%` }"></b></i></div>
      <el-button :disabled="!session || session.status === 'FINISHED' || busy" :loading="finishing" @click="requestFinish"><SwitchButton />结束面试</el-button>
    </header>

    <section v-if="loading && !session" class="room-empty-state"><h1>正在恢复面试现场</h1><p>正在读取当前阶段与对话进度。</p></section>
    <section v-else-if="error && !session" class="room-empty-state"><h1>暂时无法进入这场面试</h1><p>{{ error }}</p><el-button @click="load">重新连接</el-button></section>
    <section v-else-if="session" class="simulation-room__body">
      <aside class="simulation-room__context">
        <p class="page-kicker">INTERVIEW FLOW</p>
        <ol><li v-for="item in flow" :key="item.key" :class="item.status"><span>{{ item.index }}</span><div><strong>{{ item.label }}</strong><small>{{ item.note }}</small></div><CircleCheck v-if="item.status === 'completed'" /></li></ol>
        <p>面试过程中只关注问题和回答。评分、证据与改进建议会在结束后统一呈现。</p>
      </aside>

      <section class="simulation-conversation">
        <div ref="messageList" class="simulation-message-list">
          <div class="simulation-transition"><span>{{ stageLabel }}</span><p>{{ stageOpening }}</p></div>
          <article v-for="message in messages" :key="message.key" :class="['simulation-message', `simulation-message--${message.role.toLowerCase()}`]">
            <span>{{ message.role === 'INTERVIEWER' ? '面' : '我' }}</span><div><small>{{ message.role === 'INTERVIEWER' ? '面试官' : '我的回答' }}</small><p>{{ message.content }}</p></div>
          </article>
          <article v-if="busy && !childFinished" class="simulation-message simulation-message--thinking"><span>面</span><div><small>面试官</small><p>正在听取你的回答...</p></div></article>
        </div>

        <p v-if="error" class="composer-error">{{ error }}</p>
        <div v-if="turnState === 'RETRYABLE_ERROR'" class="turn-recovery"><span>上一条回答没有处理完成，可以安全重试，不会重复推进面试。</span><el-button type="primary" :loading="submitting" @click="retry">重试回答</el-button></div>
        <div v-else-if="childFinished && session.currentStage !== 'FINISHED'" class="simulation-stage-ready"><CircleCheck /><div><strong>{{ stageLabel }}到这里结束</strong><small>{{ transitionText }}</small></div><el-button type="primary" :loading="advancing" @click="advance">{{ nextButtonText }}<ArrowRight /></el-button></div>
        <div v-else-if="session.status === 'FINISHED'" class="simulation-stage-ready"><CircleCheck /><div><strong>本场面试已结束</strong><small>复盘报告已经生成</small></div><el-button type="primary" @click="openReport">查看复盘</el-button></div>
        <div v-else class="simulation-composer">
          <el-input v-model="answer" type="textarea" :rows="5" resize="none" maxlength="12000" placeholder="像真实面试一样，直接说出你的判断和推导过程..." :disabled="busy || turnState !== 'IDLE'" @keydown.ctrl.enter.prevent="send" />
          <div><span>文字作答</span><el-button type="primary" :loading="submitting" :disabled="!answer.trim() || busy" @click="send">提交回答<Promotion /></el-button></div>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, CircleCheck, Promotion, SwitchButton } from '@element-plus/icons-vue'
import { advanceSimulation, finishSimulation, getSimulation, retrySimulationAnswer, submitSimulationAnswer } from '../api/simulationApi'
import type { SimulationSession } from '../model/types'
import type { ProjectInterviewSession, PublicInterviewTurn } from '@/features/project-deep-dive/model/types'
import type { AlgorithmSession, AlgorithmTurn } from '@/features/algorithm-practice/model/types'
import type { InterviewSession } from '@/api/modules/interview'

const props = defineProps<{ sessionId: string }>()
const id = Number(props.sessionId)
const router = useRouter()
const session = ref<SimulationSession | null>(null)
const answer = ref('')
const loading = ref(false); const submitting = ref(false); const advancing = ref(false); const finishing = ref(false)
const error = ref(''); const messageList = ref<HTMLElement | null>(null)
const busy = computed(() => loading.value || submitting.value || advancing.value || finishing.value || turnState.value === 'PROCESSING')
const stageLabel = computed(() => ({ PROJECT: '项目交流', KNOWLEDGE: '基础问答', ALGORITHM: '算法讨论', FINISHED: '面试结束' })[session.value?.currentStage ?? 'PROJECT'])
const stageOpening = computed(() => ({ PROJECT: '我们先从你的项目经历聊起。', KNOWLEDGE: '项目部分先到这里，接下来问几个基础问题。', ALGORITHM: '最后讨论一道算法题，请重点讲思路，不需要写代码。', FINISHED: '感谢你的回答，本场面试已经结束。' })[session.value?.currentStage ?? 'PROJECT'])
const progress = computed(() => ({ PROJECT: 18, KNOWLEDGE: 52, ALGORITHM: 78, FINISHED: 100 })[session.value?.currentStage ?? 'PROJECT'])
const data = computed(() => session.value?.stageData as any)
const turnState = computed(() => data.value?.turnState ?? 'IDLE')
const childFinished = computed(() => Boolean(data.value?.status === 'FINISHED'))
const flow = computed(() => [
  { key: 'PROJECT', index: '01', label: '项目交流', note: '真实经历与技术取舍' },
  { key: 'KNOWLEDGE', index: '02', label: '基础问答', note: 'Java 核心知识校准' },
  { key: 'ALGORITHM', index: '03', label: '算法讨论', note: '方案、复杂度与边界' },
].map((item, index) => ({ ...item, status: session.value?.currentStage === 'FINISHED' || index < currentIndex.value ? 'completed' : index === currentIndex.value ? 'active' : 'pending' })))
const currentIndex = computed(() => ['PROJECT', 'KNOWLEDGE', 'ALGORITHM'].indexOf(session.value?.currentStage ?? 'PROJECT'))
const transitionText = computed(() => session.value?.currentStage === 'ALGORITHM' ? '可以收束本场面试并查看复盘。' : '准备好后继续下一部分。')
const nextButtonText = computed(() => session.value?.currentStage === 'ALGORITHM' ? '结束并生成复盘' : '继续面试')
const messages = computed(() => {
  if (!session.value?.stageData) return []
  if (session.value.currentStage === 'PROJECT') return (session.value.stageData as ProjectInterviewSession).turns.filter(turn => turn.role !== 'SYSTEM').map(mapProjectTurn)
  if (session.value.currentStage === 'ALGORITHM') return (session.value.stageData as AlgorithmSession).turns.map(mapAlgorithmTurn)
  const knowledge = session.value.stageData as InterviewSession
  return knowledge.currentQuestion ? [{ key: `question-${knowledge.currentQuestion.questionId}`, id: knowledge.currentQuestion.questionId, role: 'INTERVIEWER' as const, content: knowledge.currentQuestion.questionText }] : []
})
const currentQuestionId = computed(() => {
  const last = [...messages.value].reverse().find(item => item.role === 'INTERVIEWER')
  if (session.value?.currentStage === 'KNOWLEDGE') return (session.value.stageData as InterviewSession)?.currentQuestion?.questionId ?? null
  return last?.id ?? null
})

onMounted(load)
async function load() { loading.value = true; error.value = ''; try { session.value = (await getSimulation(id)).data.data; if (session.value.status === 'FINISHED') await openReport(); await scrollBottom() } catch { error.value = '无法读取这场面试，请稍后重试。' } finally { loading.value = false } }
async function send() {
  if (!session.value || !answer.value.trim()) return
  submitting.value = true; error.value = ''
  try {
    session.value = (await submitSimulationAnswer(id, { clientTurnId: crypto.randomUUID(), questionTurnId: currentQuestionId.value, expectedChildVersion: session.value.currentStage === 'ALGORITHM' ? data.value.version : null, content: answer.value.trim(), inputModality: 'TEXT' })).data.data
    answer.value = ''; await scrollBottom()
  } catch { error.value = '回答没有处理完成，请刷新或安全重试。'; await load() }
  finally { submitting.value = false }
}
async function retry() { submitting.value = true; error.value = ''; try { session.value = (await retrySimulationAnswer(id)).data.data; await scrollBottom() } catch { error.value = '恢复失败，请稍后再试。' } finally { submitting.value = false } }
async function advance() { if (!session.value) return; advancing.value = true; try { session.value = (await advanceSimulation(id, session.value.version)).data.data; if (session.value.status === 'FINISHED') await openReport(); else await scrollBottom() } catch { error.value = '阶段切换失败，正在重新同步。'; await load() } finally { advancing.value = false } }
async function requestFinish() { try { await ElMessageBox.confirm('现在结束后，未进行的部分会标记为未覆盖，不会按零分处理。', '结束这场面试？', { confirmButtonText: '结束并生成复盘', cancelButtonText: '继续面试', type: 'warning' }) } catch { return } if (!session.value) return; finishing.value = true; try { session.value = (await finishSimulation(id, session.value.version)).data.data; await openReport() } catch { error.value = '暂时无法结束面试，请稍后重试。' } finally { finishing.value = false } }
async function openReport() { await router.replace(`/simulation/${id}/report`) }
function mapProjectTurn(turn: PublicInterviewTurn) { return { key: `project-${turn.turnId}`, id: turn.turnId, role: turn.role === 'CANDIDATE' ? 'CANDIDATE' as const : 'INTERVIEWER' as const, content: turn.content } }
function mapAlgorithmTurn(turn: AlgorithmTurn) { return { key: `algorithm-${turn.id}`, id: turn.id, role: turn.role, content: turn.content } }
async function scrollBottom() { await nextTick(); messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' }) }
</script>
