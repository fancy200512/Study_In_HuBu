<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import { Calendar, Delete, RefreshRight, Tickets, UploadFilled, View } from '@element-plus/icons-vue'

import { getDietLogAPI, type DietLog } from '@/api/dietLog'
import {
  refineRecognitionAPI,
  recognizeAPI,
  type ImageNutrition,
  type NeedMoreInfoType,
  type RecognitionFollowUpItem,
  type RecognitionResponse
} from '@/api/recognition'
import { deleteSessionAPI, getSessionDetailAPI, getSessionListAPI, updateSessionMealTypeAPI, type SessionRecord } from '@/api/session'
import { formatDateTime, getLocalDateString } from '@/utils/date'
import { formatCalories, roundTo, toNumber } from '@/utils/health'
import { getMealTypeLabel, MEAL_TYPE_OPTIONS, type MealType } from '@/utils/meal'

type RefineDialogMode = 'manual' | 'auto'
type PendingNeedMoreInfoType = Extract<NeedMoreInfoType, 'WEIGHT' | 'COUNT' | 'COOKING_METHOD'>
const NEED_MORE_INFO_TYPES: PendingNeedMoreInfoType[] = ['WEIGHT', 'COUNT', 'COOKING_METHOD']
const INFO_TYPE_LABELS: Record<PendingNeedMoreInfoType, string> = {
  WEIGHT: '重量',
  COUNT: '个数',
  COOKING_METHOD: '烹饪方式'
}

const uploadFileList = ref<UploadUserFile[]>([])
const latestRecognition = ref<RecognitionResponse | null>(null)
const recognitionLoading = ref(false)

const sessionLoading = ref(false)
const sessionQuery = reactive({
  page: 1,
  pageSize: 8,
  date: getLocalDateString()
})
const sessions = ref<SessionRecord[]>([])
const sessionTotal = ref(0)
const sessionListMessage = ref('')
const selectedDayLog = ref<DietLog | null>(null)

const sessionDetailVisible = ref(false)
const sessionDetail = ref<SessionRecord | null>(null)

const refineDialogVisible = ref(false)
const refining = ref(false)
const refineDialogMode = ref<RefineDialogMode>('manual')
const autoRefineQueue = ref<number[]>([])
const autoRefineTotal = ref(0)
const refineSelections = ref<Record<string, string>>({})
const refineForm = reactive({
  sessionId: 0,
  imageId: 0
})

const recognitionForm = reactive({
  mealType: 'LUNCH' as MealType,
  prompt: ''
})

const selectedFileCount = computed(() => uploadFileList.value.filter((item) => item.raw).length)
const currentRefineImage = computed(
  () => latestRecognition.value?.images.find((item) => item.imageId === refineForm.imageId) || null
)
const currentFollowUpItems = computed(() => normalizeFollowUpItems(currentRefineImage.value?.followUpItems))
const pendingRefineCount = computed(() => latestRecognition.value?.images.filter((item) => hasFollowUpItems(item)).length || 0)
const autoRefineStep = computed(() => {
  if (refineDialogMode.value !== 'auto' || !autoRefineTotal.value) return 0
  return autoRefineTotal.value - autoRefineQueue.value.length
})
const currentPrimaryInfoType = computed(() => getPendingNeedMoreInfoType(currentFollowUpItems.value[0]?.infoType))
const refineDialogTitle = computed(() => `补充${getInfoTypeLabel(currentPrimaryInfoType.value)}`)
const refineDialogActionText = computed(() => {
  if (refineDialogMode.value !== 'auto') return '提交选择'
  return autoRefineQueue.value.length ? '提交并继续下一张' : '提交并完成'
})
const refineDialogCancelText = computed(() => (refineDialogMode.value === 'auto' ? '跳过这张' : '取消'))
const refineDialogProgressText = computed(() => {
  if (refineDialogMode.value !== 'auto' || !autoRefineTotal.value) return ''
  return `待补充 ${autoRefineStep.value}/${autoRefineTotal.value}`
})
const missingRefineSelectionCount = computed(
  () => currentFollowUpItems.value.filter((_, index) => !refineSelections.value[String(index)]).length
)

const recognitionSummary = computed(() => {
  if (!latestRecognition.value) return null
  return {
    calories: formatCalories(latestRecognition.value.totalCalories),
    protein: `${roundTo(latestRecognition.value.protein)} g`,
    fat: `${roundTo(latestRecognition.value.fat)} g`,
    carbs: `${roundTo(latestRecognition.value.carbs)} g`
  }
})

const getPendingNeedMoreInfoType = (needMoreInfo?: string | null): PendingNeedMoreInfoType | null =>
  NEED_MORE_INFO_TYPES.includes(needMoreInfo as PendingNeedMoreInfoType)
    ? (needMoreInfo as PendingNeedMoreInfoType)
    : null

const getInfoTypeLabel = (needMoreInfo?: string | null) => {
  const type = getPendingNeedMoreInfoType(needMoreInfo)
  return type ? INFO_TYPE_LABELS[type] : '信息'
}

const normalizeFollowUpItems = (items?: RecognitionFollowUpItem[] | null): RecognitionFollowUpItem[] =>
  (items || [])
    .map((item) => ({
      foodName: item.foodName?.trim() || '',
      infoType: item.infoType?.toString().trim().toUpperCase() || 'NONE',
      options: (item.options || []).map((option) => option.trim()).filter(Boolean)
    }))
    .filter((item) => item.foodName && getPendingNeedMoreInfoType(item.infoType) && item.options.length)

const buildFallbackFollowUpItems = (needMoreInfo?: string | null): RecognitionFollowUpItem[] => {
  const infoType = getPendingNeedMoreInfoType(needMoreInfo)
  if (!infoType) return []

  if (infoType === 'WEIGHT') {
    return [
      {
        foodName: '主要食物',
        infoType,
        options: ['约100g', '约200g', '约300g', '其他/不确定']
      }
    ]
  }

  if (infoType === 'COUNT') {
    return [
      {
        foodName: '主要食物',
        infoType,
        options: ['1个', '2个', '3个', '其他/不确定']
      }
    ]
  }

  return [
    {
      foodName: '主要食物',
      infoType,
      options: ['清蒸/水煮', '少油煎制', '煎炸/重油', '其他/不确定']
    }
  ]
}

const hasFollowUpItems = (image?: ImageNutrition | null) => normalizeFollowUpItems(image?.followUpItems).length > 0

const normalizeImageNutrition = (image: ImageNutrition): ImageNutrition => {
  const followUpItems = normalizeFollowUpItems(image.followUpItems)
  const effectiveFollowUpItems = followUpItems.length
    ? followUpItems
    : buildFallbackFollowUpItems(image.needMoreInfo)
  return {
    ...image,
    needMoreInfo: effectiveFollowUpItems[0]?.infoType || getPendingNeedMoreInfoType(image.needMoreInfo) || 'NONE',
    followUpItems: effectiveFollowUpItems
  }
}

const normalizeRecognitionResponse = (data: RecognitionResponse): RecognitionResponse => ({
  ...data,
  images: (data.images || []).map((item) => normalizeImageNutrition(item))
})

const getFollowUpSummary = (image: ImageNutrition) =>
  normalizeFollowUpItems(image.followUpItems)
    .map((item) => `${item.foodName}${getInfoTypeLabel(item.infoType)}`)
    .join('、')

const pageRange = computed(() => {
  if (!sessionTotal.value || !sessions.value.length) return '暂无记录'
  const start = (sessionQuery.page - 1) * sessionQuery.pageSize + 1
  const end = Math.min(start + sessions.value.length - 1, sessionTotal.value)
  return `${start}-${end} / ${sessionTotal.value}`
})

const normalizeRecognitionTotals = () => {
  if (!latestRecognition.value) return

  const totals = latestRecognition.value.images.reduce(
    (sum, item) => {
      sum.totalCalories += toNumber(item.totalCalories)
      sum.protein += toNumber(item.protein)
      sum.fat += toNumber(item.fat)
      sum.carbs += toNumber(item.carbs)
      return sum
    },
    { totalCalories: 0, protein: 0, fat: 0, carbs: 0 }
  )

  latestRecognition.value = {
    ...latestRecognition.value,
    ...totals
  }
}

const resetRefineFlow = () => {
  refineDialogMode.value = 'manual'
  autoRefineQueue.value = []
  autoRefineTotal.value = 0
}

const closeRefineDialog = () => {
  refineDialogVisible.value = false
  refineForm.sessionId = 0
  refineForm.imageId = 0
  refineSelections.value = {}
}

const updateRecognitionImage = (imageId: number, next: Partial<ImageNutrition>) => {
  if (!latestRecognition.value) return

  latestRecognition.value = {
    ...latestRecognition.value,
    images: latestRecognition.value.images.map((item) =>
      item.imageId === imageId ? { ...item, ...next } : item
    )
  }
}

const openRefineDialog = (image: ImageNutrition, mode: RefineDialogMode = 'manual') => {
  if (!latestRecognition.value) return
  if (!hasFollowUpItems(image)) {
    ElMessage.info('这张图片当前没有可选的补充项')
    return
  }
  refineDialogMode.value = mode
  refineForm.sessionId = latestRecognition.value.sessionId
  refineForm.imageId = image.imageId
  refineSelections.value = {}
  refineDialogVisible.value = true
}

const finishAutoRefineFlow = () => {
  const total = autoRefineTotal.value
  closeRefineDialog()
  resetRefineFlow()

  if (total) {
    ElMessage.success('补充流程已结束')
  }
}

const openNextAutoRefineDialog = () => {
  if (!latestRecognition.value) {
    finishAutoRefineFlow()
    return
  }

  while (autoRefineQueue.value.length) {
    const nextImageId = autoRefineQueue.value.shift()
    const image = latestRecognition.value.images.find((item) => item.imageId === nextImageId)
    if (image) {
      openRefineDialog(image, 'auto')
      return
    }
  }

  finishAutoRefineFlow()
}

const startAutoRefineFlow = () => {
  if (!latestRecognition.value) return

  const queue = latestRecognition.value.images
    .filter((item) => hasFollowUpItems(item))
    .map((item) => item.imageId)

  if (!queue.length) {
    resetRefineFlow()
    return
  }

  autoRefineQueue.value = queue
  autoRefineTotal.value = queue.length
  openNextAutoRefineDialog()
}

const selectFollowUpOption = (index: number, option: string) => {
  refineSelections.value = {
    ...refineSelections.value,
    [String(index)]: option
  }
}

const isFollowUpOptionSelected = (index: number, option: string) => refineSelections.value[String(index)] === option

const buildRefineExtraInfo = () =>
  currentFollowUpItems.value
    .map((item, index) => `${item.foodName}的${getInfoTypeLabel(item.infoType)}：${refineSelections.value[String(index)]}`)
    .join('；')

const loadDayLog = async () => {
  const res = await getDietLogAPI(sessionQuery.date)
  selectedDayLog.value = res.data
}

const loadSessions = async () => {
  sessionLoading.value = true
  sessionListMessage.value = ''

  try {
    const res = await getSessionListAPI({
      page: sessionQuery.page,
      pageSize: sessionQuery.pageSize,
      date: sessionQuery.date
    })
    sessions.value = res.data?.records || []
    sessionTotal.value = res.data?.total || 0
  } catch {
    sessions.value = []
    sessionTotal.value = 0
    sessionListMessage.value =
      '如果这里拿不到列表，请确认前端是通过本目录的 Vite 代理启动的。当前后端 `GET /session/list` 在 controller 中使用了 `@RequestBody`，浏览器需要代理桥接。'
  } finally {
    sessionLoading.value = false
  }
}

const reloadHistory = async () => {
  await Promise.all([loadSessions(), loadDayLog()])
}

const submitRecognition = async () => {
  const files: File[] = []
  uploadFileList.value.forEach((item) => {
    if (item.raw) {
      files.push(item.raw)
    }
  })

  if (!files.length) {
    ElMessage.warning('请先选择至少一张图片')
    return
  }

  recognitionLoading.value = true
  try {
    const res = await recognizeAPI({
      mealType: recognitionForm.mealType,
      prompt: recognitionForm.prompt,
      files
    })

    const normalizedRecognition = normalizeRecognitionResponse(res.data)
    latestRecognition.value = normalizedRecognition
    uploadFileList.value = []
    recognitionForm.prompt = ''
    sessionQuery.date = getLocalDateString()
    sessionQuery.page = 1
    await reloadHistory()

    const needRefineImages = normalizedRecognition.images.filter((item) => hasFollowUpItems(item))
    if (needRefineImages.length) {
      ElMessage.success(`识别完成，大模型为 ${needRefineImages.length} 张图片生成了补充选项`)
      startAutoRefineFlow()
    } else {
      ElMessage.success('识别完成')
    }
  } finally {
    recognitionLoading.value = false
  }
}

const handleRefineCancel = () => {
  if (refining.value) return

  if (refineDialogMode.value === 'auto') {
    ElMessage.info('已跳过当前图片，你也可以稍后手动补充')
    openNextAutoRefineDialog()
    return
  }

  closeRefineDialog()
}

const submitRefine = async () => {
  if (!refineForm.imageId) return
  if (!currentFollowUpItems.value.length) {
    ElMessage.warning('当前图片没有可提交的补充选项')
    return
  }
  if (missingRefineSelectionCount.value) {
    ElMessage.warning('请先为每个食物选择一个选项')
    return
  }

  const extraInfo = buildRefineExtraInfo()
  const isAutoMode = refineDialogMode.value === 'auto'
  refining.value = true
  try {
    const res = await refineRecognitionAPI({
      sessionId: refineForm.sessionId || undefined,
      imageId: refineForm.imageId,
      extraInfo
    })

    updateRecognitionImage(res.data.imageId, normalizeImageNutrition(res.data))
    normalizeRecognitionTotals()
    await reloadHistory()

    if (isAutoMode) {
      if (autoRefineQueue.value.length) {
        ElMessage.success('当前图片已重新识别，请继续选择下一张')
        openNextAutoRefineDialog()
      } else {
        finishAutoRefineFlow()
      }
    } else {
      closeRefineDialog()
      ElMessage.success('已按选择结果重新识别')
    }
  } finally {
    refining.value = false
  }
}

const openSessionDetail = async (row: SessionRecord) => {
  const res = await getSessionDetailAPI(row.id)
  sessionDetail.value = res.data
  sessionDetailVisible.value = true
}

const handleMealTypeChange = async (row: SessionRecord, mealType: string) => {
  await updateSessionMealTypeAPI(row.id, mealType)
  row.mealType = mealType
  ElMessage.success('餐别已更新')
}

const handleDelete = async (row: SessionRecord) => {
  await ElMessageBox.confirm('删除后该次识别记录将不可恢复，确定继续吗？', '删除会话', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })

  await deleteSessionAPI(row.id)
  ElMessage.success('已删除该条识别记录')

  if (sessions.value.length === 1 && sessionQuery.page > 1) {
    sessionQuery.page -= 1
  }

  await reloadHistory()
}

const handlePageChange = (page: number) => {
  sessionQuery.page = page
  void loadSessions()
}

const handleDateChange = () => {
  sessionQuery.page = 1
  void reloadHistory()
}

const handleMealTypeSelect = (row: SessionRecord, value: string | number | boolean) => {
  void handleMealTypeChange(row, String(value))
}

onMounted(() => {
  void reloadHistory()
})
</script>

<template>
  <div class="diet-page">
    <section class="top-grid">
      <el-card class="glass-effect upload-card">
        <template #header>
          <div class="card-title">
            <el-icon><UploadFilled /></el-icon>
            <span>多图识别</span>
          </div>
        </template>

        <div class="upload-form">
          <div class="form-row">
            <label>餐别</label>
            <el-select v-model="recognitionForm.mealType">
              <el-option
                v-for="item in MEAL_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>

          <div class="form-row">
            <label>补充提示词</label>
            <el-input
              v-model="recognitionForm.prompt"
              type="textarea"
              :rows="3"
              placeholder="可选，例如：请重点识别主食重量和烹饪油脂。"
            />
          </div>

          <el-upload
            v-model:file-list="uploadFileList"
            drag
            multiple
            :auto-upload="false"
            :show-file-list="true"
            accept="image/*"
            class="upload-box"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽图片到这里，或 <em>点击选择</em></div>
          </el-upload>

          <div class="action-row">
            <span>当前已选择 {{ selectedFileCount }} 张图片</span>
            <el-button type="primary" :loading="recognitionLoading" @click="submitRecognition">
              开始识别
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="glass-effect result-card">
        <template #header>
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>最新识别结果</span>
          </div>
        </template>

        <template v-if="latestRecognition">
          <div class="result-summary">
            <div class="summary-chip warm">Session #{{ latestRecognition.sessionId }}</div>
            <div class="summary-metrics">
              <div>
                <span>总热量</span>
                <strong>{{ recognitionSummary?.calories }}</strong>
              </div>
              <div>
                <span>蛋白质</span>
                <strong>{{ recognitionSummary?.protein }}</strong>
              </div>
              <div>
                <span>脂肪</span>
                <strong>{{ recognitionSummary?.fat }}</strong>
              </div>
              <div>
                <span>碳水</span>
                <strong>{{ recognitionSummary?.carbs }}</strong>
              </div>
            </div>

            <div v-if="pendingRefineCount" class="pending-refine-banner">
              <div class="pending-refine-copy">
                <strong>有 {{ pendingRefineCount }} 张图片已生成补充选项</strong>
                <span>系统已经挑出最值得确认的食物，你只需要点选对应选项，我们会根据你的选择自动更新识别结果。</span>
              </div>
              <el-button plain type="primary" @click="startAutoRefineFlow">继续补充</el-button>
            </div>
          </div>

          <div class="image-result-list">
            <article v-for="image in latestRecognition.images" :key="image.imageId" class="image-result-item">
              <el-image :src="image.imageUrl" fit="cover" class="food-image" :preview-src-list="[image.imageUrl]" />
              <div class="image-copy">
                <div class="image-head">
                  <span>图片 #{{ image.imageId }}</span>
                  <el-button v-if="hasFollowUpItems(image)" link type="primary" @click="openRefineDialog(image)">
                    选择补充项
                  </el-button>
                  <span v-else class="image-status-text">无需补充</span>
                </div>
                <div v-if="hasFollowUpItems(image)" class="image-alert">
                  <el-tag type="warning" effect="light">待补充 {{ image.followUpItems?.length || 0 }} 项</el-tag>
                  <span>{{ getFollowUpSummary(image) }}</span>
                </div>
                <div class="image-metrics">
                  <span>{{ formatCalories(image.totalCalories) }}</span>
                  <span>P {{ roundTo(image.protein) }} g</span>
                  <span>F {{ roundTo(image.fat) }} g</span>
                  <span>C {{ roundTo(image.carbs) }} g</span>
                </div>
              </div>
            </article>
          </div>
        </template>

        <el-empty v-else description="还没有识别结果" :image-size="88" />
      </el-card>
    </section>

    <section class="history-grid">
      <el-card class="glass-effect history-card">
        <template #header>
          <div class="history-head">
            <div class="card-title">
              <el-icon><Calendar /></el-icon>
              <span>会话记录</span>
            </div>

            <div class="history-toolbar">
              <el-date-picker
                v-model="sessionQuery.date"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="筛选日期"
                @change="handleDateChange"
              />
              <el-button :icon="RefreshRight" @click="reloadHistory">刷新</el-button>
            </div>
          </div>
        </template>

        <div class="history-summary">
          <div class="summary-block">
            <span>日期</span>
            <strong>{{ sessionQuery.date }}</strong>
          </div>
          <div class="summary-block">
            <span>当日热量</span>
            <strong>{{ selectedDayLog ? formatCalories(selectedDayLog.totalCalories) : '--' }}</strong>
          </div>
          <div class="summary-block">
            <span>蛋白质 / 脂肪 / 碳水</span>
            <strong>
              {{
                selectedDayLog
                  ? `${roundTo(selectedDayLog.protein)} / ${roundTo(selectedDayLog.fat)} / ${roundTo(selectedDayLog.carbs)} g`
                  : '--'
              }}
            </strong>
          </div>
          <div class="summary-block">
            <span>分页</span>
            <strong>{{ pageRange }}</strong>
          </div>
        </div>

        <div v-if="sessionListMessage" class="history-note">{{ sessionListMessage }}</div>

        <el-table :data="sessions" v-loading="sessionLoading" stripe class="history-table">
          <el-table-column prop="createTime" label="识别时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>

          <el-table-column label="餐别" min-width="160">
            <template #default="{ row }">
              <el-select
                :model-value="row.mealType"
                size="small"
                @change="handleMealTypeSelect(row, $event)"
              >
                <el-option
                  v-for="item in MEAL_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="营养汇总" min-width="280">
            <template #default="{ row }">
              <div class="table-metrics">
                <span>{{ formatCalories(row.totalCalories) }}</span>
                <span>P {{ roundTo(row.protein) }} g</span>
                <span>F {{ roundTo(row.fat) }} g</span>
                <span>C {{ roundTo(row.carbs) }} g</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'DONE' ? 'success' : 'warning'" effect="light">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" min-width="170" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" :icon="View" @click="openSessionDetail(row)">详情</el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="sessionQuery.pageSize"
            :current-page="sessionQuery.page"
            :total="sessionTotal"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </section>

    <el-dialog
      v-model="refineDialogVisible"
      :title="refineDialogTitle"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="!refining"
      :show-close="!refining && refineDialogMode !== 'auto'"
    >
      <div class="refine-dialog-body">
        <div v-if="refineDialogProgressText" class="refine-progress">
          <span>{{ refineDialogProgressText }}</span>
          <el-tag type="warning" effect="light">需补充{{ getInfoTypeLabel(currentPrimaryInfoType) }}</el-tag>
        </div>

        <div v-if="currentRefineImage" class="refine-preview">
          <el-image
            :src="currentRefineImage.imageUrl"
            fit="cover"
            class="refine-preview-image"
            :preview-src-list="[currentRefineImage.imageUrl]"
          />
          <div class="refine-preview-copy">
            <strong>图片 #{{ currentRefineImage.imageId }}</strong>
            <span>大模型已经先挑出了最值得确认的食物和补充方向。请为每一项选择一个最接近的答案，再继续重新识别。</span>
            <small>这里不再需要手动输入，直接点选选项即可。</small>
          </div>
        </div>

        <div class="follow-up-list">
          <article
            v-for="(item, index) in currentFollowUpItems"
            :key="`${item.foodName}-${item.infoType}-${index}`"
            class="follow-up-item"
          >
            <div class="follow-up-item-head">
              <div>
                <strong>{{ item.foodName }}</strong>
                <span>补充{{ getInfoTypeLabel(item.infoType) }}</span>
              </div>
              <el-tag effect="light">{{ getInfoTypeLabel(item.infoType) }}</el-tag>
            </div>

            <div class="follow-up-option-grid">
              <button
                v-for="option in item.options"
                :key="option"
                type="button"
                class="follow-up-option"
                :class="{ active: isFollowUpOptionSelected(index, option) }"
                @click="selectFollowUpOption(index, option)"
              >
                {{ option }}
              </button>
            </div>
          </article>
        </div>
      </div>

      <template #footer>
        <el-button @click="handleRefineCancel">{{ refineDialogCancelText }}</el-button>
        <el-button
          type="primary"
          :loading="refining"
          :disabled="!currentFollowUpItems.length || missingRefineSelectionCount > 0"
          @click="submitRefine"
        >
          {{ refineDialogActionText }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="sessionDetailVisible" title="会话详情" size="420px">
      <template v-if="sessionDetail">
        <div class="detail-grid">
          <div class="detail-item">
            <span>会话 ID</span>
            <strong>#{{ sessionDetail.id }}</strong>
          </div>
          <div class="detail-item">
            <span>餐别</span>
            <strong>{{ getMealTypeLabel(sessionDetail.mealType) }}</strong>
          </div>
          <div class="detail-item">
            <span>热量</span>
            <strong>{{ formatCalories(sessionDetail.totalCalories) }}</strong>
          </div>
          <div class="detail-item">
            <span>状态</span>
            <strong>{{ sessionDetail.status }}</strong>
          </div>
          <div class="detail-item">
            <span>蛋白质</span>
            <strong>{{ roundTo(sessionDetail.protein) }} g</strong>
          </div>
          <div class="detail-item">
            <span>脂肪</span>
            <strong>{{ roundTo(sessionDetail.fat) }} g</strong>
          </div>
          <div class="detail-item">
            <span>碳水</span>
            <strong>{{ roundTo(sessionDetail.carbs) }} g</strong>
          </div>
          <div class="detail-item">
            <span>创建时间</span>
            <strong>{{ formatDateTime(sessionDetail.createTime) }}</strong>
          </div>
        </div>

        <div class="drawer-note">
          当前后端 `GET /session/{id}` 返回的是会话汇总实体，不包含历史图片列表，所以这里展示的是后端真实能给到的字段。
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.diet-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 16px;
}

.upload-card,
.result-card,
.history-card {
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

.upload-form {
  display: grid;
  gap: 18px;
}

.form-row {
  display: grid;
  gap: 8px;
}

.form-row label {
  font-size: 13px;
  font-weight: 700;
  color: #7c2d12;
}

.upload-box {
  :deep(.el-upload-dragger) {
    border-radius: 24px;
    background: rgba(255, 250, 244, 0.92);
    border: 1px dashed #ffb676;
    padding: 26px 12px;
  }
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: #6b7280;
  font-size: 13px;
}

.result-summary {
  display: grid;
  gap: 14px;
}

.pending-refine-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 244, 229, 0.96);
  border: 1px solid rgba(251, 146, 60, 0.22);
}

.pending-refine-copy {
  display: grid;
  gap: 6px;
}

.pending-refine-copy strong {
  color: #9a3412;
  font-size: 14px;
}

.pending-refine-copy span {
  color: #7c2d12;
  font-size: 12px;
  line-height: 1.6;
}

.summary-chip {
  display: inline-flex;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.summary-chip.warm {
  background: #fff0e1;
  color: #9a3412;
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-metrics > div {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.9);
  display: grid;
  gap: 6px;
}

.summary-metrics span,
.summary-block span,
.detail-item span {
  color: #6b7280;
  font-size: 12px;
}

.summary-metrics strong,
.summary-block strong,
.detail-item strong {
  color: #7c2d12;
}

.summary-metrics strong {
  font-size: 20px;
}

.image-result-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.image-result-item {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.86);
}

.food-image {
  width: 96px;
  height: 96px;
  border-radius: 18px;
  overflow: hidden;
}

.image-copy {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.image-alert {
  display: grid;
  gap: 8px;
}

.image-alert span {
  color: #7c2d12;
  font-size: 12px;
  line-height: 1.6;
}

.image-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #7c2d12;
  font-weight: 700;
}

.image-status-text {
  color: #9ca3af;
  font-size: 12px;
  font-weight: 600;
}

.image-metrics,
.table-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-metrics span,
.table-metrics span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff0e1;
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.history-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-block {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 250, 244, 0.9);
  display: grid;
  gap: 6px;
}

.summary-block strong {
  font-size: 18px;
}

.history-note {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.8);
  color: #9a3412;
  line-height: 1.7;
  font-size: 13px;
}

.history-table {
  min-height: 300px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.refine-dialog-body {
  display: grid;
  gap: 16px;
}

.refine-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 244, 229, 0.92);
  color: #9a3412;
  font-size: 12px;
  font-weight: 700;
}

.refine-preview {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: 22px;
  background: rgba(255, 250, 244, 0.94);
}

.refine-preview-image {
  width: 120px;
  height: 120px;
  border-radius: 18px;
  overflow: hidden;
}

.refine-preview-copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.refine-preview-copy strong {
  color: #7c2d12;
  font-size: 15px;
}

.refine-preview-copy span {
  color: #9a3412;
  line-height: 1.7;
}

.refine-preview-copy small {
  color: #6b7280;
  line-height: 1.6;
}

.follow-up-list {
  display: grid;
  gap: 12px;
}

.follow-up-item {
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.94);
  display: grid;
  gap: 12px;
}

.follow-up-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.follow-up-item-head > div {
  display: grid;
  gap: 4px;
}

.follow-up-item-head strong {
  color: #7c2d12;
  font-size: 16px;
}

.follow-up-item-head span {
  color: #9a3412;
  font-size: 12px;
}

.follow-up-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.follow-up-option {
  appearance: none;
  border: 1px solid #f5d6b2;
  background: #fff;
  color: #7c2d12;
  border-radius: 16px;
  padding: 12px 14px;
  text-align: left;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  cursor: pointer;
  transition: all 0.2s ease;
}

.follow-up-option:hover {
  border-color: #fb923c;
  box-shadow: 0 8px 24px rgba(251, 146, 60, 0.12);
}

.follow-up-option.active {
  border-color: #f97316;
  background: linear-gradient(135deg, #fff3e5, #ffe3c2);
  color: #9a3412;
  box-shadow: 0 10px 24px rgba(249, 115, 22, 0.16);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 250, 244, 0.92);
  display: grid;
  gap: 8px;
}

.drawer-note {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.82);
  color: #9a3412;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .top-grid {
    grid-template-columns: 1fr;
  }

  .history-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .history-head,
  .history-toolbar,
  .action-row,
  .image-head,
  .pending-refine-banner {
    flex-direction: column;
    align-items: stretch;
  }

  .history-summary,
  .summary-metrics,
  .detail-grid,
  .follow-up-option-grid {
    grid-template-columns: 1fr;
  }

  .image-result-item {
    grid-template-columns: 1fr;
  }

  .food-image {
    width: 100%;
    height: 220px;
  }

  .refine-preview {
    grid-template-columns: 1fr;
  }

  .refine-preview-image {
    width: 100%;
    height: 220px;
  }
}
</style>
