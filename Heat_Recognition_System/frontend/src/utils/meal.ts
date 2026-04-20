export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'CUSTOM'

export const MEAL_TYPE_OPTIONS: Array<{ label: string; value: MealType }> = [
  { label: '早餐', value: 'BREAKFAST' },
  { label: '午餐', value: 'LUNCH' },
  { label: '晚餐', value: 'DINNER' },
  { label: '加餐', value: 'SNACK' },
  { label: '自定义', value: 'CUSTOM' }
]

const mealTypeMap: Record<string, string> = Object.fromEntries(
  MEAL_TYPE_OPTIONS.map((item) => [item.value, item.label])
)

export const getMealTypeLabel = (value?: string | null) => {
  if (!value) return '未设置'
  return mealTypeMap[value] || value
}
