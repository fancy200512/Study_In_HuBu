import request from '@/utils/request'
import type { MealType } from '@/utils/meal'

export type NeedMoreInfoType = 'WEIGHT' | 'COUNT' | 'COOKING_METHOD' | 'NONE'
export interface RecognitionFollowUpItem {
  foodName: string
  infoType: NeedMoreInfoType | string
  options: string[]
}

export interface ImageNutrition {
  imageId: number
  imageUrl: string
  totalCalories: number
  protein: number
  fat: number
  carbs: number
  needMoreInfo?: NeedMoreInfoType | string | null
  followUpItems?: RecognitionFollowUpItem[] | null
}

export interface RecognitionResponse {
  sessionId: number
  totalCalories: number
  protein: number
  fat: number
  carbs: number
  images: ImageNutrition[]
}

export interface RefinePayload {
  sessionId?: number
  imageId: number
  extraInfo: string
}

export const recognizeAPI = (payload: { mealType?: MealType; prompt?: string; files: File[] }) => {
  const formData = new FormData()
  if (payload.mealType) formData.append('mealType', payload.mealType)
  if (payload.prompt?.trim()) formData.append('prompt', payload.prompt.trim())
  payload.files.forEach((file) => formData.append('files', file))

  return request<RecognitionResponse>({
    url: '/recognition/recognize',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 0
  })
}

export const refineRecognitionAPI = (data: RefinePayload) =>
  request<ImageNutrition>({
    url: '/recognition/refine',
    method: 'post',
    data,
    timeout: 0
  })
