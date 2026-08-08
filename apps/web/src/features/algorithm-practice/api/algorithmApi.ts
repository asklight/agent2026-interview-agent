import http from '@/api/http'
import type { ApiResponse } from '@/api/http'
import type { AlgorithmProblem, AlgorithmReport, AlgorithmSession, AlgorithmInputModality } from '../model/types'

export function getAlgorithmProblems(filters?: { difficulty?: string; tag?: string }) {
  return http.get<ApiResponse<AlgorithmProblem[]>>('/algorithm-problems', { params: filters })
}

export function createAlgorithmSession(problemId: number) {
  return http.post<ApiResponse<AlgorithmSession>>('/algorithm-sessions', { problemId })
}

export function getAlgorithmSession(sessionId: number) {
  return http.get<ApiResponse<AlgorithmSession>>(`/algorithm-sessions/${sessionId}`)
}

export function submitAlgorithmTurn(sessionId: number, payload: {
  clientTurnId: string
  questionTurnId: number
  expectedVersion: number
  content: string
  inputModality: AlgorithmInputModality
}) {
  return http.post<ApiResponse<AlgorithmSession>>(`/algorithm-sessions/${sessionId}/turns`, payload)
}

export function retryAlgorithmTurn(sessionId: number) {
  return http.post<ApiResponse<AlgorithmSession>>(`/algorithm-sessions/${sessionId}/turns/retry-pending`)
}

export function finishAlgorithmSession(sessionId: number) {
  return http.post<ApiResponse<AlgorithmSession>>(`/algorithm-sessions/${sessionId}/finish`)
}

export function getAlgorithmReport(sessionId: number) {
  return http.get<ApiResponse<AlgorithmReport>>(`/algorithm-sessions/${sessionId}/report`)
}
