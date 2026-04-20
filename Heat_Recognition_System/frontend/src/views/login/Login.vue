<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Promotion, User } from '@element-plus/icons-vue'

import { loginAPI } from '@/api/auth'
import { getUserSnapshotAPI } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginMode = ref<'PASSWORD' | 'CODE'>('PASSWORD')
const loading = ref(false)
const form = reactive({
  account: '',
  password: '',
  code: '123456'
})

const submitLabel = computed(() => (loginMode.value === 'PASSWORD' ? '密码登录' : '验证码登录'))
const helperText = computed(() =>
  loginMode.value === 'PASSWORD'
    ? '请输入邮箱或手机号，以及你在数据库里已有的密码。'
    : '当前后端验证码登录是最小可运行版，默认验证码固定为 123456。'
)

const handleLogin = async () => {
  const account = form.account.trim()
  if (!account) {
    ElMessage.warning('请输入邮箱或手机号')
    return
  }

  if (loginMode.value === 'PASSWORD' && !form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  if (loginMode.value === 'CODE' && !form.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const res = await loginAPI({
      loginType: loginMode.value,
      account,
      password: loginMode.value === 'PASSWORD' ? form.password : undefined,
      code: loginMode.value === 'CODE' ? form.code.trim() : undefined
    })

    userStore.setLoginSession(res.data.id, res.data.token)

    try {
      const snapshot = await getUserSnapshotAPI(res.data.id)
      userStore.setUserInfo(snapshot)
    } catch {
      userStore.setUserInfo({ id: res.data.id, userId: res.data.id })
    }

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.token) {
    router.replace('/dashboard')
  }
})
</script>

<template>
  <div class="login-page">
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="glass-login-card">
      <div class="brand-side">
        <div class="logo-area">
          <div class="logo-icon">ZS</div>
          <h1>智膳热鉴</h1>
        </div>
        <div class="ai-pill">智膳热鉴</div>
        <p class="slogan">
          把饮食识别、营养汇总和个人档案放到一个前端里，
          按照后端现在已经实现的接口直接连通。
        </p>
        <img src="@/assets/side-health.png" class="brand-img" alt="品牌插图" />
      </div>

      <div class="form-side">
        <div class="mode-switch">
          <button
            type="button"
            class="mode-pill"
            :class="{ active: loginMode === 'PASSWORD' }"
            @click="loginMode = 'PASSWORD'"
          >
            密码登录
          </button>
          <button
            type="button"
            class="mode-pill"
            :class="{ active: loginMode === 'CODE' }"
            @click="loginMode = 'CODE'"
          >
            验证码登录
          </button>
        </div>

        <h2>{{ submitLabel }}</h2>
        <p class="helper-text">{{ helperText }}</p>

        <el-form label-position="top" @submit.prevent>
          <el-form-item label="账号">
            <el-input
              v-model="form.account"
              placeholder="邮箱或手机号"
              :prefix-icon="loginMode === 'PASSWORD' ? User : Message"
              class="capsule-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item v-if="loginMode === 'PASSWORD'" label="密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              class="capsule-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item v-else label="验证码">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              :prefix-icon="Promotion"
              class="capsule-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            {{ submitLabel }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  z-index: 1;
  opacity: 0.6;
}

.blob-1 {
  width: 400px;
  height: 400px;
  background: #ffd6b3;
  top: -100px;
  left: -100px;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: #ffb676;
  bottom: -50px;
  right: -50px;
}

.glass-login-card {
  width: 960px;
  min-height: 580px;
  display: flex;
  z-index: 10;
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: fade-up 0.45s ease;
}

.brand-side {
  width: 42%;
  background: linear-gradient(165deg, rgba(255, 218, 183, 0.92), rgba(255, 180, 120, 0.66));
  padding: 52px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.logo-icon {
  width: 42px;
  height: 42px;
  background: #ff7a18;
  border-radius: 12px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.logo-area h1 {
  margin: 0;
  font-size: 28px;
  color: #ea580c;
}

.ai-pill {
  display: inline-flex;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 14px;
}

.slogan {
  margin: 0;
  font-size: 18px;
  color: #4a5568;
  line-height: 1.8;
  font-weight: 500;
}

.brand-img {
  width: 118%;
  margin-left: -9%;
  margin-top: 24px;
  filter: drop-shadow(0 20px 22px rgba(0, 0, 0, 0.09));
  animation: soft-float 4s ease-in-out infinite;
}

.form-side {
  width: 58%;
  padding: 56px 52px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.mode-switch {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  width: fit-content;
}

.mode-pill {
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.mode-pill.active {
  background: linear-gradient(120deg, #ff9838, #ff7a18);
  color: #fff;
  box-shadow: 0 10px 22px rgba(255, 122, 24, 0.2);
}

.form-side h2 {
  margin: 26px 0 10px;
  font-size: 30px;
  color: #1e293b;
}

.helper-text {
  margin: 0 0 24px;
  color: #64748b;
  line-height: 1.7;
}

.submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 18px;
  border-radius: 18px !important;
  font-size: 16px;
  font-weight: 700;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.64) !important;
  border-radius: 18px !important;
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.82) !important;
  height: 50px;
}

@media (max-width: 960px) {
  .glass-login-card {
    width: calc(100vw - 24px);
    min-height: auto;
    flex-direction: column;
  }

  .brand-side,
  .form-side {
    width: 100%;
    padding: 24px;
  }

  .brand-side {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  }

  .brand-img {
    width: 100%;
    margin: 16px 0 0;
    max-height: 220px;
    object-fit: contain;
  }
}
</style>
