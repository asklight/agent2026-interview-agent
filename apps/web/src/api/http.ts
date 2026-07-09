import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

const http = axios.create({
  baseURL: '/api',
  timeout: 70000,
  headers: { 'Content-Type': 'application/json' },
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
      return Promise.reject(new Error(message))
    }
    return response
  },
  (error: AxiosError<ApiResponse>) => {
    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试'
      : readableMessage(error.response?.data?.code, error.response?.data?.msg || '网络异常，请稍后重试')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
