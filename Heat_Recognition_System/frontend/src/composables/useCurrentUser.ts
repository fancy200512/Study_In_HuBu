import { computed } from 'vue'

import { getUserSnapshotAPI } from '@/api/user'
import { useUserStore } from '@/stores/user'

export const useCurrentUser = () => {
  const userStore = useUserStore()

  const currentUserId = computed(() => Number(userStore.userInfo.id || userStore.userInfo.userId || 0))

  const ensureUserLoaded = async () => {
    const userId = currentUserId.value
    if (!userId) return null

    const snapshot = await getUserSnapshotAPI(userId)
    userStore.setUserInfo(snapshot)
    return snapshot
  }

  return {
    userStore,
    currentUserId,
    ensureUserLoaded
  }
}
