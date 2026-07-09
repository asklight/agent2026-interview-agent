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
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const data = response.data
    if (data && typeof data === 'object' && data.code !== 200) {
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResponse>) => {
    const message = error.response?.data?.msg || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
