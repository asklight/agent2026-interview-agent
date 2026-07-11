import http from '@/api/http'
import type { ApiResponse } from '@/api/http'
import type {
  CreateProjectInterviewPayload,
  ProjectInterviewReport,
  ProjectInterviewSession,
  SubmitProjectTurnPayload,
} from '@/features/project-deep-dive/model/types'

const resourceHeaders = (accessToken: string) => ({
  headers: { 'X-Resource-Token': accessToken },
})

export function createProjectInterview(payload: CreateProjectInterviewPayload, accessToken: string) {
  return http.post<ApiResponse<ProjectInterviewSession>>('/interview-sessions', payload, resourceHeaders(accessToken))
}

export function getProjectInterviewTurns(sessionId: number, accessToken: string) {
  return http.get<ApiResponse<ProjectInterviewSession>>(`/interview-sessions/${sessionId}/turns`, resourceHeaders(accessToken))
}

export function submitProjectInterviewTurn(
  sessionId: number,
  accessToken: string,
  payload: SubmitProjectTurnPayload,
) {
  return http.post<ApiResponse<ProjectInterviewSession>>(
    `/interview-sessions/${sessionId}/turns`,
    payload,
    resourceHeaders(accessToken),
  )
}

export function finishProjectInterview(sessionId: number, accessToken: string) {
  return http.post<ApiResponse<ProjectInterviewSession>>(
    `/interview-sessions/${sessionId}/finish`,
    undefined,
    resourceHeaders(accessToken),
  )
}

export function getProjectInterviewReport(sessionId: number, accessToken: string) {
  return http.get<ApiResponse<ProjectInterviewReport>>(
    `/interview-sessions/${sessionId}/report`,
    resourceHeaders(accessToken),
  )
}
