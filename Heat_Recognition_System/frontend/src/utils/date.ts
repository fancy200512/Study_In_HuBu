const pad2 = (value: number) => String(value).padStart(2, '0')

export const getLocalDateString = (date: Date = new Date()) => {
  const year = date.getFullYear()
  const month = pad2(date.getMonth() + 1)
  const day = pad2(date.getDate())
  return `${year}-${month}-${day}`
}

export const getLocalDateOffsetString = (offsetDays: number) => {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return getLocalDateString(date)
}

export const getTodayTagText = (date: Date = new Date()) => `${date.getMonth() + 1}月${date.getDate()}日`

export const formatDateTime = (value?: string | null) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return `${getLocalDateString(date)} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}
