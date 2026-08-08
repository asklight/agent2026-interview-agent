import axios from 'axios'
import http, { type ApiResponse } from '@/api/http'
import type { AuthPayload } from '../model/types'

export function register(username: string, password: string) {
  return http.post<ApiResponse<AuthPayload>>('/auth/register', { username, password }, { withCredentials: true })
}

export function login(username: string, password: string) {
  return http.post<ApiResponse<AuthPayload>>('/auth/login', { username, password }, { withCredentials: true })
}

export function logout() {
  return http.post<ApiResponse<void>>('/auth/logout', {}, { withCredentials: true })
}

export function refreshSession() {
  return axios.post<ApiResponse<AuthPayload>>('/api/auth/refresh', {}, {
    withCredentials: true,
    headers: { 'Content-Type': 'application/json' },
    timeout: 15000,
  })
}
