import { createRouter, createWebHistory } from 'vue-router'

import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/Login.vue')
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue')
        },
        {
          path: 'diet',
          name: 'Diet',
          component: () => import('@/views/diet/index.vue')
        },
        {
          path: 'report',
          name: 'Report',
          component: () => import('@/views/report/index.vue')
        },
        {
          path: 'science',
          name: 'Science',
          component: () => import('@/views/science/index.vue')
        },
        {
          path: 'user',
          name: 'User',
          component: () => import('@/views/user/index.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const hasToken = Boolean(userStore.token)

  if (to.path === '/login' && hasToken) {
    next('/dashboard')
    return
  }

  if (to.path !== '/login' && !hasToken) {
    next('/login')
    return
  }

  next()
})

export default router
