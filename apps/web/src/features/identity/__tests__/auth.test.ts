import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import * as authApi from '../api/authApi'

vi.mock('../api/authApi', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
}))

vi.mock('@/api/http', () => ({
  setAccessToken: vi.fn(),
  setRefreshHandler: vi.fn(),
}))

const payload = {
  accessToken: 'access-token',
  accessTokenExpiresAt: '2026-08-08T12:00:00Z',
  user: { id: 7, username: 'TestUser' },
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('restores the user from refresh cookie during initialization', async () => {
    vi.mocked(authApi.refreshSession).mockResolvedValue({ data: { data: payload } } as never)
    const auth = useAuthStore()

    await Promise.all([auth.initialize(), auth.initialize()])

    expect(auth.user).toEqual(payload.user)
    expect(auth.isAuthenticated).toBe(true)
    expect(authApi.refreshSession).toHaveBeenCalledTimes(1)
  })

  it('clears local identity when refresh is unavailable', async () => {
    vi.mocked(authApi.refreshSession).mockRejectedValue(new Error('expired'))
    const auth = useAuthStore()

    await auth.initialize()

    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('clears identity even when logout request fails', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ data: { data: payload } } as never)
    vi.mocked(authApi.logout).mockRejectedValue(new Error('offline'))
    const auth = useAuthStore()
    await auth.signIn('TestUser', 'password1')

    await expect(auth.signOut()).resolves.toBeUndefined()

    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })
})
