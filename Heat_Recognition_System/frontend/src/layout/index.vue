<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CaretBottom,
  Document,
  Expand,
  Fold,
  Food,
  House,
  PieChart,
  Reading,
  Sunny,
  User
} from '@element-plus/icons-vue'

import { useCurrentUser } from '@/composables/useCurrentUser'
import { getTodayTagText } from '@/utils/date'

const router = useRouter()
const route = useRoute()
const { currentUserId, ensureUserLoaded, userStore } = useCurrentUser()

const isCollapse = ref(false)
const isNarrow = ref(typeof window !== 'undefined' ? window.innerWidth <= 1280 : false)
const currentTipIndex = ref(0)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

let tipTimer: number | null = null

const tipsList = [
  '识别完成后可以直接修改餐别，方便把早餐、午餐和加餐归类到当天记录里。',
  '如果您了解所拍食物的具体信息，可以在弹窗中补充说明，帮助系统进一步细化识别结果。',
  '个人中心里补全年龄、身高、体重和目标热量后，首页的热量进度会更直观。',
  '饮食报告页会优先展示近 7 天记录，连续记录越多，趋势越有参考价值。'
]

const menuCollapsed = computed(() => isCollapse.value || isNarrow.value)

const breadcrumbName = computed(() => {
  const map: Record<string, string> = {
    Dashboard: '首页概览',
    Diet: '识别记录',
    Report: '饮食报告',
    Science: '饮食科普',
    User: '个人中心'
  }

  return map[String(route.name || '')] || '饮食健康'
})

const displayName = computed(() => {
  const nickname = userStore.userInfo.nickname?.trim()
  if (nickname) return nickname
  if (currentUserId.value) return `用户 ${currentUserId.value}`
  return '智膳热鉴'
})

const updateNarrow = () => {
  if (typeof window === 'undefined') return
  isNarrow.value = window.innerWidth <= 1280
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  updateNarrow()
  window.addEventListener('resize', updateNarrow)

  if (currentUserId.value) {
    void ensureUserLoaded()
  }

  tipTimer = window.setInterval(() => {
    currentTipIndex.value = (currentTipIndex.value + 1) % tipsList.length
  }, 5000)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateNarrow)
  if (tipTimer) clearInterval(tipTimer)
})
</script>

<template>
  <div class="app-layout">
    <aside class="side-panel glass-effect" :class="{ 'is-collapsed': menuCollapsed }">
      <div class="side-top">
        <div class="side-logo" @click="router.push('/dashboard')">
          <div class="logo-inner">ZS</div>
          <span v-show="!menuCollapsed" class="logo-text">智膳热鉴</span>
        </div>

        <el-menu
          :default-active="route.path"
          :collapse="menuCollapsed"
          :collapse-transition="false"
          router
          class="menu-list"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <template #title>首页概览</template>
          </el-menu-item>
          <el-menu-item index="/diet">
            <el-icon><Food /></el-icon>
            <template #title>识别记录</template>
          </el-menu-item>
          <el-menu-item index="/report">
            <el-icon><PieChart /></el-icon>
            <template #title>饮食报告</template>
          </el-menu-item>
          <el-menu-item index="/science">
            <el-icon><Reading /></el-icon>
            <template #title>饮食科普</template>
          </el-menu-item>
          <el-menu-item index="/user">
            <el-icon><User /></el-icon>
            <template #title>个人中心</template>
          </el-menu-item>
        </el-menu>
      </div>

      <div v-show="!menuCollapsed" class="side-widget-area">
        <div class="advice-bubble">
          <div class="label">
            <el-icon><Sunny /></el-icon>
            使用提醒
          </div>
          <transition name="tip-fade" mode="out-in">
            <div :key="currentTipIndex" class="tip-content">{{ tipsList[currentTipIndex] }}</div>
          </transition>
        </div>

        <div class="illustration-container">
          <img src="@/assets/side-health.png" class="big-side-img" alt="健康插图" />
        </div>
      </div>
    </aside>

    <main class="main-panel">
      <header class="top-nav glass-effect">
        <div class="nav-left">
          <div class="toggle-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="20"><component :is="menuCollapsed ? Expand : Fold" /></el-icon>
          </div>
          <div class="today-tag">今天 · {{ getTodayTagText() }}</div>
          <div class="breadcrumb">{{ breadcrumbName }}</div>
        </div>

        <div class="nav-right">
          <el-button class="nav-action" round @click="router.push('/diet')">
            <el-icon><Food /></el-icon>
            去识别
          </el-button>
          <el-button class="nav-action light" round @click="router.push('/report')">
            <el-icon><Document /></el-icon>
            查看报告
          </el-button>
          <el-button class="nav-action light" round @click="router.push('/science')">
            <el-icon><Reading /></el-icon>
            饮食科普
          </el-button>
          <el-dropdown trigger="click">
            <div class="user-capsule">
              <el-avatar :size="34" :src="userStore.userInfo.avatar || defaultAvatar" />
              <span class="user-name">{{ displayName }}</span>
              <el-icon><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content-body">
        <router-view />
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
.app-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.side-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.28s ease;
  background: linear-gradient(180deg, rgba(255, 172, 122, 0.82), rgba(255, 122, 24, 0.96)) !important;
  animation: fade-up 0.45s ease;

  &.is-collapsed {
    width: 78px;

    .side-logo {
      justify-content: center;
      padding: 0;
    }
  }
}

.side-top {
  flex: none;
}

.side-logo {
  height: 92px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 25px;
  cursor: pointer;

  .logo-inner {
    width: 34px;
    height: 34px;
    flex-shrink: 0;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 800;
    color: #fff;
    background: rgba(255, 255, 255, 0.34);
  }

  .logo-text {
    font-weight: 800;
    font-size: 20px;
    color: #fff;
    white-space: nowrap;
  }
}

.menu-list {
  width: 100%;
  padding: 0 6px 8px;
  background: transparent !important;
  border: none !important;

  :deep(.el-menu) {
    background: transparent !important;
    border-right: none !important;
  }

  :deep(.el-menu-item) {
    margin: 6px 10px;
    height: 46px;
    line-height: 46px;
    border-radius: 16px;
    background: transparent !important;
  }

  :deep(.el-menu-item:hover) {
    background: rgba(255, 255, 255, 0.26) !important;
  }

  :deep(.el-menu-item),
  :deep(.el-menu-item .el-icon) {
    color: #fff !important;
  }

  :deep(.el-menu-item.is-active) {
    color: #ff7a18 !important;
    background: rgba(255, 255, 255, 0.92) !important;
  }
}

.side-widget-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 20px;
}

.advice-bubble {
  padding: 15px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.24);

  .label {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    font-size: 12px;
    font-weight: 700;
    color: #fff;
  }

  .tip-content {
    font-size: 13px;
    line-height: 1.6;
    color: rgba(255, 255, 255, 0.92);
  }
}

.illustration-container {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.big-side-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 15px 15px rgba(0, 0, 0, 0.05));
}

.main-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px;
}

.top-nav {
  min-height: 74px;
  padding: 0 22px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.nav-left,
.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.toggle-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 122, 24, 0.08);
  color: #9a3412;
  cursor: pointer;
}

.today-tag {
  padding: 9px 14px;
  border-radius: 999px;
  background: #fff0e1;
  color: #9a3412;
  font-size: 13px;
  font-weight: 700;
}

.breadcrumb {
  font-size: 22px;
  font-weight: 800;
  color: #7c2d12;
}

.nav-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.nav-action.light {
  background: rgba(255, 255, 255, 0.8) !important;
  color: #9a3412 !important;
  box-shadow: none !important;
}

.user-capsule {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: #7c2d12;
}

.content-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.tip-fade-enter-active,
.tip-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.tip-fade-enter-from,
.tip-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 960px) {
  .app-layout {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }

  .side-panel,
  .side-panel.is-collapsed {
    width: auto;
  }

  .side-widget-area {
    display: none;
  }

  .main-panel {
    padding: 12px;
  }

  .top-nav {
    min-height: auto;
    padding: 14px;
    flex-direction: column;
    align-items: stretch;
  }

  .nav-left,
  .nav-right {
    flex-wrap: wrap;
  }

  .breadcrumb {
    font-size: 18px;
  }
}
</style>
