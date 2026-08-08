import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AlgorithmSelectionView from '../views/AlgorithmSelectionView.vue'
import { createAlgorithmSession, getAlgorithmProblems } from '../api/algorithmApi'

const push = vi.fn()

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/algorithmApi', () => ({
  createAlgorithmSession: vi.fn(),
  getAlgorithmProblems: vi.fn(),
}))

const problems = [
  { id: 1, code: 'two-sum', title: '两数之和', statement: '找到目标和', difficulty: 'easy', tags: ['hash'], constraints: [] },
  { id: 2, code: 'lru', title: 'LRU 缓存', statement: '设计缓存', difficulty: 'medium', tags: ['design'], constraints: [] },
]

function response(data: unknown) { return { data: { data } } as never }

describe('AlgorithmSelectionView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(getAlgorithmProblems).mockResolvedValue(response(problems))
    vi.mocked(createAlgorithmSession).mockResolvedValue(response({ sessionId: 42 }))
  })

  it('filters problems and starts the selected oral interview', async () => {
    const wrapper = mount(AlgorithmSelectionView, {
      global: {
        stubs: {
          'el-button': { emits: ['click'], template: '<button @click="$emit(\'click\')"><slot /></button>' },
          Right: true,
        },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('两数之和')
    expect(wrapper.text()).toContain('LRU 缓存')

    const medium = wrapper.findAll('button').find(button => button.text() === '中等')!
    await medium.trigger('click')
    expect(wrapper.text()).not.toContain('两数之和')
    expect(wrapper.text()).toContain('LRU 缓存')

    const start = wrapper.findAll('button').find(button => button.text().includes('开始口述'))!
    await start.trigger('click')
    await flushPromises()

    expect(createAlgorithmSession).toHaveBeenCalledWith(2)
    expect(push).toHaveBeenCalledWith({ name: 'algorithm-room', params: { sessionId: 42 } })
  })
})
