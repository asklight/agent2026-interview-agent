import http from '@/api/http'
import type { ApiResponse } from '@/api/http'

export interface TrainingRecommendationItem {
  revision: number
  trainingType: string
  dimensionCode: string
  title: string
  reason: string
  estimatedMinutes: number
  action: Record<string, unknown>
  evidenceCount: number
}

export interface AbilityFocus {
  dimensionCode: string
  label: string
  sourceType: string
  abilityState: string
  evidenceCount: number
  lastObservedAt: string | null
}

export interface RecentProgress {
  dimensionCode: string
  label: string
  sourceType: string
  abilityState: string
  lastObservedAt: string | null
}

export type TrainingAgentDashboardState = 'COLD_START' | 'READY' | 'DISABLED' | 'DEGRADED'

export interface TrainingAgentDashboard {
  enabled: boolean
  degraded: boolean
  state: TrainingAgentDashboardState
  primaryRecommendation: TrainingRecommendationItem | null
  alternatives: TrainingRecommendationItem[]
  focusDimensions: AbilityFocus[]
  recentProgress: RecentProgress | null
  generatedAt: string | null
}

export function getTrainingAgentDashboard() {
  return http.get<ApiResponse<TrainingAgentDashboard>>('/training-agent/dashboard')
}
