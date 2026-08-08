import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SimulationSetupView from '../views/SimulationSetupView.vue'
import { createSimulation, getSimulationOptions } from '../api/simulationApi'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
}))
vi.mock('../api/simulationApi', () => ({
  createSimulation: vi.fn(),
  getSimulationOptions: vi.fn(),
}))

function response(data: unknown) { return { data: { data } } as never }

describe('SimulationSetupView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getSimulationOptions).mockResolvedValue(response({
      projects: [{ id: 8, name: 'RegPilot', summary: '注册中心治理', techStack: ['Java', 'MySQL'] }],
      algorithmProblems: [{ id: 9, title: 'LRU 缓存', difficulty: 'medium', code: 'lru', statement: '', tags: [], constraints: [] }],
      knowledgeModules: ['JAVA'],
      difficulties: ['MEDIUM'],
    }))
    vi.mocked(createSimulation).mockResolvedValue(response({ simulationId: 50 }))
  })

  it('loads defaults and creates one comprehensive interview', async () => {
    const wrapper = mount(SimulationSetupView, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
          'el-select': { template: '<div><slot /></div>' },
          'el-option': true,
          'el-button': { template: '<button><slot /></button>' },
          Briefcase: true,
          VideoPlay: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('RegPilot')
    expect(wrapper.text()).toContain('约 20 分钟')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createSimulation).toHaveBeenCalledWith({
      clientRequestId: expect.any(String),
      projectProfileId: 8,
      algorithmProblemId: 9,
      knowledgeModule: 'JAVA',
      difficulty: 'MEDIUM',
    })
    expect(push).toHaveBeenCalledWith('/simulation/50')
  })
})
