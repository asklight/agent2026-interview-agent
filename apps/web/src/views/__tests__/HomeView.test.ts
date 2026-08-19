import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from '../HomeView.vue'
import { getTrainingAgentDashboard, type TrainingAgentDashboard, type TrainingRecommendationItem } from '@/features/training-agent/api/trainingAgentApi'

vi.mock('@/features/training-agent/api/trainingAgentApi', () => ({
  getTrainingAgentDashboard: vi.fn(),
}))

function response(data: TrainingAgentDashboard) { return { data: { data } } as never }

function item(trainingType: string, title: string, dimensionCode: string, action: Record<string, unknown> = {}):
  TrainingRecommendationItem {
  return { revision: 1, trainingType, title, dimensionCode, action, reason: `${title}的推荐原因`, estimatedMinutes: 15, evidenceCount: 2 }
}

function dashboard(overrides: Partial<TrainingAgentDashboard> = {}): TrainingAgentDashboard {
  return {
    enabled: true,
    degraded: false,
    state: 'READY',
    primaryRecommendation: item('ALGORITHM', '复杂度专项', 'ALGORITHM.COMPLEXITY', {
      difficulty: 'medium', tag: 'array', dimensionCode: 'ALGORITHM.COMPLEXITY',
    }),
    alternatives: [],
    focusDimensions: [],
    recentProgress: null,
    generatedAt: '2026-08-19T10:00:00',
    ...overrides,
  }
}

function mountHome() {
  return mount(HomeView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        ArrowRight: true,
        Briefcase: true,
        Collection: true,
        DataAnalysis: true,
        VideoCamera: true,
      },
    },
  })
}

function primaryLink(wrapper: ReturnType<typeof mountHome>) {
  return wrapper.findAllComponents(RouterLinkStub).find(link => link.classes().includes('primary-link'))!
}

describe('HomeView training agent dashboard', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows cold start separately and carries the calibration preset', async () => {
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(dashboard({
      state: 'COLD_START', primaryRecommendation: null,
    })))
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('先做一次基础校准')
    expect(primaryLink(wrapper).props('to')).toEqual({
      name: 'knowledge-practice',
      query: { module: 'Java', difficulty: 'mixed', questionCount: '3' },
    })
  })

  it('shows one primary recommendation, independent focus and progress, and at most two other modules', async () => {
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(dashboard({
      alternatives: [
        item('ALGORITHM', '同类算法备选', 'ALGORITHM.EDGE_CASE'),
        item('PROJECT_DEEP_DIVE', '项目取舍练习', 'PROJECT.TRADEOFF', { profileId: 8 }),
        item('KNOWLEDGE', 'MySQL 校准', 'KNOWLEDGE.MYSQL', { module: 'MYSQL', questionCount: 3 }),
        item('COMPREHENSIVE_SIMULATION', '综合模拟', 'GENERAL.EVIDENCE'),
      ],
      focusDimensions: [
        { dimensionCode: 'ALGORITHM.COMPLEXITY', label: '复杂度分析', sourceType: 'ALGORITHM', abilityState: 'NEEDS_WORK', evidenceCount: 3, lastObservedAt: null },
        { dimensionCode: 'PROJECT.TRADEOFF', label: '方案取舍', sourceType: 'PROJECT_DEEP_DIVE', abilityState: 'DEVELOPING', evidenceCount: 2, lastObservedAt: null },
        { dimensionCode: 'KNOWLEDGE.MYSQL', label: 'MySQL', sourceType: 'KNOWLEDGE', abilityState: 'STABLE', evidenceCount: 4, lastObservedAt: null },
        { dimensionCode: 'GENERAL.EVIDENCE', label: '不应显示的第四项', sourceType: 'KNOWLEDGE', abilityState: 'UNKNOWN', evidenceCount: 0, lastObservedAt: null },
      ],
      recentProgress: { dimensionCode: 'KNOWLEDGE.MYSQL', label: 'MySQL', sourceType: 'KNOWLEDGE', abilityState: 'STABLE', lastObservedAt: null },
    })))
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('今天最值得练：复杂度专项')
    expect(wrapper.text()).toContain('当前训练重点')
    expect(wrapper.text()).toContain('“MySQL”已经形成较稳定的表现。')
    expect(wrapper.text()).not.toContain('不应显示的第四项')
    expect(wrapper.text()).not.toContain('同类算法备选')
    expect(wrapper.text()).toContain('项目取舍练习')
    expect(wrapper.text()).toContain('MySQL 校准')
    expect(wrapper.text()).not.toContain('综合模拟的推荐原因')
    expect(primaryLink(wrapper).props('to')).toEqual({
      name: 'algorithm-selection',
      query: { difficulty: 'medium', tag: 'array', dimension: 'ALGORITHM.COMPLEXITY' },
    })
  })

  it('keeps all module entries and shows a useful fallback when the request fails', async () => {
    vi.mocked(getTrainingAgentDashboard).mockRejectedValue(new Error('network unavailable'))
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('个性化推荐暂时没有连接上')
    expect(wrapper.findAll('.training-entry')).toHaveLength(4)
    expect(wrapper.find('.agent-recommendation').exists()).toBe(false)
  })

  it('uses the last available recommendation in degraded state', async () => {
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(dashboard({ state: 'DEGRADED', degraded: true })))
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('今天最值得练：复杂度专项')
    expect(wrapper.text()).toContain('推荐同步暂时延迟')
  })

  it('hides the agent region when the feature is disabled', async () => {
    vi.mocked(getTrainingAgentDashboard).mockResolvedValue(response(dashboard({
      enabled: false, state: 'DISABLED', primaryRecommendation: null,
    })))
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.find('.agent-recommendation').exists()).toBe(false)
    expect(wrapper.find('.agent-degraded').exists()).toBe(false)
    expect(wrapper.findAll('.training-entry')).toHaveLength(4)
  })
})
