<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Monitor, Opportunity, User } from '@element-plus/icons-vue'

import { updateUserBodyProfileAPI, updateUserProfileAPI } from '@/api/user'
import { useCurrentUser } from '@/composables/useCurrentUser'
import { calculateBmi, calculateBmr, getBmiStatus, toNumber } from '@/utils/health'

const { currentUserId, ensureUserLoaded, userStore } = useCurrentUser()

const saving = ref(false)
const form = reactive({
  nickname: '',
  avatar: '',
  gender: 0,
  age: 0,
  height: 0,
  weight: 0,
  bodyFat: 0,
  targetCalories: 0
})

const profileCompletion = computed(() => {
  let score = 0
  if (form.nickname.trim()) score += 15
  if (form.avatar.trim()) score += 10
  if (form.age > 0) score += 15
  if (form.height > 0) score += 15
  if (form.weight > 0) score += 15
  if (form.bodyFat > 0) score += 10
  if (form.targetCalories > 0) score += 20
  return Math.min(100, score)
})

const bmi = computed(() => calculateBmi(form.height, form.weight))
const bmr = computed(() => calculateBmr(form.gender, form.age, form.height, form.weight))
const bmiStatus = computed(() => getBmiStatus(bmi.value))

const previewAvatar = computed(
  () => form.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
)

const assignForm = () => {
  form.nickname = userStore.userInfo.nickname || ''
  form.avatar = userStore.userInfo.avatar || ''
  form.gender = Number(userStore.userInfo.gender || 0)
  form.age = Number(userStore.userInfo.age || 0)
  form.height = Number(userStore.userInfo.height || 0)
  form.weight = Number(userStore.userInfo.weight || 0)
  form.bodyFat = Number(userStore.userInfo.bodyFat || 0)
  form.targetCalories = Number(userStore.userInfo.targetCalories || 0)
}

const saveAll = async () => {
  const userId = currentUserId.value
  if (!userId) {
    ElMessage.warning('当前没有可用的用户 ID')
    return
  }

  saving.value = true
  try {
    const nextBmi = bmi.value
    const nextBmr = bmr.value

    await Promise.all([
      updateUserProfileAPI({
        userId,
        nickname: form.nickname.trim(),
        avatar: form.avatar.trim(),
        gender: form.gender
      }),
      updateUserBodyProfileAPI({
        userId,
        age: toNumber(form.age),
        height: toNumber(form.height),
        weight: toNumber(form.weight),
        bodyFat: toNumber(form.bodyFat),
        bmi: nextBmi,
        bmr: nextBmr,
        targetCalories: toNumber(form.targetCalories)
      })
    ])

    userStore.setUserInfo({
      id: userId,
      userId,
      nickname: form.nickname.trim(),
      avatar: form.avatar.trim(),
      gender: form.gender,
      age: toNumber(form.age),
      height: toNumber(form.height),
      weight: toNumber(form.weight),
      bodyFat: toNumber(form.bodyFat),
      bmi: nextBmi,
      bmr: nextBmr,
      targetCalories: toNumber(form.targetCalories)
    })

    ElMessage.success('个人档案已更新')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await ensureUserLoaded()
  assignForm()
})
</script>

<template>
  <div class="user-page">
    <section class="hero-card glass-effect">
      <div class="hero-main">
        <div class="avatar-wrap">
          <el-avatar :size="108" :src="previewAvatar" />
        </div>

        <div class="hero-copy">
          <span class="eyebrow">个人中心</span>
          <h2>{{ form.nickname || `用户 ${currentUserId || ''}` }}</h2>
          <div class="tag-row">
            <span class="tag warm">完成度 {{ profileCompletion }}%</span>
            <span class="tag">BMI {{ bmi ? bmi.toFixed(1) : '--' }}</span>
            <span class="tag">{{ bmiStatus }}</span>
          </div>
        </div>
      </div>

      <div class="hero-stats">
        <div class="stat-box">
          <span>目标热量</span>
          <strong>{{ form.targetCalories || '--' }}</strong>
          <small>kcal</small>
        </div>
        <div class="stat-box">
          <span>BMR</span>
          <strong>{{ bmr || '--' }}</strong>
          <small>kcal</small>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <el-card class="glass-effect form-card">
        <template #header>
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>基础信息</span>
          </div>
        </template>

        <div class="form-grid">
          <div class="form-item">
            <label>昵称</label>
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </div>

          <div class="form-item">
            <label>头像链接</label>
            <el-input v-model="form.avatar" placeholder="请输入头像图片 URL" />
          </div>

          <div class="form-item">
            <label>性别</label>
            <el-radio-group v-model="form.gender">
              <el-radio-button :label="0">未知</el-radio-button>
              <el-radio-button :label="1">男</el-radio-button>
              <el-radio-button :label="2">女</el-radio-button>
            </el-radio-group>
          </div>

          <div class="form-item">
            <label>年龄</label>
            <el-input-number v-model="form.age" :min="0" :max="120" />
          </div>
        </div>
      </el-card>

      <el-card class="glass-effect form-card">
        <template #header>
          <div class="card-title">
            <el-icon><Monitor /></el-icon>
            <span>身体指标</span>
          </div>
        </template>

        <div class="form-grid">
          <div class="form-item">
            <label>身高 (cm)</label>
            <el-input-number v-model="form.height" :min="0" :precision="1" />
          </div>

          <div class="form-item">
            <label>体重 (kg)</label>
            <el-input-number v-model="form.weight" :min="0" :precision="1" />
          </div>

          <div class="form-item">
            <label>体脂率 (%)</label>
            <el-input-number v-model="form.bodyFat" :min="0" :precision="1" />
          </div>

          <div class="form-item">
            <label>目标热量 (kcal)</label>
            <el-input-number v-model="form.targetCalories" :min="0" :precision="0" />
          </div>
        </div>

        <div class="derived-grid">
          <div class="derived-item">
            <span>BMI</span>
            <strong>{{ bmi ? bmi.toFixed(1) : '--' }}</strong>
          </div>
          <div class="derived-item">
            <span>BMR</span>
            <strong>{{ bmr || '--' }}</strong>
          </div>
        </div>
      </el-card>
    </section>

    <section class="content-grid">
      <el-card class="glass-effect note-card">
        <template #header>
          <div class="card-title">
            <el-icon><Opportunity /></el-icon>
            <span>健康档案小贴士</span>
          </div>
        </template>

        <div class="note-list">
          <div class="note-item">把年龄、身高、体重和目标热量补完整后，首页看到的热量进度会更贴近你当天的真实状态。</div>
          <div class="note-item">体重或体脂率有变化时，记得及时更新，这样饮食记录和阶段趋势会更容易看出自己的节奏。</div>
          <div class="note-item">如果你最近在减脂、增肌或调整作息，也可以回到这里重新设置目标热量，让每天的饮食参考更合适。</div>
        </div>
      </el-card>

      <el-card class="glass-effect action-card">
        <template #header>
          <div class="card-title">
            <el-icon><EditPen /></el-icon>
            <span>保存修改</span>
          </div>
        </template>

        <div class="action-copy">
          <p>保存后，首页热量进度、饮食记录汇总和相关健康指标都会按你最新的档案信息更新。</p>
          <el-button type="primary" size="large" :loading="saving" @click="saveAll">保存全部信息</el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<style scoped lang="scss">
.user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.hero-card {
  padding: 24px 26px;
  border-radius: 30px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(255, 232, 211, 0.96), transparent 42%),
    linear-gradient(135deg, rgba(255, 250, 244, 0.98), rgba(255, 239, 223, 0.88));
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-copy h2 {
  margin: 12px 0 10px;
  font-size: clamp(28px, 2.8vw, 40px);
  color: #7c2d12;
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff0e1;
  color: #9a3412;
  font-size: 12px;
  font-weight: 800;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #7c2d12;
  font-size: 12px;
  font-weight: 700;
}

.tag.warm {
  background: #fff0e1;
  color: #b45309;
}

.hero-stats {
  display: grid;
  gap: 12px;
}

.stat-box,
.derived-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.9);
  display: grid;
  gap: 8px;
}

.stat-box span,
.form-item label,
.derived-item span {
  color: #6b7280;
  font-size: 13px;
}

.stat-box strong,
.derived-item strong {
  color: #7c2d12;
  font-size: 26px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-card,
.note-card,
.action-card {
  border-radius: 30px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-item {
  display: grid;
  gap: 8px;
}

.derived-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.note-list {
  display: grid;
  gap: 12px;
}

.note-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.76);
  color: #7c2d12;
  line-height: 1.8;
}

.action-copy {
  display: grid;
  gap: 16px;
  align-content: start;
}

.action-copy p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .hero-card,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-main,
  .form-grid,
  .derived-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
