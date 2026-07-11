import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { PENDING_TURNS_KEY, useInterviewSession } from '@/features/project-deep-dive/composables/useInterviewSession'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'
import type { ProjectInterviewSession } from '@/features/project-deep-dive/model/types'
import { submitProjectInterviewTurn } from '@/features/project-deep-dive/api/interviewApi'

vi.mock('@/features/project-deep-dive/api/interviewApi', () => ({
  getProjectInterviewTurns: vi.fn(),
  submitProjectInterviewTurn: vi.fn(),
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
  turns: [],
}

describe('useInterviewSession submit idempotency guard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
    setActivePinia(createPinia())
    useInterviewSessionStore().saveSessionAccess(7, 3, 'resource-token')
  })

  it('allows only one API call when the submit action is triggered twice', async () => {
    let resolveRequest!: (value: { data: { data: ProjectInterviewSession } }) => void
    vi.mocked(submitProjectInterviewTurn).mockReturnValue(new Promise(resolve => { resolveRequest = resolve }) as never)
    const interview = useInterviewSession(7)

    const first = interview.submit('我先说明指标基线。')
    const second = interview.submit('我先说明指标基线。')

    expect(submitProjectInterviewTurn).toHaveBeenCalledTimes(1)
    const payload = vi.mocked(submitProjectInterviewTurn).mock.calls[0][2]
    expect(payload.clientTurnId).toBeTruthy()
    expect(second).resolves.toBeNull()

    resolveRequest({ data: { data: session } })
    await first
    expect(JSON.parse(window.sessionStorage.getItem(PENDING_TURNS_KEY) || '{}')).toEqual({})
  })

  it('restores the same clientTurnId after a failed request and page refresh', async () => {
    vi.mocked(submitProjectInterviewTurn).mockRejectedValueOnce(new Error('timeout'))
    const firstInstance = useInterviewSession(7)
    await firstInstance.submit('这是一条需要安全重试的回答。')
    const firstId = vi.mocked(submitProjectInterviewTurn).mock.calls[0][2].clientTurnId

    vi.mocked(submitProjectInterviewTurn).mockResolvedValueOnce({ data: { data: session } } as never)
    const refreshedInstance = useInterviewSession(7)
    expect(refreshedInstance.pendingSubmission.value?.content).toBe('这是一条需要安全重试的回答。')
    await refreshedInstance.submit('这是一条需要安全重试的回答。')

    expect(vi.mocked(submitProjectInterviewTurn).mock.calls[1][2].clientTurnId).toBe(firstId)
  })
})
