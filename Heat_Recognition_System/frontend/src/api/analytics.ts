import request from '@/utils/request'

export interface CaloriesTrendPoint {
  date: string
  calories: number
}

export interface NutritionRatio {
  proteinRatio: number
  fatRatio: number
  carbsRatio: number
}

export const getCaloriesTrendAPI = () =>
  request<CaloriesTrendPoint[]>({
    url: '/analytics/calories',
    method: 'get'
  })

export const getNutritionRatioAPI = () =>
  request<NutritionRatio>({
    url: '/analytics/nutrition',
    method: 'get'
  })

export const getAdviceAPI = () =>
  request<string[]>({
    url: '/analytics/advice',
    method: 'get'
  })
