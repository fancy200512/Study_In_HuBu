<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { DataLine, Lightning, PieChart, Tickets } from '@element-plus/icons-vue'

import { getAdviceAPI, getCaloriesTrendAPI, getNutritionRatioAPI, type CaloriesTrendPoint } from '@/api/analytics'
import { getDietHistoryAPI, type DietLog } from '@/api/dietLog'
import { useCurrentUser } from '@/composables/useCurrentUser'
import { formatCalories, roundTo, toNumber } from '@/utils/health'

const { ensureUserLoaded } = useCurrentUser()

const loading = ref(false)
const historyList = ref<DietLog[]>([])
const trendPoints = ref<CaloriesTrendPoint[]>([])
const adviceList = ref<string[]>([])
const nutritionRatio = ref({
  proteinRatio: 0,
  fatRatio: 0,
  carbsRatio: 0
})

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const historyTableData = computed(() => historyList.value.slice(0, 14))
const recordDays = computed(() => historyList.value.length)
const latestRecordDate = computed(() => historyList.value[0]?.date || '--')
const averageCalories = computed(() => {
  if (!historyList.value.length) return 0
  const total = historyList.value.reduce((sum, item) => sum + toNumber(item.totalCalories), 0)
  return Math.round(total / historyList.value.length)
})

const nutritionRows = computed(() => [
  { label: '蛋白质', value: nutritionRatio.value.proteinRatio, color: '#f97316' },
  { label: '脂肪', value: nutritionRatio.value.fatRatio, color: '#fb7185' },
  { label: '碳水', value: nutritionRatio.value.carbsRatio, color: '#f59e0b' }
])

const renderChart = () => {
  if (!chartRef.value) return

  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  chartInstance.setOption({
    grid: { left: 26, right: 18, top: 36, bottom: 24, containLabel: true },
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
        type: 'bar',
        barWidth: 18,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ffb676' },
            { offset: 1, color: '#ff7a18' }
          ]),
          borderRadius: [10, 10, 0, 0]
        },
        data: trendPoints.value.map((item) => item.calories)
      }
    ]
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    await ensureUserLoaded()

    const [historyRes, trendRes, ratioRes, adviceRes] = await Promise.all([
      getDietHistoryAPI(),
      getCaloriesTrendAPI(),
      getNutritionRatioAPI(),
      getAdviceAPI()
    ])

    historyList.value = historyRes.data || []
    trendPoints.value = trendRes.data || []
    nutritionRatio.value = ratioRes.data || nutritionRatio.value
    adviceList.value = adviceRes.data || []

    await nextTick()
    renderChart()
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
  chartInstance?.dispose()
})
</script>

<template>
  <div
    class="report-page"
    v-loading="loading"
    element-loading-text="正在生成饮食报告..."
    element-loading-background="rgba(255, 255, 255, 0.72)"
  >
    <section class="hero-grid">
      <article class="hero-card glass-effect">
        <span class="eyebrow">饮食报告</span>
        <h2>从最近的饮食记录里，看看你的饮食状态是否稳定</h2>
        <p>这里会结合近几天的热量变化、营养比例和饮食建议，帮你更快看出自己最近吃得是否均衡、有没有需要调整的地方。</p>
      </article>

      <article class="summary-panel glass-effect">
        <div class="summary-item">
          <span>记录天数</span>
          <strong>{{ recordDays }}</strong>
        </div>
        <div class="summary-item">
          <span>最近一次记录</span>
          <strong>{{ latestRecordDate }}</strong>
        </div>
        <div class="summary-item">
          <span>平均热量</span>
          <strong>{{ averageCalories ? `${averageCalories} kcal` : '--' }}</strong>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <el-card class="glass-effect chart-card">
        <template #header>
          <div class="card-title">
            <el-icon><DataLine /></el-icon>
            <span>近 7 天热量变化</span>
          </div>
        </template>
        <div ref="chartRef" class="chart-shell"></div>
      </el-card>

      <el-card class="glass-effect ratio-card">
        <template #header>
          <div class="card-title">
            <el-icon><PieChart /></el-icon>
            <span>近 7 天营养比例</span>
          </div>
        </template>

        <div class="ratio-list">
          <div v-for="row in nutritionRows" :key="row.label" class="ratio-row">
            <div class="ratio-meta">
              <span>{{ row.label }}</span>
              <b>{{ roundTo(row.value) }}%</b>
            </div>
            <el-progress :percentage="roundTo(row.value)" :stroke-width="12" :color="row.color" />
          </div>
        </div>
      </el-card>
    </section>

    <section class="content-grid">
      <el-card class="glass-effect history-card">
        <template #header>
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>历史汇总</span>
          </div>
        </template>

        <el-table :data="historyTableData" stripe>
          <el-table-column prop="date" label="日期" width="140" />
          <el-table-column label="热量" min-width="130">
            <template #default="{ row }">{{ formatCalories(row.totalCalories) }}</template>
          </el-table-column>
          <el-table-column label="蛋白质" min-width="120">
            <template #default="{ row }">{{ roundTo(row.protein) }} g</template>
          </el-table-column>
          <el-table-column label="脂肪" min-width="120">
            <template #default="{ row }">{{ roundTo(row.fat) }} g</template>
          </el-table-column>
          <el-table-column label="碳水" min-width="120">
            <template #default="{ row }">{{ roundTo(row.carbs) }} g</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="glass-effect advice-card">
        <template #header>
          <div class="card-title">
            <el-icon><Lightning /></el-icon>
            <span>规则建议</span>
          </div>
        </template>

        <el-empty v-if="!adviceList.length" description="暂无建议" :image-size="84" />
        <div v-else class="advice-list">
          <div v-for="item in adviceList" :key="item" class="advice-item">
            <span class="bullet"></span>
            <p>{{ item }}</p>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<style scoped lang="scss">
.report-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.hero-grid,
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 16px;
}

.hero-card,
.summary-panel {
  padding: 24px 26px;
  border-radius: 30px;
}

.hero-card {
  background:
    radial-gradient(circle at top left, rgba(255, 232, 211, 0.96), transparent 42%),
    linear-gradient(135deg, rgba(255, 250, 244, 0.98), rgba(255, 239, 223, 0.88));
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

.hero-card h2 {
  margin: 12px 0 10px;
  font-size: clamp(28px, 2.6vw, 38px);
  color: #7c2d12;
  line-height: 1.2;
}

.hero-card p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.8;
}

.summary-panel {
  display: grid;
  gap: 12px;
}

.summary-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.9);
  display: grid;
  gap: 8px;
}

.summary-item span {
  color: #6b7280;
  font-size: 13px;
}

.summary-item strong {
  color: #7c2d12;
  font-size: 24px;
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
  height: 310px;
}

.ratio-list {
  display: grid;
  gap: 16px;
}

.ratio-row {
  display: grid;
  gap: 10px;
}

.ratio-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ratio-meta span {
  color: #6b7280;
  font-size: 13px;
}

.ratio-meta b {
  color: #7c2d12;
  font-size: 18px;
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

@media (max-width: 1100px) {
  .hero-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
