import type { RouteLocationRaw } from 'vue-router'
import type { TrainingRecommendationItem } from '../api/trainingAgentApi'

type RecommendationAction = Record<string, unknown>

const knowledgeModules: Record<string, string> = {
  JAVA: 'Java',
  MYSQL: 'MySQL',
  REDIS: 'Redis',
  SPRING: 'Spring',
  NETWORK: 'Network',
  OS: 'OperatingSystem',
  OPERATING_SYSTEM: 'OperatingSystem',
  OPERATINGSYSTEM: 'OperatingSystem',
}

function scalar(value: unknown) {
  if (typeof value === 'string') return value.trim() || undefined
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return undefined
}

function positiveInteger(value: unknown) {
  const parsed = Number(scalar(value))
  return Number.isInteger(parsed) && parsed > 0 ? String(parsed) : undefined
}

function difficulty(value: unknown, allowMixed = false) {
  const normalized = scalar(value)?.toLowerCase()
  const allowed = allowMixed ? ['easy', 'medium', 'hard', 'mixed'] : ['easy', 'medium', 'hard']
  return normalized && allowed.includes(normalized) ? normalized : undefined
}

function knowledgeModule(value: unknown) {
  const raw = scalar(value)
  if (!raw) return undefined
  return knowledgeModules[raw.replace(/\./g, '_').toUpperCase()]
}

function dimension(action: RecommendationAction, fallback?: string) {
  return scalar(action.targetDimension) ?? scalar(action.dimension) ?? scalar(action.dimensionCode) ?? scalar(fallback)
}

function compactQuery(query: Record<string, string | undefined>) {
  return Object.fromEntries(Object.entries(query).filter((entry): entry is [string, string] => Boolean(entry[1])))
}

export function recommendationTarget(item: TrainingRecommendationItem): RouteLocationRaw {
  const action = item.action ?? {}
  const targetDimension = dimension(action, item.dimensionCode)

  if (item.trainingType === 'PROJECT_DEEP_DIVE') {
    const profileId = positiveInteger(action.profileId)
    const query = compactQuery({ targetDimension, profileId })
    return profileId
      ? { name: 'project-deep-dive-profile', params: { profileId }, query }
      : { name: 'project-deep-dive-new', query }
  }

  if (item.trainingType === 'ALGORITHM') {
    return {
      name: 'algorithm-selection',
      query: compactQuery({
        difficulty: difficulty(action.difficulty),
        tag: scalar(action.tag) ?? (Array.isArray(action.tags) ? scalar(action.tags[0]) : undefined),
        dimension: targetDimension,
      }),
    }
  }

  if (item.trainingType === 'COMPREHENSIVE_SIMULATION') {
    return {
      name: 'simulation-setup',
      query: compactQuery({ targetDimension }),
    }
  }

  const count = Number(scalar(action.questionCount))
  const questionCount = Number.isInteger(count) && count >= 3 && count <= 10 ? String(count) : undefined
  return {
    name: 'knowledge-practice',
    query: compactQuery({
      module: knowledgeModule(action.module),
      difficulty: difficulty(action.difficulty, true),
      questionCount,
    }),
  }
}

export const coldStartTarget: RouteLocationRaw = {
  name: 'knowledge-practice',
  query: { module: 'Java', difficulty: 'mixed', questionCount: '3' },
}
