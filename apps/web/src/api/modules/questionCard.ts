import http from '@/api/http'
import type { ApiResponse } from '@/api/http'

export function getQuestionModules() {
  return http.get<ApiResponse<string[]>>('/question-cards/modules')
}
