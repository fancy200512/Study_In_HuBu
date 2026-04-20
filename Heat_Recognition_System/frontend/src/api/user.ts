import request from '@/utils/request'

export interface UserProfile {
  id?: number
  userId: number
  nickname?: string
  avatar?: string
  gender?: number
}

export interface UserBodyProfile {
  id?: number
  userId: number
  height?: number
  weight?: number
  bodyFat?: number
  age?: number
  bmi?: number
  bmr?: number
  targetCalories?: number
  updateTime?: string
}

export interface UserSnapshot {
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

const createEmptySnapshot = (userId: number): UserSnapshot => ({
  id: userId,
  userId,
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

export const getUserProfileAPI = (userId: number) =>
  request<UserProfile | null>({
    url: `/user/user/profile/${userId}`,
    method: 'get'
  })

export const updateUserProfileAPI = (data: UserProfile) =>
  request<void>({
    url: '/user/user/profile',
    method: 'put',
    data
  })

export const getUserBodyProfileAPI = (userId: number) =>
  request<UserBodyProfile | null>({
    url: `/user/user/body/${userId}`,
    method: 'get'
  })

export const updateUserBodyProfileAPI = (data: UserBodyProfile) =>
  request<void>({
    url: '/user/user/body',
    method: 'put',
    data
  })

export const getUserSnapshotAPI = async (userId: number) => {
  const [profileRes, bodyRes] = await Promise.all([
    getUserProfileAPI(userId),
    getUserBodyProfileAPI(userId)
  ])

  const profile = profileRes.data || null
  const body = bodyRes.data || null

  return {
    ...createEmptySnapshot(userId),
    nickname: profile?.nickname || '',
    avatar: profile?.avatar || '',
    gender: Number(profile?.gender ?? 0),
    age: Number(body?.age ?? 0),
    height: Number(body?.height ?? 0),
    weight: Number(body?.weight ?? 0),
    bodyFat: Number(body?.bodyFat ?? 0),
    bmi: Number(body?.bmi ?? 0),
    bmr: Number(body?.bmr ?? 0),
    targetCalories: Number(body?.targetCalories ?? 0)
  } as UserSnapshot
}
