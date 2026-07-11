<template>
  <main class="interview-room">
    <header class="interview-room__header">
      <div class="room-brand"><span>北</span><div><strong>项目经历深挖</strong><small>沉浸式文字面试</small></div></div>
      <div class="room-status"><i :class="store.connectionStatus"></i><span>{{ connectionLabel }}</span><time>{{ elapsed }}</time></div>
      <el-button :loading="finishing" :disabled="!session || session.status === 'FINISHED'" @click="requestFinish">结束面试</el-button>
    </header>

    <section v-if="!hasAccess" class="room-empty-state">
      <h1>无法恢复这场面试</h1><p>当前标签页中没有会话访问令牌。为了保护项目内容，系统不会通过会话 ID 绕过归属校验。</p><RouterLink class="primary-link" to="/project-deep-dive">返回项目深挖</RouterLink>
    </section>
    <section v-else-if="loading && !session" class="room-empty-state"><span class="analysis-state__orb"></span><h1>正在恢复面试现场</h1><p>从后端重新拉取完整对话，不依赖浏览器中的消息缓存。</p></section>
    <section v-else-if="errorMessage && !session" class="room-empty-state"><h1>暂时无法恢复面试现场</h1><p>{{ errorMessage }}</p><el-button type="primary" :loading="loading" @click="load">重新连接</el-button></section>
    <section v-else-if="session" class="interview-room__body">
      <div class="interview-context-bar"><span>项目面试进行中</span><strong>{{ session.completedProbeCount }}/{{ session.totalProbeCount || '—' }} 个探查点</strong><small>过程中不展示评分，完整反馈将在结束后统一生成</small></div>
      <InterviewMessageList :turns="session.turns" :thinking="submitting" />
      <p v-if="errorMessage" class="composer-error">{{ errorMessage }}</p>
      <InterviewComposer v-model="answer" :submitting="submitting" :disabled="session.status === 'FINISHED'" @submit="sendAnswer" />
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { RouterLink, useRouter } from 'vue-router'
import InterviewComposer from '@/features/project-deep-dive/components/InterviewComposer.vue'
import InterviewMessageList from '@/features/project-deep-dive/components/InterviewMessageList.vue'
import { useInterviewSession } from '@/features/project-deep-dive/composables/useInterviewSession'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

const props = defineProps<{ sessionId: string }>()
const numericSessionId = Number(props.sessionId)
const router = useRouter()
const store = useInterviewSessionStore()
store.restoreSessionAccess(numericSessionId)
const { session, loading, submitting, finishing, errorMessage, pendingSubmission, hasAccess, load, submit, finish } = useInterviewSession(numericSessionId)
const answer = ref('')
const now = ref(Date.now())
let timer: number | undefined

const connectionLabel = computed(() => store.connectionStatus === 'online' ? '已连接' : store.connectionStatus === 'connecting' ? '正在恢复' : store.connectionStatus === 'offline' ? '连接中断' : '等待连接')
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
  if (result?.status === 'FINISHED') await router.replace({ name: 'project-interview-report', params: { sessionId: numericSessionId } })
}

async function requestFinish() {
  await ElMessageBox.confirm('现在结束后，未覆盖的维度会标记为“未评估”，不会按零分计算。', '结束这场面试？', { confirmButtonText: '结束并查看复盘', cancelButtonText: '继续面试', type: 'warning' })
  const result = await finish()
  if (result?.status === 'FINISHED') await router.push({ name: 'project-interview-report', params: { sessionId: numericSessionId } })
}

onMounted(async () => {
  await load()
  if (pendingSubmission.value) answer.value = pendingSubmission.value.content
  timer = window.setInterval(() => { now.value = Date.now() }, 1000)
  if (session.value?.status === 'FINISHED') await router.replace({ name: 'project-interview-report', params: { sessionId: numericSessionId } })
})
onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>
