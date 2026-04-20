export const toNumber = (value: unknown) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

export const roundTo = (value: unknown, digits = 1) => {
  const num = toNumber(value)
  const factor = 10 ** digits
  return Math.round(num * factor) / factor
}

export const formatCalories = (value: unknown) => {
  const num = toNumber(value)
  const rounded = roundTo(num, 1)
  return Number.isInteger(rounded) ? `${Math.round(rounded)} kcal` : `${rounded} kcal`
}

export const calculateBmi = (height: unknown, weight: unknown) => {
  const heightCm = toNumber(height)
  const weightKg = toNumber(weight)
  if (!heightCm || !weightKg) return 0

  const heightMeter = heightCm / 100
  return roundTo(weightKg / (heightMeter * heightMeter))
}

export const calculateBmr = (gender: unknown, age: unknown, height: unknown, weight: unknown) => {
  const genderValue = toNumber(gender)
  const ageValue = toNumber(age)
  const heightValue = toNumber(height)
  const weightValue = toNumber(weight)

  if (!ageValue || !heightValue || !weightValue) return 0

  const genderOffset = genderValue === 1 ? 5 : genderValue === 2 ? -161 : -78
  return Math.round(10 * weightValue + 6.25 * heightValue - 5 * ageValue + genderOffset)
}

export const getBmiStatus = (bmi: unknown) => {
  const value = toNumber(bmi)
  if (!value) return '待完善'
  if (value < 18.5) return '偏瘦'
  if (value < 24) return '正常'
  if (value < 28) return '偏高'
  return '超重'
}
