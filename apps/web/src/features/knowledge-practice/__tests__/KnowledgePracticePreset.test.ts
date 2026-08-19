import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import KnowledgePracticeView from '../views/KnowledgePracticeView.vue'
import { getHealth } from '@/api/modules/health'
import { getQuestionModules } from '@/api/modules/questionCard'
import { createInterviewSession } from '@/api/modules/interview'

const { routeQuery } = vi.hoisted(() => ({ routeQuery: {} as Record<string, string> }))

vi.mock('vue-router', () => ({ useRoute: () => ({ query: routeQuery }) }))
vi.mock('@/api/modules/health', () => ({ getHealth: vi.fn() }))
vi.mock('@/api/modules/questionCard', () => ({ getQuestionModules: vi.fn() }))
vi.mock('@/api/modules/interview', () => ({
  createInterviewSession: vi.fn(),
  finishInterviewSession: vi.fn(),
  getInterviewReport: vi.fn(),
  nextInterviewQuestion: vi.fn(),
  submitInterviewAnswer: vi.fn(),
}))

function response(data: unknown) { return { data: { data } } as never }

describe('KnowledgePracticeView recommendation presets', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    Object.keys(routeQuery).forEach(key => delete routeQuery[key])
    vi.mocked(getHealth).mockResolvedValue(response('ok'))
    vi.mocked(getQuestionModules).mockResolvedValue(response(['Java', 'MySQL', 'OperatingSystem']))
  })

  it('prefills a valid recommendation but waits for the user to start', async () => {
    routeQuery.module = 'MySQL'
    routeQuery.difficulty = 'hard'
    routeQuery.questionCount = '7'
    const wrapper = shallowMount(KnowledgePracticeView, {
      global: {
        stubs: {
          'el-select': { name: 'ElSelect', props: ['modelValue'], template: '<div class="module-value">{{ modelValue }}</div>' },
          'el-radio-group': { name: 'ElRadioGroup', props: ['modelValue'], template: '<div class="difficulty-value">{{ modelValue }}<slot /></div>' },
          'el-slider': { name: 'ElSlider', props: ['modelValue'], template: '<div class="question-count">{{ modelValue }}</div>' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.module-value').text()).toBe('MySQL')
    expect(wrapper.find('.difficulty-value').text()).toContain('hard')
    expect(wrapper.find('.question-count').text()).toBe('7')
    expect(createInterviewSession).not.toHaveBeenCalled()
  })
})
