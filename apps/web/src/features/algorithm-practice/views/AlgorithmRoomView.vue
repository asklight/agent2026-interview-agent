<template>
  <main class="algorithm-room">
    <header class="algorithm-room__header">
      <div class="room-brand"><span><DataAnalysis /></span><div><strong>算法口述面试</strong><small>文字模式</small></div></div>
      <div class="algorithm-room__status"><i :class="connection"></i>{{ connectionLabel }}</div>
      <el-button :disabled="!session || session.status === 'FINISHED' || busy" :loading="finishing" @click="requestFinish">
        <SwitchButton />结束面试
      </el-button>
    </header>

    <section v-if="loading && !session" class="room-empty-state"><h1>正在恢复面试现场</h1><p>正在读取题目和对话进度。</p></section>
    <section v-else-if="error && !session" class="room-empty-state"><h1>暂时无法进入面试</h1><p>{{ error }}</p><el-button @click="load">重新连接</el-button></section>
    <section v-else-if="session" class="algorithm-room__body">
      <aside class="algorithm-question-panel">
        <span :class="`difficulty difficulty--${session.problem.difficulty}`">{{ difficultyLabel }}</span>
        <h1>{{ session.problem.title }}</h1>
        <p>{{ session.problem.statement }}</p>
        <details>
          <summary>题目约束</summary>
          <ul><li v-for="item in session.problem.constraints" :key="item">{{ item }}</li></ul>
        </details>
        <div class="algorithm-stage-progress">
          <span>面试进度</span><strong>{{ stageIndex }}/6</strong>
          <i><b :style="{ width: `${stageIndex / 6 * 100}%` }"></b></i>
        </div>
      </aside>

      <section class="algorithm-conversation">
        <div ref="messageList" class="algorithm-message-list">
          <article v-for="turn in session.turns" :key="turn.id" :class="['algorithm-message', `algorithm-message--${turn.role.toLowerCase()}`]">
            <span>{{ turn.role === 'INTERVIEWER' ? '官' : '我' }}</span>
            <div><small>{{ turn.role === 'INTERVIEWER' ? '面试官' : '我的回答' }}</small><p>{{ turn.content }}</p></div>
          </article>
          <article v-if="busy && session.turnState !== 'RETRYABLE_ERROR'" class="algorithm-message algorithm-message--thinking">
            <span>官</span><div><small>面试官</small><p>正在听取并整理下一问...</p></div>
          </article>
        </div>

        <p v-if="error" class="composer-error">{{ error }}</p>
        <div v-if="session.turnState === 'RETRYABLE_ERROR'" class="turn-recovery">
          <span>上一条回答没有处理完成，可以安全重试，不会重复推进面试。</span>
          <el-button type="primary" :loading="submitting" @click="retryPending">重试回答</el-button>
        </div>
        <div v-if="session.status === 'FINISHED'" class="report-ready">
          <CircleCheck /><span><strong>本轮口述已经结束</strong><small>评分和证据复盘已生成</small></span>
          <el-button type="primary" @click="openReport">查看复盘</el-button>
        </div>
        <div v-else class="algorithm-composer">
          <el-input v-model="answer" type="textarea" :rows="5" resize="none" maxlength="12000"
            placeholder="像真实面试一样，把你的判断和推导过程完整说出来..."
            :disabled="busy || session.turnState !== 'IDLE'" @keydown.ctrl.enter.prevent="send" />
          <div>
            <span>文字口述</span>
            <el-button type="primary" :loading="submitting" :disabled="!answer.trim() || busy" @click="send">
              提交回答 <Promotion />
            </el-button>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { CircleCheck, DataAnalysis, Promotion, SwitchButton } from '@element-plus/icons-vue'
import { finishAlgorithmSession, getAlgorithmSession, retryAlgorithmTurn, submitAlgorithmTurn } from '../api/algorithmApi'
import type { AlgorithmSession } from '../model/types'

const props = defineProps<{ sessionId: string }>()
const id = Number(props.sessionId)
const router = useRouter()
const session = ref<AlgorithmSession | null>(null)
const answer = ref('')
const loading = ref(false)
const submitting = ref(false)
const finishing = ref(false)
const error = ref('')
const connection = ref<'online' | 'connecting' | 'offline'>('connecting')
const messageList = ref<HTMLElement | null>(null)
const busy = computed(() => loading.value || submitting.value || finishing.value || session.value?.turnState === 'PROCESSING')
const connectionLabel = computed(() => ({ online: '已连接', connecting: '正在恢复', offline: '连接中断' })[connection.value])
const difficultyLabel = computed(() => ({ easy: '简单', medium: '中等', hard: '困难' })[session.value?.problem.difficulty ?? 'easy'])
const stages = ['CLARIFY', 'BASELINE_SOLUTION', 'OPTIMIZATION', 'COMPLEXITY', 'EDGE_CASE', 'FOLLOW_UP']
const stageIndex = computed(() => session.value?.status === 'FINISHED' ? 6 : Math.max(1, stages.indexOf(session.value?.currentStage ?? '') + 1))

onMounted(load)

async function load() {
  loading.value = true
  connection.value = 'connecting'
  error.value = ''
  try { session.value = (await getAlgorithmSession(id)).data.data; connection.value = 'online'; await scrollBottom() }
  catch { connection.value = 'offline'; error.value = '无法读取这场算法训练，请稍后重试。' }
  finally { loading.value = false }
}

async function send() {
  const content = answer.value.trim()
  const current = session.value
  const question = [...(current?.turns ?? [])].reverse().find(turn => turn.role === 'INTERVIEWER')
  if (!content || !current || !question) return
  submitting.value = true
  error.value = ''
  try {
    const clientTurnId = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
    session.value = (await submitAlgorithmTurn(id, {
      clientTurnId, questionTurnId: question.id, expectedVersion: current.version,
      content, inputModality: 'TEXT',
    })).data.data
    answer.value = ''
    await scrollBottom()
  } catch { error.value = '回答没有处理完成。你可以刷新页面后安全恢复。'; await load() }
  finally { submitting.value = false }
}

async function retryPending() {
  submitting.value = true
  error.value = ''
  try { session.value = (await retryAlgorithmTurn(id)).data.data; await scrollBottom() }
  catch { error.value = '恢复失败，请稍后再试。' }
  finally { submitting.value = false }
}

async function requestFinish() {
  try {
    await ElMessageBox.confirm('现在结束后，未覆盖的维度会标记为“未评估”，不会按零分计算。', '结束这场面试？', {
      confirmButtonText: '结束并查看复盘', cancelButtonText: '继续面试', type: 'warning',
    })
  } catch { return }
  finishing.value = true
  try { session.value = (await finishAlgorithmSession(id)).data.data; await openReport() }
  catch { error.value = '暂时无法结束面试，请稍后重试。' }
  finally { finishing.value = false }
}

async function openReport() { await router.push({ name: 'algorithm-report', params: { sessionId: id } }) }
async function scrollBottom() { await nextTick(); messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' }) }
</script>
