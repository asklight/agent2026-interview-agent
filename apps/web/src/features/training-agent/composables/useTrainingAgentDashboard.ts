import { onBeforeUnmount, ref } from 'vue'
import {
  getTrainingAgentDashboard,
  type TrainingAgentDashboard,
} from '../api/trainingAgentApi'

const REFRESH_DELAYS_MS = [600, 1200] as const

export type TrainingAgentRefreshState = 'idle' | 'refreshing' | 'settled' | 'failed'

function fingerprint(value: TrainingAgentDashboard) {
  return [
    value.state,
    value.generatedAt ?? '',
    value.primaryRecommendation?.revision ?? '',
  ].join(':')
}

export function useTrainingAgentDashboard() {
  const dashboard = ref<TrainingAgentDashboard | null>(null)
  const loading = ref(true)
  const requestFailed = ref(false)
  const refreshState = ref<TrainingAgentRefreshState>('idle')

  let generation = 0
  let timer: ReturnType<typeof setTimeout> | null = null
  let request: AbortController | null = null
  let complete: (() => void) | null = null

  function finish(run: number, state: TrainingAgentRefreshState) {
    if (run !== generation) return
    timer = null
    request = null
    loading.value = false
    refreshState.value = state
    const resolve = complete
    complete = null
    resolve?.()
  }

  function cancel() {
    generation += 1
    if (timer) clearTimeout(timer)
    timer = null
    request?.abort()
    request = null
    const resolve = complete
    complete = null
    resolve?.()
  }

  async function attempt(run: number, index: number, baseline: string | null, refresh: boolean) {
    if (run !== generation) return
    request = new AbortController()
    try {
      const response = await getTrainingAgentDashboard(request.signal)
      if (run !== generation) return

      const next = response.data.data
      dashboard.value = next
      requestFailed.value = false
      const current = fingerprint(next)
      const changed = baseline !== null && current !== baseline
      const terminal = next.state === 'DISABLED' || next.state === 'DEGRADED'
      const lastAttempt = !refresh || index >= REFRESH_DELAYS_MS.length

      if (changed || terminal || lastAttempt) {
        finish(run, 'settled')
        return
      }

      timer = setTimeout(
        () => void attempt(run, index + 1, baseline ?? current, refresh),
        REFRESH_DELAYS_MS[index],
      )
    } catch (error) {
      if (run !== generation || request?.signal.aborted) return
      requestFailed.value = dashboard.value === null
      finish(run, 'failed')
    }
  }

  function load(options: { refresh?: boolean } = {}) {
    cancel()
    const run = generation
    const refresh = options.refresh === true
    loading.value = dashboard.value === null
    requestFailed.value = false
    refreshState.value = refresh ? 'refreshing' : 'idle'

    return new Promise<void>((resolve) => {
      complete = resolve
      void attempt(run, 0, null, refresh)
    })
  }

  onBeforeUnmount(cancel)

  return {
    dashboard,
    loading,
    requestFailed,
    refreshState,
    load,
    cancel,
  }
}
