import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import InterviewMessageList from '@/features/project-deep-dive/components/InterviewMessageList.vue'
import type { PublicInterviewTurn } from '@/features/project-deep-dive/model/types'

describe('InterviewMessageList', () => {
  it('renders only public conversation fields and never internal evaluation data', () => {
    const turn = {
      turnId: 1,
      sequenceNo: 1,
      role: 'INTERVIEWER',
      turnType: 'FOLLOW_UP',
      content: '你提到延迟降低了 60%，这个指标的基线和采样周期是什么？',
      inputModality: 'TEXT',
      startedAt: null,
      endedAt: null,
      createTime: '2026-07-11T10:00:00',
      score: '99 INTERNAL SCORE',
      hitPoints: ['INTERNAL HIT POINT'],
      missingPoints: ['INTERNAL MISSING POINT'],
      decision: 'SWITCH_CLAIM_INTERNAL',
      modelRawResponse: 'RAW MODEL SECRET',
    } as unknown as PublicInterviewTurn

    const wrapper = mount(InterviewMessageList, { props: { turns: [turn] } })
    const text = wrapper.text()

    expect(text).toContain('这个指标的基线和采样周期是什么')
    expect(text).not.toContain('99 INTERNAL SCORE')
    expect(text).not.toContain('INTERNAL HIT POINT')
    expect(text).not.toContain('INTERNAL MISSING POINT')
    expect(text).not.toContain('SWITCH_CLAIM_INTERNAL')
    expect(text).not.toContain('RAW MODEL SECRET')
  })
})
