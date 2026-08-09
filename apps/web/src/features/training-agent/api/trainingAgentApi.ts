import http from '@/api/http'
import type { ApiResponse } from '@/api/http'

export interface TrainingRecommendationItem {
  trainingType: string
  dimensionCode: string
  title: string
  reason: string
  estimatedMinutes: number
  action: Record<string, unknown>
  evidenceIds: number[]
}

export interface AbilityFocus {
  dimensionCode: string
  label: string
  sourceType: string
  state: string
  confidence: number
  gapCount: number
  riskCount: number
  lastObservedAt: string | null
}

export interface TrainingAgentDashboard {
  enabled: boolean
  degraded: boolean
  state: string
  primary: TrainingRecommendationItem | null
  alternatives: TrainingRecommendationItem[]
  focus: AbilityFocus[]
  generatedAt: string
}

export function getTrainingAgentDashboard() {
  return http.get<ApiResponse<TrainingAgentDashboard>>('/training-agent/dashboard')
}
