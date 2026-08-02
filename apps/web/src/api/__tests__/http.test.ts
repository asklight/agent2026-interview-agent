import { AxiosError } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import http, { isApiBusinessError } from '@/api/http'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

describe('HTTP business error classification', () => {
  it('converts a business code in the unified response envelope', async () => {
    const request = http.get('/test-business-error', {
      adapter: async config => ({
        data: { code: 40911, msg: 'state changed', data: null },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }),
    })

    await expect(request).rejects.toMatchObject({
      name: 'ApiBusinessError',
      code: 40911,
      message: 'state changed',
    })
    await request.catch(error => expect(isApiBusinessError(error)).toBe(true))
  })

  it('keeps a transport failure distinct from a business rejection', async () => {
    const transportError = new AxiosError('network unavailable', 'ERR_NETWORK')
    const request = http.get('/test-transport-error', {
      adapter: async () => Promise.reject(transportError),
    })

    await expect(request).rejects.toBe(transportError)
    expect(isApiBusinessError(transportError)).toBe(false)
  })
})
