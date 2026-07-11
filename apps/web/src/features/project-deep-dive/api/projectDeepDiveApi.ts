import http from '@/api/http'
import type { ApiResponse } from '@/api/http'
import type {
  CreateProjectProfileResult,
  PatchProjectProfilePayload,
  ProjectProfile,
} from '@/features/project-deep-dive/model/types'

const resourceHeaders = (accessToken: string) => ({
  headers: { 'X-Resource-Token': accessToken },
})

export function createProjectProfile(description: string) {
  return http.post<ApiResponse<CreateProjectProfileResult>>('/project-profiles', { description })
}

export function getProjectProfile(profileId: number, accessToken: string) {
  return http.get<ApiResponse<ProjectProfile>>(`/project-profiles/${profileId}`, resourceHeaders(accessToken))
}

export function analyzeProjectProfile(profileId: number, accessToken: string) {
  return http.post<ApiResponse<ProjectProfile>>(`/project-profiles/${profileId}/analyze`, undefined, resourceHeaders(accessToken))
}

export function patchProjectProfile(profileId: number, accessToken: string, payload: PatchProjectProfilePayload) {
  return http.patch<ApiResponse<ProjectProfile>>(`/project-profiles/${profileId}`, payload, resourceHeaders(accessToken))
}

export function confirmProjectProfile(profileId: number, accessToken: string) {
  return http.post<ApiResponse<ProjectProfile>>(`/project-profiles/${profileId}/confirm`, undefined, resourceHeaders(accessToken))
}
