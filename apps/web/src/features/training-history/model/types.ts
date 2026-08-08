export type TrainingType = 'KNOWLEDGE' | 'PROJECT_DEEP_DIVE' | 'ALGORITHM' | 'COMPREHENSIVE_SIMULATION'

export interface TrainingHistoryItem {
  id: number
  trainingType: TrainingType
  sourceSessionId: number
  status: string
  title: string
  summary: string | null
  startedAt: string
  finishedAt: string | null
}

export interface TrainingHistoryPage {
  items: TrainingHistoryItem[]
  total: number
  page: number
  pageSize: number
}
