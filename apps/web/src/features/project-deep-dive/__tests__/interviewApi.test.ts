import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '@/api/http'
import { createProjectInterview } from '@/features/project-deep-dive/api/interviewApi'

vi.mock('@/api/http', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('project interview API', () => {
  beforeEach(() => {
    vi.mocked(http.post).mockReset()
  })

  it('forwards targetDimension with the complete create payload and resource token', () => {
    const payload = {
      mode: 'PROJECT_DEEP_DIVE' as const,
      projectProfileId: 27,
      durationMinutes: 20,
      maxFollowUpsPerClaim: 3,
      inputModality: 'TEXT' as const,
      targetDimension: 'PROJECT.TRADEOFF',
    }

    createProjectInterview(payload, 'profile-token')

    expect(http.post).toHaveBeenCalledWith('/interview-sessions', payload, {
      headers: { 'X-Resource-Token': 'profile-token' },
    })
  })
})
