import request from '@/utils/request'

export interface DietLog {
  id?: number
  userId?: number
  date: string
  mealType?: string
  totalCalories: number
  protein: number
  fat: number
  carbs: number
  createTime?: string
}

export const getDietLogAPI = (date: string) =>
  request<DietLog | null>({
    url: '/diet-log',
    method: 'get',
    params: { date }
  })

export const getDietHistoryAPI = () =>
  request<DietLog[]>({
    url: '/diet-log/history',
    method: 'get'
  })
