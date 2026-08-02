import { computed, ref } from 'vue'
import {
  finishProjectInterview,
  getProjectInterviewTurns,
  retryPendingProjectInterviewTurn,
  submitProjectInterviewTurn,
} from '@/features/project-deep-dive/api/interviewApi'
import { isApiBusinessError } from '@/api/http'
import type { ApiBusinessError } from '@/api/http'
import type { InputModality, ProjectInterviewSession } from '@/features/project-deep-dive/model/types'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

function newClientTurnId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export const PENDING_TURNS_KEY = 'agent2026:project-deep-dive:pending-turns'

interface PendingSubmission {
  clientTurnId: string
  questionTurnId: number | null
  content: string
  inputModality: InputModality
}

function pendingSubmission(value: unknown): PendingSubmission | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null
  const candidate = value as Partial<PendingSubmission>
  if (typeof candidate.clientTurnId !== 'string') return null
  if (typeof candidate.content !== 'string') return null
  const clientTurnId = candidate.clientTurnId.trim()
  const content = candidate.content.trim()
  if (!clientTurnId || clientTurnId.length > 64) return null
  if (!content || content.length > 20_000) return null
  if (candidate.questionTurnId !== undefined
    && candidate.questionTurnId !== null
    && (typeof candidate.questionTurnId !== 'number'
      || !Number.isSafeInteger(candidate.questionTurnId)
      || candidate.questionTurnId <= 0)) return null
  if (candidate.inputModality !== undefined
    && candidate.inputModality !== 'TEXT'
    && candidate.inputModality !== 'VOICE_TRANSCRIPT') return null
  return {
    clientTurnId,
    questionTurnId: candidate.questionTurnId ?? null,
    content,
    inputModality: candidate.inputModality || 'TEXT',
  }
}

function readPending(sessionId: number): PendingSubmission | null {
  if (typeof window === 'undefined') return null
  try {
    const parsed = JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}') as unknown
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('invalid pending map')
    const values = parsed as Record<string, unknown>
    const key = String(sessionId)
    if (values[key] === undefined) return null
    const pending = pendingSubmission(values[key])
    if (pending) return pending
    delete values[key]
    window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify(values))
    return null
  } catch {
    window.sessionStorage.removeItem(PENDING_TURNS_KEY)
    return null
  }
}

function writePending(sessionId: number, pending: PendingSubmission | null) {
  if (typeof window === 'undefined') return
  let values: Record<string, PendingSubmission> = {}
  try {
    const parsed = JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}') as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      values = parsed as Record<string, PendingSubmission>
    }
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
  const isThinking = computed(() => submitting.value || session.value?.turnState === 'PROCESSING')

  function applySession(value: ProjectInterviewSession) {
    session.value = value
    store.conversationPhase = value.conversationPhase || ''
    store.inputModality = value.inputModality
    store.connectionStatus = 'online'
    if (value.status === 'FINISHED') clearPending()
  }

  function clearPending() {
    pendingSubmission.value = null
    writePending(sessionId, null)
  }

  function currentQuestionTurnId(value: ProjectInterviewSession | null = session.value) {
    if (!value) return null
    return [...value.turns].reverse().find(turn => turn.role === 'INTERVIEWER')?.turnId || null
  }

  async function postPending(pending: PendingSubmission) {
    const response = await submitProjectInterviewTurn(sessionId, accessToken.value, pending)
    applySession(response.data.data)
    clearPending()
    return response.data.data
  }

  function describePendingState(value: ProjectInterviewSession | null) {
    if (value?.turnState === 'RETRYABLE_ERROR') {
      return '这次回答没有处理完成，可以安全重试原回答。'
    }
    if (value?.turnState === 'PROCESSING') {
      return '回答已经送达，面试官仍在处理。'
    }
    return '暂时无法确认处理结果，请稍后重试上一条回答。'
  }

  async function handleSubmitBusinessError(error: ApiBusinessError) {
    const questionChanged = error.code === 40911
    if (questionChanged) clearPending()
    const refreshed = await load()
    if (refreshed?.status === 'FINISHED') return refreshed
    errorMessage.value = questionChanged
      ? '面试官已经进入下一题，这条回答没有提交。请根据当前问题调整后再回答。'
      : describePendingState(refreshed)
    return null
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
      const questionTurnId = currentQuestionTurnId()
      if (!questionTurnId) {
        errorMessage.value = '当前问题还没有加载完成，请重新连接后再回答。'
        return null
      }
      pendingSubmission.value = {
        clientTurnId: newClientTurnId(),
        questionTurnId,
        content: normalized,
        inputModality,
      }
      writePending(sessionId, pendingSubmission.value)
    }

    submitting.value = true
    errorMessage.value = ''
    try {
      return await postPending(pendingSubmission.value)
    } catch (error) {
      if (isApiBusinessError(error)) {
        return await handleSubmitBusinessError(error)
      }
      if (session.value) session.value.turnState = 'PROCESSING'
      let refreshed = await load()
      if (refreshed?.status === 'FINISHED') return refreshed
      if (refreshed?.turnState === 'IDLE' && pendingSubmission.value) {
        try {
          return await postPending(pendingSubmission.value)
        } catch (retryError) {
          if (isApiBusinessError(retryError)) {
            return await handleSubmitBusinessError(retryError)
          }
          refreshed = await load()
        }
      }
      errorMessage.value = describePendingState(refreshed)
      return null
    } finally {
      submitting.value = false
    }
  }

  async function retryPending() {
    if (!hasAccess.value || submitting.value || session.value?.status === 'FINISHED') return null
    if (pendingSubmission.value) {
      return submit(pendingSubmission.value.content, pendingSubmission.value.inputModality)
    }
    submitting.value = true
    errorMessage.value = ''
    try {
      const response = await retryPendingProjectInterviewTurn(sessionId, accessToken.value)
      applySession(response.data.data)
      return response.data.data
    } catch {
      const refreshed = await load()
      if (refreshed?.turnState === 'IDLE') return refreshed
      errorMessage.value = describePendingState(refreshed)
      return null
    } finally {
      submitting.value = false
    }
  }

  async function finish() {
    if (!hasAccess.value || !session.value || finishing.value || submitting.value
      || session.value.status === 'FINISHED' || session.value.turnState !== 'IDLE') return session.value
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
    isThinking,
    load,
    submit,
    retryPending,
    finish,
  }
}
