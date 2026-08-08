<template>
  <main class="interview-room">
    <header class="interview-room__header">
      <div class="room-brand"><span><ChatDotRound /></span><div><strong>项目深挖面试</strong><small>文字面试 · 证据追问</small></div></div>
      <div class="room-status"><i :class="store.connectionStatus"></i><span>{{ connectionLabel }}</span><time>{{ elapsed }}</time></div>
      <el-button :loading="finishing" :disabled="!session || session.status === 'FINISHED' || session.turnState !== 'IDLE' || submitting" @click="requestFinish"><SwitchButton />结束面试</el-button>
    </header>

    <section v-if="!hasAccess" class="room-empty-state">
      <h1>无法恢复这场面试</h1><p>当前页面缺少这场面试的访问凭证。为保护项目内容，请从开始面试的标签页继续。</p><RouterLink class="primary-link" to="/project-deep-dive">返回项目深挖</RouterLink>
    </section>
    <section v-else-if="loading && !session" class="room-empty-state"><span class="analysis-state__orb"></span><h1>正在恢复面试现场</h1><p>正在回到刚才的对话进度，请稍候。</p></section>
    <section v-else-if="errorMessage && !session" class="room-empty-state"><h1>暂时无法恢复面试现场</h1><p>{{ errorMessage }}</p><el-button type="primary" :loading="loading" @click="load">重新连接</el-button></section>
    <section v-else-if="session" class="interview-room__body">
      <div class="interview-context-bar">
        <div><span>面试进度</span><strong>{{ session.completedProbeCount }}/{{ session.totalProbeCount || '—' }}</strong><small>过程中不展示评分</small></div>
        <div class="interview-progress" aria-label="面试进度"><i :style="{ width: `${progressPercent}%` }"></i></div>
      </div>
      <InterviewMessageList :turns="session.turns" :thinking="isThinking" />
      <p v-if="errorMessage" class="composer-error">{{ errorMessage }}</p>
      <div v-if="session.turnState === 'RETRYABLE_ERROR'" class="turn-recovery">
        <span>上一条回答没有处理完成，可以安全重试，不会重复推进面试。</span>
        <el-button type="primary" :loading="submitting" @click="recoverPending">重试上一条回答</el-button>
      </div>
      <div v-if="session.status === 'FINISHED'" class="report-ready"><CircleCheck /><span><strong>面试已结束</strong><small>复盘报告已经可以查看</small></span><el-button type="primary" @click="openReport">查看复盘</el-button></div>
      <InterviewComposer v-model="answer" :submitting="submitting"
        :disabled="session.status === 'FINISHED' || session.turnState !== 'IDLE' || submitting" @submit="sendAnswer" />
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { RouterLink, useRouter } from 'vue-router'
import { ChatDotRound, CircleCheck, SwitchButton } from '@element-plus/icons-vue'
import InterviewComposer from '@/features/project-deep-dive/components/InterviewComposer.vue'
import InterviewMessageList from '@/features/project-deep-dive/components/InterviewMessageList.vue'
import { useInterviewSession } from '@/features/project-deep-dive/composables/useInterviewSession'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

const props = defineProps<{ sessionId: string }>()
const numericSessionId = Number(props.sessionId)
const router = useRouter()
const store = useInterviewSessionStore()
store.restoreSessionAccess(numericSessionId)
const { session, loading, submitting, finishing, errorMessage, pendingSubmission, hasAccess,
  isThinking, load, submit, retryPending, finish } = useInterviewSession(numericSessionId)
const answer = ref('')
const now = ref(Date.now())
let timer: number | undefined
let recoveryTimer: number | undefined
let disposed = false
let reportNavigationStarted = false

const connectionLabel = computed(() => store.connectionStatus === 'online' ? '已连接' : store.connectionStatus === 'connecting' ? '正在恢复' : store.connectionStatus === 'offline' ? '连接中断' : '等待连接')
const progressPercent = computed(() => session.value?.totalProbeCount
  ? Math.min(100, Math.round(session.value.completedProbeCount / session.value.totalProbeCount * 100))
  : 0)
const elapsed = computed(() => {
  const first = session.value?.turns[0]?.createTime
  if (!first) return '00:00'
  const totalSeconds = Math.max(0, Math.floor((now.value - new Date(first).getTime()) / 1000))
  return `${String(Math.floor(totalSeconds / 60)).padStart(2, '0')}:${String(totalSeconds % 60).padStart(2, '0')}`
})

async function sendAnswer() {
  const content = answer.value.trim()
  if (!content) return
  const result = await submit(content, store.inputModality)
  if (result) answer.value = ''
}

async function requestFinish() {
  await ElMessageBox.confirm('现在结束后，未覆盖的维度会标记为“未评估”，不会按零分计算。', '结束这场面试？', { confirmButtonText: '结束并查看复盘', cancelButtonText: '继续面试', type: 'warning' })
  await finish()
}

async function recoverPending() {
  const result = await retryPending()
  if (result) answer.value = ''
}

async function openReport() {
  if (disposed || reportNavigationStarted) return
  reportNavigationStarted = true
  try {
    await router.replace({ name: 'project-interview-report', params: { sessionId: numericSessionId } })
  } catch {
    reportNavigationStarted = false
  }
}

watch(() => session.value?.status, (status) => {
  if (status === 'FINISHED') void openReport()
})

function lifecycleStopped() {
  return disposed || session.value?.status === 'FINISHED'
}

onMounted(async () => {
  await load()
  if (lifecycleStopped()) return
  if (pendingSubmission.value) {
    answer.value = pendingSubmission.value.content
    if (session.value?.turnState === 'IDLE') await recoverPending()
  }
  if (lifecycleStopped()) return
  timer = window.setInterval(() => { now.value = Date.now() }, 1000)
  recoveryTimer = window.setInterval(() => {
    if (!disposed && session.value?.turnState === 'PROCESSING' && !loading.value) {
      void load().then(() => {
        if (!disposed && session.value?.turnState === 'IDLE' && pendingSubmission.value && !submitting.value) {
          void recoverPending()
        }
      })
    }
  }, 2500)
})
onBeforeUnmount(() => {
  disposed = true
  if (timer !== undefined) window.clearInterval(timer)
  if (recoveryTimer !== undefined) window.clearInterval(recoveryTimer)
})
</script>
