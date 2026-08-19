import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getTrainingAgentDashboard, type TrainingAgentDashboard } from '../../api/trainingAgentApi'
import { useTrainingAgentDashboard } from '../useTrainingAgentDashboard'

vi.mock('../../api/trainingAgentApi', () => ({
  getTrainingAgentDashboard: vi.fn(),
}))

function response(data: TrainingAgentDashboard) {
  return { data: { data } } as never
}

function dashboard(overrides: Partial<TrainingAgentDashboard> = {}): TrainingAgentDashboard {
  return {
    enabled: true,
    degraded: false,
    state: 'READY',
    primaryRecommendation: {
      revision: 1,
      trainingType: 'KNOWLEDGE',
      dimensionCode: 'KNOWLEDGE.JAVA',
      title: 'Java 校准',
      reason: '最近一次训练暴露了并发知识缺口',
      estimatedMinutes: 10,
      action: { module: 'Java' },
      evidenceCount: 1,
    },
    alternatives: [],
    focusDimensions: [],
    recentProgress: null,
    generatedAt: '2026-08-19T10:00:00',
    ...overrides,
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

function mountComposable() {
  let result!: ReturnType<typeof useTrainingAgentDashboard>
  const wrapper = mount(defineComponent({
    setup() {
      result = useTrainingAgentDashboard()
      return () => null
    },
  }))
  return { result, wrapper }
}

async function settleImmediateAttempt() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('useTrainingAgentDashboard', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
  })

  it('performs a single request for an ordinary load', async () => {
    const current = dashboard()
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(current))
    const { result, wrapper } = mountComposable()

    await result.load()

    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(1)
    expect(getTrainingAgentDashboard).toHaveBeenCalledWith(expect.any(AbortSignal))
    expect(result.dashboard.value).toEqual(current)
    expect(result.loading.value).toBe(false)
    expect(result.requestFailed.value).toBe(false)
    expect(result.refreshState.value).toBe('settled')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('makes at most three attempts after a completed training', async () => {
    const unchanged = dashboard()
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(unchanged))
    const { result, wrapper } = mountComposable()

    const completed = result.load({ refresh: true })
    await settleImmediateAttempt()
    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(1)
    expect(result.refreshState.value).toBe('refreshing')

    await vi.advanceTimersByTimeAsync(600)
    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(2)
    expect(result.refreshState.value).toBe('refreshing')

    await vi.advanceTimersByTimeAsync(1_200)
    await completed

    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(3)
    expect(result.dashboard.value).toEqual(unchanged)
    expect(result.refreshState.value).toBe('settled')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('stops polling as soon as the dashboard fingerprint changes', async () => {
    const initial = dashboard()
    const updated = dashboard({
      generatedAt: '2026-08-19T10:00:01',
      primaryRecommendation: { ...dashboard().primaryRecommendation!, revision: 2 },
    })
    vi.mocked(getTrainingAgentDashboard)
      .mockResolvedValueOnce(response(initial))
      .mockResolvedValueOnce(response(updated))
    const { result, wrapper } = mountComposable()

    const completed = result.load({ refresh: true })
    await settleImmediateAttempt()
    await vi.advanceTimersByTimeAsync(600)
    await completed

    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(2)
    expect(result.dashboard.value).toEqual(updated)
    expect(result.refreshState.value).toBe('settled')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it.each(['DISABLED', 'DEGRADED'] as const)('stops immediately for the %s state', async (state) => {
    const terminal = dashboard({
      enabled: state !== 'DISABLED',
      degraded: state === 'DEGRADED',
      state,
      primaryRecommendation: null,
    })
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(terminal))
    const { result, wrapper } = mountComposable()

    await result.load({ refresh: true })

    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(1)
    expect(result.dashboard.value).toEqual(terminal)
    expect(result.refreshState.value).toBe('settled')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('stops on failure and preserves the last usable dashboard', async () => {
    const previous = dashboard()
    vi.mocked(getTrainingAgentDashboard).mockResolvedValueOnce(response(previous))
    const { result, wrapper } = mountComposable()
    await result.load()
    vi.mocked(getTrainingAgentDashboard).mockRejectedValueOnce(new Error('network unavailable'))

    await result.load({ refresh: true })

    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(2)
    expect(result.dashboard.value).toEqual(previous)
    expect(result.loading.value).toBe(false)
    expect(result.requestFailed.value).toBe(false)
    expect(result.refreshState.value).toBe('failed')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('clears the refresh timer and aborts its request when unmounted', async () => {
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(dashboard()))
    const { result, wrapper } = mountComposable()

    const completed = result.load({ refresh: true })
    await settleImmediateAttempt()
    const signal = vi.mocked(getTrainingAgentDashboard).mock.calls[0][0]!
    expect(vi.getTimerCount()).toBe(1)
    expect(signal.aborted).toBe(false)

    wrapper.unmount()
    await completed
    await vi.advanceTimersByTimeAsync(5_000)

    expect(signal.aborted).toBe(true)
    expect(vi.getTimerCount()).toBe(0)
    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(1)
  })

  it('ignores a response that arrives after unmounting', async () => {
    const pending = deferred<ReturnType<typeof response>>()
    vi.mocked(getTrainingAgentDashboard).mockImplementation(() => pending.promise)
    const { result, wrapper } = mountComposable()

    const completed = result.load()
    await settleImmediateAttempt()
    const signal = vi.mocked(getTrainingAgentDashboard).mock.calls[0][0]!
    wrapper.unmount()
    pending.resolve(response(dashboard({ generatedAt: '2026-08-19T10:00:05' })))
    await completed
    await settleImmediateAttempt()

    expect(signal.aborted).toBe(true)
    expect(result.dashboard.value).toBeNull()
    expect(result.requestFailed.value).toBe(false)
    expect(getTrainingAgentDashboard).toHaveBeenCalledTimes(1)
  })
})
