import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
}) as any

service.interceptors.request.use((config: any) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.token = userStore.token
  }
  return config
})

let errorToastLock = false

const showOneError = (message: string) => {
  if (errorToastLock) return
  errorToastLock = true
  ElMessage.closeAll()
  ElMessage.error(message)

  window.setTimeout(() => {
    errorToastLock = false
  }, 500)
}

service.interceptors.response.use(
  (response: { data: ApiResponse }) => {
    const payload = response.data as ApiResponse

    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 1) {
        return payload
      }

      const message = payload.msg || '请求失败'
      showOneError(message)
      return Promise.reject(payload)
    }

    return payload
  },
  (error: any) => {
    const userStore = useUserStore()
    const status = error?.response?.status
    const message =
      error?.response?.data?.msg ||
      error?.response?.data?.message ||
      error?.message ||
      '请求失败'

    if (status === 401) {
      userStore.logout()
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    } else if (message !== 'canceled') {
      showOneError(message)
    }

    return Promise.reject(error)
  }
)

const request = <T = unknown>(config: AxiosRequestConfig) => {
  return service.request(config) as Promise<ApiResponse<T>>
}

export default request
