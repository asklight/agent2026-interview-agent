import { computed, ref } from 'vue'
import { finishProjectInterview, getProjectInterviewTurns, submitProjectInterviewTurn } from '@/features/project-deep-dive/api/interviewApi'
import type { InputModality, ProjectInterviewSession } from '@/features/project-deep-dive/model/types'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

function newClientTurnId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const PENDING_TURNS_KEY = 'agent2026:project-deep-dive:pending-turns'

interface PendingSubmission { clientTurnId: string; content: string }

function readPending(sessionId: number): PendingSubmission | null {
  if (typeof window === 'undefined') return null
  try {
    const values = JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}') as Record<string, PendingSubmission>
    return values[String(sessionId)] || null
  } catch {
    window.sessionStorage.removeItem(PENDING_TURNS_KEY)
    return null
  }
}

function writePending(sessionId: number, pending: PendingSubmission | null) {
  if (typeof window === 'undefined') return
  let values: Record<string, PendingSubmission> = {}
  try {
    values = JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}') as Record<string, PendingSubmission>
  } catch {
    // Invalid local state is replaced with a clean map.
  }
  if (pending) values[String(sessionId)] = pending
  else delete values[String(sessionId)]
  window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify(values))
}

export function useInterviewSession(sessionId: number) {
  const store = useInterviewSessionStore()
  const session = ref<ProjectInterviewSession | null>(null)
  const loading = ref(false)
  const submitting = ref(false)
  const finishing = ref(false)
  const errorMessage = ref('')
  const pendingSubmission = ref<PendingSubmission | null>(readPending(sessionId))

  const accessToken = computed(() => store.accessToken || store.restoreSessionAccess(sessionId)?.accessToken || '')
  const hasAccess = computed(() => Boolean(accessToken.value))

  function applySession(value: ProjectInterviewSession) {
    session.value = value
    store.conversationPhase = value.conversationPhase || ''
    store.inputModality = value.inputModality
    store.connectionStatus = 'online'
  }

  async function load() {
    if (!hasAccess.value || loading.value) return null
    loading.value = true
    store.connectionStatus = 'connecting'
    errorMessage.value = ''
    try {
      const response = await getProjectInterviewTurns(sessionId, accessToken.value)
      applySession(response.data.data)
      return response.data.data
    } catch {
      store.connectionStatus = 'offline'
      errorMessage.value = '会话恢复失败，请检查网络后重试。'
      return null
    } finally {
      loading.value = false
    }
  }

  async function submit(content: string, inputModality: InputModality = 'TEXT') {
    const normalized = content.trim()
    if (!normalized || !hasAccess.value || submitting.value || session.value?.status === 'FINISHED') return null

    if (pendingSubmission.value && pendingSubmission.value.content !== normalized) {
      errorMessage.value = '检测到一条尚未完成的回答，请先重试原回答，避免重复推进面试。'
      return null
    }
    if (!pendingSubmission.value) {
      pendingSubmission.value = { clientTurnId: newClientTurnId(), content: normalized }
      writePending(sessionId, pendingSubmission.value)
    }

    submitting.value = true
    errorMessage.value = ''
    try {
      const response = await submitProjectInterviewTurn(sessionId, accessToken.value, {
        clientTurnId: pendingSubmission.value.clientTurnId,
        content: normalized,
        inputModality,
      })
      applySession(response.data.data)
      pendingSubmission.value = null
      writePending(sessionId, null)
      return response.data.data
    } catch {
      errorMessage.value = '这次回答没有成功处理，可以直接重试；系统会复用同一个提交标识。'
      return null
    } finally {
      submitting.value = false
    }
  }

  async function finish() {
    if (!hasAccess.value || finishing.value || session.value?.status === 'FINISHED') return session.value
    finishing.value = true
    try {
      const response = await finishProjectInterview(sessionId, accessToken.value)
      applySession(response.data.data)
      return response.data.data
    } finally {
      finishing.value = false
    }
  }

  return {
    session,
    loading,
    submitting,
    finishing,
    errorMessage,
    pendingSubmission,
    hasAccess,
    load,
    submit,
    finish,
  }
}
