import request from '@/utils/request'

export interface SessionRecord {
  id: number
  userId?: number
  mealType: string
  customMealName?: string
  totalCalories: number
  protein: number
  fat: number
  carbs: number
  status: string
  createTime: string
}

export interface SessionListResult {
  total: number
  records: SessionRecord[]
}

export interface SessionListParams {
  page: number
  pageSize: number
  date?: string
}

export const getSessionListAPI = (params: SessionListParams) => {
  const query: Record<string, string | number> = {
    page: params.page,
    pageSize: params.pageSize
  }

  if (params.date) {
    query.beginDate = params.date
    query.endDate = params.date
  }

  return request<SessionListResult>({
    url: '/session/list',
    method: 'get',
    params: query
  })
}

export const getSessionDetailAPI = (id: number) =>
  request<SessionRecord | null>({
    url: `/session/${id}`,
    method: 'get'
  })

export const deleteSessionAPI = (id: number) =>
  request<void>({
    url: `/session/${id}`,
    method: 'delete'
  })

export const updateSessionMealTypeAPI = (id: number, mealType: string) =>
  request<void>({
    url: `/session/${id}/mealType`,
    method: 'put',
    data: { mealType }
  })
