import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

export class ApiBusinessError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiBusinessError'
    this.code = code
  }
}

export function isApiBusinessError(error: unknown): error is ApiBusinessError {
  return error instanceof ApiBusinessError
}

const http = axios.create({
  baseURL: '/api',
  timeout: 70000,
  headers: { 'Content-Type': 'application/json' },
})

let accessToken: string | null = null
let refreshAccessToken: (() => Promise<string | null>) | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function setRefreshHandler(handler: () => Promise<string | null>) {
  refreshAccessToken = handler
}

http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

function readableMessage(code?: number, fallback?: string) {
  if (code === 40110) return '学校 API Key 无效、过期或无权限，请检查后端环境变量'
  if (code === 42910) return '学校 API 调用过于频繁或额度受限，请稍后再试'
  if (code === 50410) return '学校 API 响应超时，请稍后重试'
  if (code === 50210 || code === 50211) return fallback || '学校 API 暂时不可用，请稍后重试'
  if (code === 50010) return fallback || '后端模型配置不完整'
  return fallback || '请求失败'
}

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const data = response.data
    if (data && typeof data === 'object' && data.code !== 200) {
      const message = readableMessage(data.code, data.msg)
      ElMessage.error(message)
      return Promise.reject(new ApiBusinessError(data.code, message))
    }
    return response
  },
  async (error: AxiosError<ApiResponse>) => {
    const request = error.config as (typeof error.config & { _authRetried?: boolean })
    const isAuthEndpoint = request?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && request && !request._authRetried && !isAuthEndpoint && refreshAccessToken) {
      request._authRetried = true
      const token = await refreshAccessToken()
      if (token) {
        request.headers.Authorization = `Bearer ${token}`
        return http.request(request)
      }
    }
    const businessCode = error.response?.data?.code
    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试'
      : readableMessage(businessCode, error.response?.data?.msg || '网络异常，请稍后重试')
    ElMessage.error(message)
    if (typeof businessCode === 'number' && businessCode !== 200) {
      return Promise.reject(new ApiBusinessError(businessCode, message))
    }
    return Promise.reject(error)
  },
)

export default http
