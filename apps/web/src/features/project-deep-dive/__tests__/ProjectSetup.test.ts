import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { flushPromises, shallowMount } from '@vue/test-utils'
import ProjectSetup from '@/features/project-deep-dive/views/ProjectSetup.vue'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'
import type { ProjectAnalysisStatus, ProjectProfile } from '@/features/project-deep-dive/model/types'
import { analyzeProjectProfile, getProjectProfile } from '@/features/project-deep-dive/api/projectDeepDiveApi'

const replace = vi.fn()
const push = vi.fn()
let pinia: Pinia

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRouter: () => ({ replace, push }),
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

vi.mock('@/features/project-deep-dive/api/projectDeepDiveApi', () => ({
  analyzeProjectProfile: vi.fn(),
  confirmProjectProfile: vi.fn(),
  createProjectProfile: vi.fn(),
  getProjectProfile: vi.fn(),
  patchProjectProfile: vi.fn(),
}))

vi.mock('@/features/project-deep-dive/api/interviewApi', () => ({
  createProjectInterview: vi.fn(),
}))

function profile(profileId: number, projectName: string, analysisStatus: ProjectAnalysisStatus,
  updateTime = new Date().toISOString()): ProjectProfile {
  return {
    profileId,
    sanitizedDescription: `项目 ${profileId} 的脱敏原文`,
    projectName,
    summary: `${projectName}摘要`,
    techStack: ['Spring Boot'],
    responsibilities: ['负责核心模块'],
    metrics: [],
    architecture: ['单体服务'],
    uncertainties: [],
    analysisStatus,
    version: 2,
    claims: [{
      claimId: profileId * 10,
      claimType: 'RESPONSIBILITY',
      statement: '负责核心模块',
      sourceFragment: '负责核心模块',
      relatedTechnologies: ['Spring Boot'],
      confirmed: analysisStatus === 'READY',
    }],
    createTime: updateTime,
    updateTime,
  }
}

function response(value: ProjectProfile) {
  return { data: { data: value } } as never
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((done, fail) => { resolve = done; reject = fail })
  return { promise, resolve, reject }
}

function mountSetup(profileId: string) {
  return shallowMount(ProjectSetup, {
    props: { profileId },
    global: {
      plugins: [pinia],
      stubs: {
        ProjectExtractionReview: {
          props: ['form', 'readonly'],
          template: '<div data-testid="review" :data-readonly="String(readonly)">{{ form.projectName }}</div>',
        },
        'el-button': {
          emits: ['click'],
          template: '<button @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

describe('ProjectSetup recovery and route reuse', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.sessionStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    const store = useInterviewSessionStore()
    store.saveProfileAccess(1, 'token-1')
    store.saveProfileAccess(2, 'token-2')
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('loads the new profile when a reused route changes its profile id', async () => {
    vi.mocked(getProjectProfile).mockImplementation((profileId) => Promise.resolve(response(
      profile(profileId, profileId === 1 ? '项目一' : '项目二', 'REVIEW_REQUIRED'),
    )))
    const wrapper = mountSetup('1')
    await flushPromises()
    expect(wrapper.get('[data-testid="review"]').text()).toBe('项目一')

    await wrapper.setProps({ profileId: '2' })
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('项目二')
    expect(getProjectProfile).toHaveBeenNthCalledWith(1, 1, 'token-1')
    expect(getProjectProfile).toHaveBeenNthCalledWith(2, 2, 'token-2')
  })

  it('reconnects after an initial profile load failure', async () => {
    vi.mocked(getProjectProfile)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(response(profile(1, '重连后的档案', 'REVIEW_REQUIRED')))
    const wrapper = mountSetup('1')
    await flushPromises()
    expect(wrapper.text()).toContain('暂时无法读取项目档案')

    const reconnect = wrapper.findAll('button').find(button => button.text().includes('重新连接'))
    await reconnect!.trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('重连后的档案')
    expect(getProjectProfile).toHaveBeenCalledTimes(2)
  })

  it('reconciles with GET when analyze succeeded but its response was lost', async () => {
    vi.mocked(getProjectProfile)
      .mockResolvedValueOnce(response(profile(1, '分析前', 'DRAFT')))
      .mockResolvedValueOnce(response(profile(1, '已恢复结果', 'REVIEW_REQUIRED')))
    vi.mocked(analyzeProjectProfile).mockRejectedValueOnce(new Error('response lost'))
    const wrapper = mountSetup('1')
    await flushPromises()

    const retry = wrapper.findAll('button').find(button => button.text().includes('重新分析'))
    expect(retry).toBeTruthy()
    await retry!.trigger('click')
    await flushPromises()

    expect(analyzeProjectProfile).toHaveBeenCalledWith(1, 'token-1')
    expect(getProjectProfile).toHaveBeenCalledTimes(2)
    expect(wrapper.get('[data-testid="review"]').text()).toBe('已恢复结果')
  })

  it('allows a ready profile to enter editing mode without recreating it', async () => {
    vi.mocked(getProjectProfile).mockResolvedValueOnce(response(profile(1, '已确认项目', 'READY')))
    const wrapper = mountSetup('1')
    await flushPromises()
    expect(wrapper.get('[data-testid="review"]').attributes('data-readonly')).toBe('true')

    const edit = wrapper.findAll('button').find(button => button.text().includes('编辑档案'))
    await edit!.trigger('click')

    expect(wrapper.get('[data-testid="review"]').attributes('data-readonly')).toBe('false')
    expect(wrapper.text()).toContain('确认并准备面试')
  })

  it('polls an in-progress analysis until the recovered result is ready', async () => {
    vi.mocked(getProjectProfile)
      .mockResolvedValueOnce(response(profile(1, '分析前', 'DRAFT')))
      .mockResolvedValueOnce(response(profile(1, '处理中', 'ANALYZING')))
      .mockResolvedValueOnce(response(profile(1, '轮询恢复结果', 'REVIEW_REQUIRED')))
    vi.mocked(analyzeProjectProfile).mockRejectedValueOnce(new Error('response lost'))
    const wrapper = mountSetup('1')
    await flushPromises()
    vi.useFakeTimers()

    const retry = wrapper.findAll('button').find(button => button.text().includes('重新分析'))
    await retry!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('正在拆解项目事实')

    await vi.advanceTimersByTimeAsync(2_500)
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('轮询恢复结果')
    expect(getProjectProfile).toHaveBeenCalledTimes(3)
    wrapper.unmount()
  })

  it('starts recovery polling when a refreshed profile is already analyzing', async () => {
    vi.useFakeTimers()
    vi.mocked(getProjectProfile)
      .mockResolvedValueOnce(response(profile(1, '刷新时处理中', 'ANALYZING')))
      .mockResolvedValueOnce(response(profile(1, '刷新后恢复结果', 'REVIEW_REQUIRED')))
    const wrapper = mountSetup('1')
    await flushPromises()
    expect(wrapper.text()).toContain('正在拆解项目事实')

    await vi.advanceTimersByTimeAsync(2_500)
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('刷新后恢复结果')
    expect(getProjectProfile).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('offers recovery immediately when the server analysis lease is already stale', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-03T10:05:00Z'))
    vi.mocked(getProjectProfile).mockResolvedValueOnce(response(
      profile(1, '超时的档案', 'ANALYZING', '2026-08-03T10:00:00Z'),
    ))

    const wrapper = mountSetup('1')
    await flushPromises()

    expect(wrapper.text()).toContain('项目分析等待时间较长')
    expect(wrapper.text()).toContain('检查并重新分析')
    expect(getProjectProfile).toHaveBeenCalledTimes(1)
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('interprets an unzoned backend timestamp in the server time zone', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-03T10:05:00Z'))
    vi.mocked(getProjectProfile).mockResolvedValueOnce(response(
      profile(1, 'stale profile', 'ANALYZING', '2026-08-03T18:00:00'),
    ))

    const wrapper = mountSetup('1')
    await flushPromises()

    expect(wrapper.text()).toContain('项目分析等待时间较长')
    expect(vi.getTimerCount()).toBe(0)
    wrapper.unmount()
  })

  it('ignores a late analysis response after navigating to another profile', async () => {
    vi.mocked(getProjectProfile).mockImplementation(profileId => Promise.resolve(response(
      profile(profileId, profileId === 1 ? '旧档案' : '当前档案',
        profileId === 1 ? 'DRAFT' : 'REVIEW_REQUIRED'),
    )))
    const pendingAnalysis = deferred<ReturnType<typeof response>>()
    vi.mocked(analyzeProjectProfile).mockReturnValue(pendingAnalysis.promise)
    const wrapper = mountSetup('1')
    await flushPromises()

    const retry = wrapper.findAll('button').find(button => button.text().includes('重新分析'))
    await retry!.trigger('click')
    await wrapper.setProps({ profileId: '2' })
    await flushPromises()
    expect(wrapper.get('[data-testid="review"]').text()).toBe('当前档案')

    pendingAnalysis.resolve(response(profile(1, '迟到的旧结果', 'REVIEW_REQUIRED')))
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('当前档案')
  })

  it('keeps the new profile polling when an old polling request fails late', async () => {
    vi.useFakeTimers()
    const oldPoll = deferred<ReturnType<typeof response>>()
    let profileTwoLoads = 0
    vi.mocked(getProjectProfile).mockImplementation(profileId => {
      if (profileId === 1) {
        if (vi.mocked(getProjectProfile).mock.calls.filter(call => call[0] === 1).length === 1) {
          return Promise.resolve(response(profile(1, '旧档案处理中', 'ANALYZING')))
        }
        return oldPoll.promise
      }
      profileTwoLoads++
      return Promise.resolve(response(profile(2,
        profileTwoLoads === 1 ? '新档案处理中' : '新档案已恢复',
        profileTwoLoads === 1 ? 'ANALYZING' : 'REVIEW_REQUIRED')))
    })
    const wrapper = mountSetup('1')
    await flushPromises()

    vi.advanceTimersByTime(2_500)
    await Promise.resolve()
    await wrapper.setProps({ profileId: '2' })
    await flushPromises()
    oldPoll.reject(new Error('late failure'))
    await flushPromises()

    await vi.advanceTimersByTimeAsync(2_500)
    await flushPromises()

    expect(wrapper.get('[data-testid="review"]').text()).toBe('新档案已恢复')
    wrapper.unmount()
  })
})
