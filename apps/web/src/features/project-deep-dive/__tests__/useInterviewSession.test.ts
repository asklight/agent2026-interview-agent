import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ApiBusinessError } from '@/api/http'
import { PENDING_TURNS_KEY, useInterviewSession } from '@/features/project-deep-dive/composables/useInterviewSession'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'
import type { ProjectInterviewSession } from '@/features/project-deep-dive/model/types'
import {
  getProjectInterviewTurns,
  retryPendingProjectInterviewTurn,
  submitProjectInterviewTurn,
} from '@/features/project-deep-dive/api/interviewApi'

vi.mock('@/features/project-deep-dive/api/interviewApi', () => ({
  getProjectInterviewTurns: vi.fn(),
  submitProjectInterviewTurn: vi.fn(),
  retryPendingProjectInterviewTurn: vi.fn(),
  finishProjectInterview: vi.fn(),
}))

const session: ProjectInterviewSession = {
  sessionId: 7,
  mode: 'PROJECT_DEEP_DIVE',
  status: 'IN_PROGRESS',
  conversationPhase: 'CLAIM_DEEP_DIVE',
  currentProbeDimension: 'METRIC',
  completedProbeCount: 1,
  totalProbeCount: 6,
  maxFollowUpsPerClaim: 3,
  inputModality: 'TEXT',
  turnState: 'IDLE',
  turns: [{
    turnId: 100,
    sequenceNo: 1,
    role: 'INTERVIEWER',
    turnType: 'OPENING',
    content: '请先介绍这个项目。',
    inputModality: 'TEXT',
    startedAt: null,
    endedAt: null,
    createTime: '2026-08-02T10:00:00Z',
  }],
}

describe('useInterviewSession submit idempotency guard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.sessionStorage.clear()
    setActivePinia(createPinia())
    useInterviewSessionStore().saveSessionAccess(7, 3, 'resource-token')
  })

  it('allows only one API call when the submit action is triggered twice', async () => {
    let resolveRequest!: (value: { data: { data: ProjectInterviewSession } }) => void
    vi.mocked(submitProjectInterviewTurn).mockReturnValue(new Promise(resolve => { resolveRequest = resolve }) as never)
    const interview = useInterviewSession(7)
    interview.session.value = { ...session }

    const first = interview.submit('我先说明指标基线。')
    const second = interview.submit('我先说明指标基线。')

    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(1)
    const payload = vi.mocked(submitProjectInterviewTurn).mock.calls[0][2]
    expect(payload.clientTurnId).toBeTruthy()
    expect(payload.questionTurnId).toBe(100)
    expect(payload.inputModality).toBe('TEXT')
    expect(second).resolves.toBeNull()

    resolveRequest({ data: { data: session } })
    await first
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({})
  })

  it('restores the same clientTurnId after a failed request and page refresh', async () => {
    vi.mocked(submitProjectInterviewTurn).mockRejectedValueOnce(new Error('timeout'))
    const firstInstance = useInterviewSession(7)
    firstInstance.session.value = { ...session }
    await firstInstance.submit('这是一条需要安全重试的回答。', 'VOICE_TRANSCRIPT')
    const firstId = vi.mocked(submitProjectInterviewTurn).mock.calls[0][2].clientTurnId

    vi.mocked(submitProjectInterviewTurn).mockResolvedValueOnce({ data: { data: session } } as never)
    const refreshedInstance = useInterviewSession(7)
    expect(refreshedInstance.pendingSubmission.value?.content).toBe('这是一条需要安全重试的回答。')
    expect(refreshedInstance.pendingSubmission.value?.inputModality).toBe('VOICE_TRANSCRIPT')
    await refreshedInstance.submit('这是一条需要安全重试的回答。')

    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2].clientTurnId).toBe(firstId)
    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2].questionTurnId).toBe(100)
    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2].inputModality).toBe('VOICE_TRANSCRIPT')
  })

  it('reconciles immediately with the same id when the first response is lost', async () => {
    vi.mocked(submitProjectInterviewTurn)
      .mockRejectedValueOnce(new Error('response lost'))
      .mockResolvedValueOnce({ data: { data: { ...session } } } as never)
    vi.mocked(getProjectInterviewTurns).mockResolvedValueOnce({ data: { data: { ...session } } } as never)
    const interview = useInterviewSession(7)
    interview.session.value = { ...session }

    const result = await interview.submit('回答已经被服务端接收。', 'VOICE_TRANSCRIPT')

    expect(result?.turnState).toBe('IDLE')
    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(2)
    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2])
      .toEqual(vi.mocked(submitProjectInterviewTurn).mock.calls[0][2])
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({})
  })

  it('treats a finished GET as the terminal result when the final response is lost', async () => {
    const finished = { ...session, status: 'FINISHED' as const, turnState: 'IDLE' as const }
    vi.mocked(submitProjectInterviewTurn).mockRejectedValueOnce(new Error('final response lost'))
    vi.mocked(getProjectInterviewTurns).mockResolvedValueOnce({ data: { data: finished } } as never)
    const interview = useInterviewSession(7)
    interview.session.value = { ...session }

    const result = await interview.submit('这是最后一轮回答。')

    expect(result?.status).toBe('FINISHED')
    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(1)
    expect(interview.pendingSubmission.value).toBeNull()
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({})
  })

  it('recovers a retryable server turn even when local pending state is missing', async () => {
    const retryableSession = { ...session, turnState: 'RETRYABLE_ERROR' as const }
    const recoveredSession = { ...session, turnState: 'IDLE' as const }
    vi.mocked(retryPendingProjectInterviewTurn).mockResolvedValueOnce({ data: { data: recoveredSession } } as never)
    const interview = useInterviewSession(7)
    interview.session.value = retryableSession

    const result = await interview.retryPending()

    expect(retryPendingProjectInterviewTurn).toHaveBeenCalledWith(7, 'resource-token')
    expect(result?.turnState).toBe('IDLE')
    expect(interview.session.value?.turnState).toBe('IDLE')
  })

  it('does not replay a stale answer after the server reports a question conflict', async () => {
    const current = useInterviewSession(7)
    current.session.value = { ...session }
    vi.mocked(submitProjectInterviewTurn).mockRejectedValueOnce(new ApiBusinessError(40911, 'state conflict'))
    vi.mocked(getProjectInterviewTurns).mockResolvedValueOnce({
      data: { data: { ...session, turns: [...session.turns, {
        ...session.turns[0],
        turnId: 101,
        sequenceNo: 2,
        turnType: 'FOLLOW_UP',
        content: '请说明你如何验证这个方案。',
      }] } },
    } as never)

    const result = await current.submit('旧标签页的回答')

    expect(result).toBeNull()
    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(1)
    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[0][2].questionTurnId).toBe(100)
    expect(current.pendingSubmission.value).toBeNull()
    expect(current.errorMessage.value).toContain('进入下一题')
  })

  it('stops after a reconciliation replay reports that the question changed', async () => {
    const nextQuestion = {
      ...session,
      turns: [...session.turns, {
        ...session.turns[0],
        turnId: 101,
        sequenceNo: 2,
        turnType: 'FOLLOW_UP' as const,
        content: '请说明你如何验证这个方案。',
      }],
    }
    vi.mocked(submitProjectInterviewTurn)
      .mockRejectedValueOnce(new Error('response lost'))
      .mockRejectedValueOnce(new ApiBusinessError(40911, 'state conflict'))
    vi.mocked(getProjectInterviewTurns)
      .mockResolvedValueOnce({ data: { data: nextQuestion } } as never)
      .mockResolvedValueOnce({ data: { data: nextQuestion } } as never)
    const current = useInterviewSession(7)
    current.session.value = { ...session }

    const result = await current.submit('旧问题的迟到回答')

    expect(result).toBeNull()
    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(2)
    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2].questionTurnId).toBe(100)
    expect(current.pendingSubmission.value).toBeNull()
    expect(current.session.value?.turns[current.session.value.turns.length - 1]?.turnId).toBe(101)
    expect(current.errorMessage.value).toContain('进入下一题')
  })

  it('does not create an unanchored answer before the interviewer question is loaded', async () => {
    const current = useInterviewSession(7)
    current.session.value = { ...session, turns: [] }

    const result = await current.submit('还没有对应问题的回答')

    expect(result).toBeNull()
    expect(submitProjectInterviewTurn).not.toHaveBeenCalled()
    expect(current.pendingSubmission.value).toBeNull()
    expect(current.errorMessage.value).toContain('还没有加载完成')
  })

  it('keeps the thinking state after refresh while the server is processing', () => {
    const interview = useInterviewSession(7)
    interview.session.value = { ...session, turnState: 'PROCESSING' }

    expect(interview.isThinking.value).toBe(true)
  })

  it('drops a malformed local pending turn so the server can drive recovery', () => {
    window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify({
      7: { clientTurnId: 'client-7', content: 42, inputModality: 'VOICE' },
      8: { clientTurnId: 'client-8', questionTurnId: 200, content: '保留其他会话', inputModality: 'TEXT' },
    }))

    const interview = useInterviewSession(7)

    expect(interview.pendingSubmission.value).toBeNull()
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({
      8: { clientTurnId: 'client-8', questionTurnId: 200, content: '保留其他会话', inputModality: 'TEXT' },
    })
  })

  it('normalizes a recovered pending turn before comparing and replaying it', async () => {
    window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify({
      7: {
        clientTurnId: '  client-7  ',
        questionTurnId: 100,
        content: '  recovered answer  ',
        inputModality: 'TEXT',
      },
    }))
    vi.mocked(submitProjectInterviewTurn).mockResolvedValueOnce({ data: { data: session } } as never)
    const interview = useInterviewSession(7)
    interview.session.value = { ...session }

    await interview.submit('recovered answer')

    expect(submitProjectInterviewTurn).toHaveBeenCalledWith(7, 'resource-token', {
      clientTurnId: 'client-7',
      questionTurnId: 100,
      content: 'recovered answer',
      inputModality: 'TEXT',
    })
    expect(interview.pendingSubmission.value).toBeNull()
  })

  it('keeps a legacy pending id without an anchor for server-side idempotent recovery', () => {
    window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify({
      7: { clientTurnId: 'legacy-client', content: 'legacy answer', inputModality: 'TEXT' },
    }))

    const interview = useInterviewSession(7)

    expect(interview.pendingSubmission.value).toEqual({
      clientTurnId: 'legacy-client',
      questionTurnId: null,
      content: 'legacy answer',
      inputModality: 'TEXT',
    })
  })

  it('drops pending values outside the public request limits', () => {
    window.sessionStorage.setItem(PENDING_TURNS_KEY, JSON.stringify({
      7: { clientTurnId: 'x'.repeat(65), questionTurnId: 100, content: 'answer', inputModality: 'TEXT' },
      8: { clientTurnId: 'client-8', questionTurnId: Number.MAX_SAFE_INTEGER + 1, content: 'answer', inputModality: 'TEXT' },
      9: { clientTurnId: 'client-9', questionTurnId: 100, content: 'x'.repeat(20_001), inputModality: 'TEXT' },
    }))

    expect(useInterviewSession(7).pendingSubmission.value).toBeNull()
    expect(useInterviewSession(8).pendingSubmission.value).toBeNull()
    expect(useInterviewSession(9).pendingSubmission.value).toBeNull()
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({})
  })
})
