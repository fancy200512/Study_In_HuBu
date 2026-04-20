import request from '@/utils/request'

export interface LoginPayload {
  loginType: 'PASSWORD' | 'CODE'
  account: string
  password?: string
  code?: string
}

export interface LoginResult {
  id: number
  token: string
}

export const loginAPI = (data: LoginPayload) =>
  request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data
  })

export const logoutAPI = () =>
  request<void>({
    url: '/auth/logout',
    method: 'post'
  })
