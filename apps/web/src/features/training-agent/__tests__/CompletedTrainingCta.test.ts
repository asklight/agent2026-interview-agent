import { flushPromises, RouterLinkStub, shallowMount, type VueWrapper } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InterviewReport from '@/features/project-deep-dive/views/InterviewReport.vue'
import AlgorithmReportView from '@/features/algorithm-practice/views/AlgorithmReportView.vue'
import SimulationReportView from '@/features/interview-simulation/views/SimulationReportView.vue'
import KnowledgePracticeView from '@/features/knowledge-practice/views/KnowledgePracticeView.vue'
import { getProjectInterviewReport } from '@/features/project-deep-dive/api/interviewApi'
import { getAlgorithmReport } from '@/features/algorithm-practice/api/algorithmApi'
import { getSimulationReport } from '@/features/interview-simulation/api/simulationApi'
import { getHealth } from '@/api/modules/health'
import { getQuestionModules } from '@/api/modules/questionCard'
import { createInterviewSession, finishInterviewSession, getInterviewReport } from '@/api/modules/interview'

const { routeQuery } = vi.hoisted(() => ({ routeQuery: {} as Record<string, string> }))

vi.mock('vue-router', async (importOriginal) => ({
  ...await importOriginal<typeof import('vue-router')>(),
  useRoute: () => ({ query: routeQuery }),
}))
vi.mock('@/features/project-deep-dive/api/interviewApi', () => ({ getProjectInterviewReport: vi.fn() }))
vi.mock('@/features/project-deep-dive/stores/interviewSession', () => ({
  useInterviewSessionStore: () => ({ accessToken: 'project-token', restoreSessionAccess: vi.fn() }),
}))
vi.mock('@/features/algorithm-practice/api/algorithmApi', () => ({ getAlgorithmReport: vi.fn() }))
vi.mock('@/features/interview-simulation/api/simulationApi', () => ({ getSimulationReport: vi.fn() }))
vi.mock('@/api/modules/health', () => ({ getHealth: vi.fn() }))
vi.mock('@/api/modules/questionCard', () => ({ getQuestionModules: vi.fn() }))
vi.mock('@/api/modules/interview', () => ({
  createInterviewSession: vi.fn(),
  finishInterviewSession: vi.fn(),
  getInterviewReport: vi.fn(),
  nextInterviewQuestion: vi.fn(),
  submitInterviewAnswer: vi.fn(),
}))

const target = { name: 'home', query: { trainingCompleted: '1' } }
function response(data: unknown) { return { data: { data } } as never }
function nextTrainingTarget(wrapper: VueWrapper) {
  return wrapper.findAllComponents(RouterLinkStub)
    .find(link => link.classes().includes('next-training-link'))!
    .props('to')
}

describe('completed training report CTA', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getHealth).mockResolvedValue(response('ok'))
    vi.mocked(getQuestionModules).mockResolvedValue(response(['Java']))
  })

  it('links the project report to the refreshed training home', async () => {
    vi.mocked(getProjectInterviewReport).mockResolvedValue(response({
      schemaVersion: 1, sessionId: 11, mode: 'PROJECT_DEEP_DIVE', generationStatus: 'COMPLETED',
      totalScore: null, coverageRate: 50, dimensions: [], strengths: [], risks: [], weaknesses: [],
      recommendations: [], claimReviews: [], rounds: [], generatedAt: '2026-08-19T10:00:00',
    }))
    const wrapper = shallowMount(InterviewReport, {
      props: { sessionId: '11' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(nextTrainingTarget(wrapper)).toEqual(target)
  })

  it('links the algorithm report to the refreshed training home', async () => {
    vi.mocked(getAlgorithmReport).mockResolvedValue(response({
      schemaVersion: 1, sessionId: 12, completionStatus: 'COMPLETE', overallScore: 80,
      coverage: 75, dimensions: [], strengths: [], gaps: [], recommendations: [], rounds: [],
      generatedAt: '2026-08-19T10:00:00',
    }))
    const wrapper = shallowMount(AlgorithmReportView, {
      props: { sessionId: '12' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(nextTrainingTarget(wrapper)).toEqual(target)
  })

  it('links the simulation report to the refreshed training home', async () => {
    vi.mocked(getSimulationReport).mockResolvedValue(response({
      schemaVersion: 1, simulationId: 13, completionStatus: 'COMPLETE', stages: [], recommendations: [],
      generatedAt: '2026-08-19T10:00:00',
    }))
    const wrapper = shallowMount(SimulationReportView, {
      props: { sessionId: '13' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(nextTrainingTarget(wrapper)).toEqual(target)
  })

  it('links the completed knowledge report to the refreshed training home', async () => {
    vi.mocked(createInterviewSession).mockResolvedValue(response({
      sessionId: 14, mode: 'KNOWLEDGE', module: 'Java', difficulty: 'medium', questionCount: 3,
      completedQuestionCount: 1, status: 'IN_PROGRESS', currentQuestion: {
        questionId: 101, cardCode: 'JAVA-101', module: 'Java', difficulty: 'medium', questionType: 'MAIN',
        questionText: '什么是线程安全？', mainQuestion: '什么是线程安全？', tags: 'concurrency',
        completedQuestionCount: 1, questionCount: 3,
      },
    }))
    vi.mocked(finishInterviewSession).mockResolvedValue(response({}))
    vi.mocked(getInterviewReport).mockResolvedValue(response({
      sessionId: 14, totalScore: 80, scoreLevel: '良好', answeredCount: 1,
      strengths: [], weaknesses: [], recommendations: [], generatedAt: '2026-08-19T10:00:00',
    }))
    const wrapper = shallowMount(KnowledgePracticeView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          'el-button': {
            name: 'ElButton',
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
          },
        },
      },
    })
    await flushPromises()
    await wrapper.find('.start-button').trigger('click')
    await flushPromises()
    await wrapper.find('.answer-card .answer-actions button').trigger('click')
    await flushPromises()

    expect(nextTrainingTarget(wrapper)).toEqual(target)
  })
})
