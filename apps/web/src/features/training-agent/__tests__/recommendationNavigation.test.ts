import { describe, expect, it } from 'vitest'
import type { TrainingRecommendationItem } from '../api/trainingAgentApi'
import { completedTrainingTarget, recommendationTarget } from '../model/recommendationNavigation'

function recommendation(trainingType: string, action: Record<string, unknown>, dimensionCode = 'GENERAL.ANSWER_STRUCTURE'):
  TrainingRecommendationItem {
  return {
    revision: 1,
    trainingType,
    dimensionCode,
    title: '训练建议',
    reason: '测试原因',
    estimatedMinutes: 10,
    action,
    evidenceCount: 1,
  }
}

describe('recommendationTarget', () => {
  it('uses one standard home target after any completed training', () => {
    expect(completedTrainingTarget).toEqual({
      name: 'home',
      query: { trainingCompleted: '1' },
    })
  })

  it('maps knowledge presets without starting a session', () => {
    expect(recommendationTarget(recommendation('KNOWLEDGE', {
      module: 'JAVA', difficulty: 'mixed', questionCount: 3,
    }, 'KNOWLEDGE.JAVA'))).toEqual({
      name: 'knowledge-practice',
      query: { module: 'Java', difficulty: 'mixed', questionCount: '3' },
    })
  })

  it('maps algorithm filters and the target dimension', () => {
    expect(recommendationTarget(recommendation('ALGORITHM', {
      difficulty: 'medium', tags: ['hash'], dimensionCode: 'ALGORITHM.COMPLEXITY',
    }, 'ALGORITHM.COMPLEXITY'))).toEqual({
      name: 'algorithm-selection',
      query: { difficulty: 'medium', tag: 'hash', dimension: 'ALGORITHM.COMPLEXITY' },
    })
  })

  it('opens an existing project profile in the preparation page and keeps its context', () => {
    expect(recommendationTarget(recommendation('PROJECT_DEEP_DIVE', {
      profileId: 27, targetDimension: 'PROJECT.TRADEOFF',
    }, 'PROJECT.TRADEOFF'))).toEqual({
      name: 'project-deep-dive-profile',
      params: { profileId: '27' },
      query: { targetDimension: 'PROJECT.TRADEOFF', profileId: '27' },
    })
  })

  it('drops unsupported preset values', () => {
    expect(recommendationTarget(recommendation('KNOWLEDGE', {
      difficulty: 'impossible', questionCount: 99,
    }))).toEqual({ name: 'knowledge-practice', query: {} })
  })
})
