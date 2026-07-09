import http from '@/api/http'
import type { ApiResponse } from '@/api/http'

export function getHealth() {
  return http.get<ApiResponse<string>>('/health')
}
