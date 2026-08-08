import http, { type ApiResponse } from '@/api/http'
import type { CreateSimulationPayload, SimulationOptions, SimulationReport, SimulationSession, SubmitSimulationPayload } from '../model/types'

export function getSimulationOptions() {
  return http.get<ApiResponse<SimulationOptions>>('/simulations/options')
}

export function createSimulation(payload: CreateSimulationPayload) {
  return http.post<ApiResponse<SimulationSession>>('/simulations', payload)
}

export function getSimulation(sessionId: number) {
  return http.get<ApiResponse<SimulationSession>>(`/simulations/${sessionId}`)
}

export function submitSimulationAnswer(sessionId: number, payload: SubmitSimulationPayload) {
  return http.post<ApiResponse<SimulationSession>>(`/simulations/${sessionId}/answers`, payload)
}

export function retrySimulationAnswer(sessionId: number) {
  return http.post<ApiResponse<SimulationSession>>(`/simulations/${sessionId}/answers/retry-pending`)
}

export function advanceSimulation(sessionId: number, expectedVersion: number) {
  return http.post<ApiResponse<SimulationSession>>(`/simulations/${sessionId}/advance`, { expectedVersion })
}

export function finishSimulation(sessionId: number, expectedVersion: number) {
  return http.post<ApiResponse<SimulationSession>>(`/simulations/${sessionId}/finish`, { expectedVersion })
}

export function getSimulationReport(sessionId: number) {
  return http.get<ApiResponse<SimulationReport>>(`/simulations/${sessionId}/report`)
}
