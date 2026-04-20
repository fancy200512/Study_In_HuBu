<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Document, Histogram, Lightning, PieChart, ScaleToOriginal } from '@element-plus/icons-vue'

import { getAdviceAPI, getCaloriesTrendAPI, getNutritionRatioAPI, type CaloriesTrendPoint, type NutritionRatio } from '@/api/analytics'
import { getDietLogAPI, type DietLog } from '@/api/dietLog'
import { useCurrentUser } from '@/composables/useCurrentUser'
import { getLocalDateString } from '@/utils/date'
import { formatCalories, getBmiStatus, toNumber } from '@/utils/health'

const { ensureUserLoaded, userStore } = useCurrentUser()

const loading = ref(false)
const todayLog = ref<DietLog | null>(null)
const trendPoints = ref<CaloriesTrendPoint[]>([])
const nutritionRatio = ref<NutritionRatio>({
  proteinRatio: 0,
  fatRatio: 0,
  carbsRatio: 0
})
const adviceList = ref<string[]>([])

const trendChartRef = ref<HTMLDivElement | null>(null)
const ratioChartRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
let ratioChart: echarts.ECharts | null = null

const targetCalories = computed(() => toNumber(userStore.userInfo.targetCalories))
const totalCalories = computed(() => toNumber(todayLog.value?.totalCalories))
const calorieDiff = computed(() => totalCalories.value - targetCalories.value)
const calorieProgress = computed(() => {
  if (!targetCalories.value) return 0
  return Math.min(100, Math.round((totalCalories.value / targetCalories.value) * 100))
})

const heroAdvice = computed(() => {
  if (adviceList.value.length) return adviceList.value[0]
  if (!totalCalories.value) return '今天还没有饮食记录，可以先去识别或补录一餐。'
  if (!targetCalories.value) return '先在个人中心完善目标热量，首页会自动展示进度判断。'
  if (calorieDiff.value > 250) return `今天摄入比目标高 ${Math.round(calorieDiff.value)} kcal，晚餐建议更清淡一些。`
  if (calorieDiff.value < -250) return `今天摄入比目标少 ${Math.abs(Math.round(calorieDiff.value))} kcal，可以补充优质蛋白和主食。`
  return '今天的摄入整体比较接近目标，继续保持这个节奏。'
})

const statCards = computed(() => [
  {
    label: '今日总热量',
    value: formatCalories(todayLog.value?.totalCalories),
    caption: '来自 diet-log 当日汇总'
  },
  {
    label: '目标热量',
    value: targetCalories.value ? formatCalories(targetCalories.value) : '未设置',
    caption: '来自用户身体档案'
  },
  {
    label: 'BMI',
    value: userStore.userInfo.bmi ? userStore.userInfo.bmi.toFixed(1) : '--',
    caption: getBmiStatus(userStore.userInfo.bmi)
  },
  {
    label: '基础代谢',
    value: userStore.userInfo.bmr ? `${Math.round(userStore.userInfo.bmr)} kcal` : '--',
    caption: 'BMR'
  }
])

const macroCards = computed(() => [
  { label: '蛋白质', value: `${toNumber(todayLog.value?.protein).toFixed(1)} g`, color: '#f97316' },
  { label: '脂肪', value: `${toNumber(todayLog.value?.fat).toFixed(1)} g`, color: '#fb7185' },
  { label: '碳水', value: `${toNumber(todayLog.value?.carbs).toFixed(1)} g`, color: '#f59e0b' }
])

const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  trendChart.setOption({
    grid: { left: 28, right: 18, top: 44, bottom: 24, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: trendPoints.value.map((item) => item.date.slice(5)),
      axisLine: { show: false },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#f1f5f9',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: trendPoints.value.map((item) => item.calories),
        symbolSize: 10,
        lineStyle: { width: 4, color: '#ff7a18' },
        itemStyle: { color: '#ff7a18' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 122, 24, 0.28)' },
            { offset: 1, color: 'rgba(255, 122, 24, 0)' }
          ])
        }
      }
    ]
  })
}

const renderRatioChart = () => {
  if (!ratioChartRef.value) return

  if (ratioChart) ratioChart.dispose()
  ratioChart = echarts.init(ratioChartRef.value)

  ratioChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 4 },
    series: [
      {
        type: 'pie',
        radius: ['52%', '75%'],
        itemStyle: { borderRadius: 12, borderColor: '#fff', borderWidth: 6 },
        label: {
          formatter: '{b}\n{d}%'
        },
        data: [
          { name: '蛋白质', value: nutritionRatio.value.proteinRatio, itemStyle: { color: '#f97316' } },
          { name: '脂肪', value: nutritionRatio.value.fatRatio, itemStyle: { color: '#fb7185' } },
          { name: '碳水', value: nutritionRatio.value.carbsRatio, itemStyle: { color: '#f59e0b' } }
        ]
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
  ratioChart?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    await ensureUserLoaded()

    const today = getLocalDateString()
    const [logRes, trendRes, ratioRes, adviceRes] = await Promise.all([
      getDietLogAPI(today),
      getCaloriesTrendAPI(),
      getNutritionRatioAPI(),
      getAdviceAPI()
    ])

    todayLog.value = logRes.data
    trendPoints.value = trendRes.data || []
    nutritionRatio.value = ratioRes.data || nutritionRatio.value
    adviceList.value = adviceRes.data || []

    await nextTick()
    renderTrendChart()
    renderRatioChart()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  ratioChart?.dispose()
})
</script>

<template>
  <div
    class="dashboard-page"
    v-loading="loading"
    element-loading-text="正在加载首页数据..."
    element-loading-background="rgba(255, 255, 255, 0.72)"
  >
    <section class="hero-card glass-effect">
      <div class="hero-copy">
        <span class="eyebrow">今日饮食概览</span>
        <h2>{{ heroAdvice }}</h2>
      </div>

      <div class="hero-progress">
        <el-progress
          type="dashboard"
          :percentage="calorieProgress"
          :stroke-width="14"
          color="#ff7a18"
        >
          <template #default>
            <div class="progress-value">{{ Math.round(totalCalories) }}</div>
            <div class="progress-label">kcal</div>
          </template>
        </el-progress>
        <div class="progress-meta">
          <span>目标 {{ targetCalories ? Math.round(targetCalories) : '--' }} kcal</span>
          <b>{{ calorieDiff > 0 ? '+' : '' }}{{ Math.round(calorieDiff) }} kcal</b>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <article v-for="card in statCards" :key="card.label" class="stat-card glass-effect">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.caption }}</small>
      </article>
    </section>

    <section class="macro-grid">
      <article v-for="item in macroCards" :key="item.label" class="macro-card glass-effect">
        <div class="macro-dot" :style="{ background: item.color }"></div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <el-card class="chart-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><Histogram /></el-icon>
            <span>近 7 天热量趋势</span>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-shell"></div>
      </el-card>

      <el-card class="chart-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><PieChart /></el-icon>
            <span>近 7 天营养比例</span>
          </div>
        </template>
        <div ref="ratioChartRef" class="chart-shell"></div>
      </el-card>
    </section>

    <section class="bottom-grid">
      <el-card class="glass-effect advice-card">
        <template #header>
          <div class="card-title">
            <el-icon><Lightning /></el-icon>
            <span>饮食建议</span>
          </div>
        </template>

        <el-empty v-if="!adviceList.length" description="暂无建议" :image-size="80" />
        <div v-else class="advice-list">
          <div v-for="item in adviceList" :key="item" class="advice-item">
            <span class="bullet"></span>
            <p>{{ item }}</p>
          </div>
        </div>
      </el-card>

      <el-card class="glass-effect body-card">
        <template #header>
          <div class="card-title">
            <el-icon><ScaleToOriginal /></el-icon>
            <span>身体档案快照</span>
          </div>
        </template>

        <div class="body-grid">
          <div class="body-item">
            <span>年龄</span>
            <strong>{{ userStore.userInfo.age || '--' }}</strong>
          </div>
          <div class="body-item">
            <span>身高</span>
            <strong>{{ userStore.userInfo.height || '--' }} cm</strong>
          </div>
          <div class="body-item">
            <span>体重</span>
            <strong>{{ userStore.userInfo.weight || '--' }} kg</strong>
          </div>
          <div class="body-item">
            <span>体脂率</span>
            <strong>{{ userStore.userInfo.bodyFat || '--' }}%</strong>
          </div>
        </div>

        <div class="note-box">
          <el-icon><Document /></el-icon>
          <span>如果这些数值还是空的，可以去个人中心补全后再回来刷新首页。</span>
        </div>
      </el-card>
    </section>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.hero-card {
  padding: 24px 26px;
  border-radius: 30px;
  display: grid;
  grid-template-columns: 1.3fr 320px;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(255, 232, 211, 0.96), transparent 42%),
    linear-gradient(135deg, rgba(255, 250, 244, 0.98), rgba(255, 239, 223, 0.88));
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff0e1;
  color: #9a3412;
  font-size: 12px;
  font-weight: 800;
}

.hero-copy h2 {
  margin: 12px 0 10px;
  font-size: clamp(28px, 2.8vw, 40px);
  line-height: 1.15;
  color: #7c2d12;
}

.hero-copy p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.8;
}

.hero-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.8);
}

.progress-value {
  font-size: 34px;
  font-weight: 800;
  color: #ea580c;
  line-height: 1;
}

.progress-label {
  margin-top: 6px;
  font-size: 12px;
  color: #9a3412;
}

.progress-meta {
  text-align: center;
  display: grid;
  gap: 4px;

  span {
    color: #64748b;
    font-size: 13px;
  }

  b {
    color: #7c2d12;
    font-size: 18px;
  }
}

.stats-grid,
.macro-grid,
.content-grid,
.bottom-grid {
  display: grid;
  gap: 16px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  padding: 20px 22px;
  display: grid;
  gap: 8px;
}

.stat-card span,
.macro-card span,
.body-item span {
  color: #6b7280;
  font-size: 13px;
}

.stat-card strong,
.macro-card strong,
.body-item strong {
  color: #7c2d12;
}

.stat-card strong {
  font-size: 24px;
}

.stat-card small {
  color: #9ca3af;
}

.macro-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.macro-card {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.macro-card strong {
  display: block;
  margin-top: 4px;
  font-size: 22px;
}

.macro-dot {
  width: 12px;
  height: 48px;
  border-radius: 999px;
}

.content-grid,
.bottom-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-card,
.advice-card,
.body-card {
  min-height: 100%;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.chart-shell {
  width: 100%;
  height: 320px;
}

.advice-list {
  display: grid;
  gap: 12px;
}

.advice-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.72);
}

.bullet {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ff7a18;
  margin-top: 7px;
  flex-shrink: 0;
}

.advice-item p {
  margin: 0;
  line-height: 1.8;
  color: #7c2d12;
}

.body-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.body-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.9);
  display: grid;
  gap: 8px;
}

.body-item strong {
  font-size: 24px;
}

.note-box {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.72);
  display: flex;
  gap: 10px;
  align-items: center;
  color: #9a3412;
  line-height: 1.6;
}

@media (max-width: 1100px) {
  .hero-card,
  .content-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .macro-grid,
  .body-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 20px;
  }
}
</style>
