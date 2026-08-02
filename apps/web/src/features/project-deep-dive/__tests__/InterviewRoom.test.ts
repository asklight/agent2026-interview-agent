import { computed, ref } from 'vue'
import { createPinia } from 'pinia'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import InterviewComposer from '@/features/project-deep-dive/components/InterviewComposer.vue'
import InterviewRoom from '@/features/project-deep-dive/views/InterviewRoom.vue'
import type { ProjectInterviewSession } from '@/features/project-deep-dive/model/types'

const router = vi.hoisted(() => ({ replace: vi.fn(), push: vi.fn() }))
const interviewSession = vi.hoisted(() => ({ use: vi.fn() }))

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRouter: () => router,
}))

vi.mock('element-plus', () => ({
  ElMessageBox: { confirm: vi.fn() },
}))

vi.mock('@/features/project-deep-dive/composables/useInterviewSession', () => ({
  useInterviewSession: interviewSession.use,
}))

const baseSession: ProjectInterviewSession = {
  sessionId: 7,
  mode: 'PROJECT_DEEP_DIVE',
  status: 'IN_PROGRESS',
  conversationPhase: 'CLAIM_DEEP_DIVE',
  currentProbeDimension: 'OWNERSHIP',
  completedProbeCount: 1,
  totalProbeCount: 6,
  maxFollowUpsPerClaim: 3,
  inputModality: 'TEXT',
  turnState: 'IDLE',
  turns: [],
}

function state(overrides: { session?: ProjectInterviewSession; submitting?: boolean; load?: () => Promise<unknown> } = {}) {
  const session = ref<ProjectInterviewSession | null>(overrides.session || { ...baseSession })
  const submitting = ref(Boolean(overrides.submitting))
  return {
    session,
    loading: ref(false),
    submitting,
    finishing: ref(false),
    errorMessage: ref(''),
    pendingSubmission: ref(null),
    hasAccess: computed(() => true),
    isThinking: computed(() => submitting.value || session.value?.turnState === 'PROCESSING'),
    load: vi.fn(overrides.load || (() => Promise.resolve(session.value))),
    submit: vi.fn(),
    retryPending: vi.fn(),
    finish: vi.fn(),
  }
}

function mountRoom() {
  return shallowMount(InterviewRoom, {
    props: { sessionId: '7' },
    global: {
      plugins: [createPinia()],
      stubs: {
        RouterLink: { template: '<a><slot /></a>' },
        'el-button': {
          props: ['disabled', 'loading'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

describe('InterviewRoom recovery lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('locks answer editing and finishing while a submission is in flight', async () => {
    const current = state({ submitting: true })
    interviewSession.use.mockReturnValue(current)
    const wrapper = mountRoom()
    await flushPromises()

    expect(wrapper.findComponent(InterviewComposer).props('disabled')).toBe(true)
    expect(wrapper.find('header button').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('does not create recovery timers after unmounting during the initial load', async () => {
    vi.useFakeTimers()
    let resolveLoad!: () => void
    const current = state({ load: () => new Promise<void>(resolve => { resolveLoad = resolve }) })
    interviewSession.use.mockReturnValue(current)
    const wrapper = mountRoom()
    expect(current.load).toHaveBeenCalledTimes(1)

    wrapper.unmount()
    resolveLoad()
    await flushPromises()
    vi.advanceTimersByTime(5_000)

    expect(vi.getTimerCount()).toBe(0)
    expect(current.load).toHaveBeenCalledTimes(1)
  })

  it('navigates to the report when polling observes a finished session', async () => {
    const current = state({ session: { ...baseSession, turnState: 'PROCESSING' } })
    interviewSession.use.mockReturnValue(current)
    const wrapper = mountRoom()
    await flushPromises()

    current.session.value = { ...baseSession, status: 'FINISHED' }
    await flushPromises()

    expect(router.replace).toHaveBeenCalledWith({
      name: 'project-interview-report',
      params: { sessionId: 7 },
    })
    wrapper.unmount()
  })
})
