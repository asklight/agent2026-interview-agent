import http from '@/api/http'
import type { ApiResponse } from '@/api/http'
import type { TrainingHistoryPage } from '../model/types'

export function getTrainingHistory(params: { type?: string; status?: string; page?: number; pageSize?: number }) {
  return http.get<ApiResponse<TrainingHistoryPage>>('/training-history', { params })
}

export function hideTrainingHistory(id: number) {
  return http.delete<ApiResponse<null>>(`/training-history/${id}`)
}
