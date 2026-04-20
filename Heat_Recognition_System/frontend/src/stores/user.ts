import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
  id: number
  userId: number
  nickname: string
  avatar: string
  gender: number
  age: number
  height: number
  weight: number
  bodyFat: number
  bmi: number
  bmr: number
  targetCalories: number
}

const createDefaultUserInfo = (): UserInfo => ({
  id: 0,
  userId: 0,
  nickname: '',
  avatar: '',
  gender: 0,
  age: 0,
  height: 0,
  weight: 0,
  bodyFat: 0,
  bmi: 0,
  bmr: 0,
  targetCalories: 0
})

const readStoredUserInfo = () => {
  const raw = localStorage.getItem('userInfo')
  if (!raw) return createDefaultUserInfo()

  try {
    return {
      ...createDefaultUserInfo(),
      ...JSON.parse(raw)
    } as UserInfo
  } catch {
    localStorage.removeItem('userInfo')
    return createDefaultUserInfo()
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(readStoredUserInfo())

  const persistUserInfo = (value: UserInfo) => {
    userInfo.value = value
    localStorage.setItem('userInfo', JSON.stringify(value))
  }

  const setLoginSession = (userId: number, nextToken: string) => {
    token.value = nextToken
    localStorage.setItem('token', nextToken)

    const nextUserInfo = {
      ...createDefaultUserInfo(),
      ...userInfo.value,
      id: userId,
      userId
    }
    persistUserInfo(nextUserInfo)
  }

  const setUserInfo = (payload: Partial<UserInfo>) => {
    const resolvedId = Number(payload.id ?? payload.userId ?? userInfo.value.id ?? 0)
    const nextUserInfo = {
      ...createDefaultUserInfo(),
      ...userInfo.value,
      ...payload,
      id: resolvedId,
      userId: Number(payload.userId ?? resolvedId)
    }
    persistUserInfo(nextUserInfo)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = createDefaultUserInfo()
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    setLoginSession,
    setUserInfo,
    logout
  }
})
