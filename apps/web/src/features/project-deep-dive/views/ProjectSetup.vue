<template>
  <main class="project-setup page-frame">
    <header class="workspace-heading setup-heading">
      <div><RouterLink class="back-link" to="/project-deep-dive">← 项目深挖</RouterLink><p class="page-kicker">PROJECT SETUP</p><h1>{{ profile ? '确认项目档案' : '准备项目面试' }}</h1></div>
      <div class="setup-heading__aside"><p>先把项目事实整理准确，面试官才会沿着真实证据继续追问。</p><el-tag v-if="profile" :type="statusMeta.type" effect="plain">{{ statusMeta.label }}</el-tag></div>
    </header>

    <ol class="setup-steps" aria-label="项目面试准备进度">
      <li :class="{ active: !profile, done: Boolean(profile) }"><span>1</span><div><strong>输入项目</strong><small>提供真实经历</small></div></li>
      <li :class="{ active: Boolean(profile) && profile?.analysisStatus !== 'READY', done: profile?.analysisStatus === 'READY' }"><span>2</span><div><strong>确认事实</strong><small>修正提取结果</small></div></li>
      <li :class="{ active: profile?.analysisStatus === 'READY' }"><span>3</span><div><strong>开始面试</strong><small>设置时长与追问</small></div></li>
    </ol>

    <div v-if="accessLost" class="access-lost-state">
      <h2>当前页面缺少这个项目的访问凭证</h2>
      <p>为保护项目内容，请从创建项目时使用的标签页继续，或重新创建档案。</p>
      <RouterLink class="primary-link" to="/project-deep-dive/new">重新创建项目</RouterLink>
    </div>

    <div v-else-if="profileLoading" class="analysis-state">
      <span class="analysis-state__orb"></span><h2>正在读取项目档案</h2><p>正在恢复已保存的项目内容和确认状态。</p>
    </div>

    <div v-else-if="profileLoadError" class="access-lost-state">
      <h2>暂时无法读取项目档案</h2><p>{{ profileLoadError }}</p>
      <el-button type="primary" :loading="profileLoading" @click="loadProfile()">重新连接</el-button>
    </div>

    <ProjectDescriptionForm v-else-if="!numericProfileId && !profile" v-model="draft" :loading="analyzing" @analyze="createAndAnalyze" />

    <template v-else-if="profile">
      <div v-if="profile.analysisStatus === 'ANALYZING' && analysisStalled" class="access-lost-state">
        <h2>项目分析等待时间较长</h2>
        <p>暂时没有拿到完整结果。项目描述已经保存，可以安全重新分析，不需要再次粘贴。</p>
        <el-button type="primary" size="large" :loading="analyzing" @click="retryAnalysis">检查并重新分析</el-button>
      </div>

      <div v-else-if="analyzing || profile.analysisStatus === 'ANALYZING'" class="analysis-state">
        <span class="analysis-state__orb"></span><p class="page-kicker">ANALYZING</p><h2>正在拆解项目事实与可追问声明</h2><p>系统只会基于你提供的内容提取，不会为你编造不存在的指标。</p>
      </div>

      <div v-else-if="profile.analysisStatus === 'FAILED' || profile.analysisStatus === 'DRAFT'" class="access-lost-state">
        <h2>{{ profile.analysisStatus === 'FAILED' ? '上一次分析没有完成' : '项目档案还没有开始分析' }}</h2>
        <p>脱敏后的项目描述已经安全保存，可以直接重新分析，不需要再次粘贴。</p>
        <el-button type="primary" size="large" :loading="analyzing" @click="retryAnalysis">重新分析项目</el-button>
      </div>

      <template v-else-if="reviewForm">
        <ProjectExtractionReview :form="reviewForm" :source-description="profile.sanitizedDescription" :readonly="reviewReadonly" />
        <section class="setup-actions-card">
          <div>
            <p class="page-kicker">STEP 03 · INTERVIEW SETTINGS</p>
            <h2>{{ profile.analysisStatus === 'READY' ? '项目档案已确认，可以开始面试' : '确认这些信息，并进入面试' }}</h2>
            <p>默认 20 分钟、每个核心声明最多追问 3 次。第一版使用文字输入。</p>
          </div>
          <el-collapse class="advanced-settings">
            <el-collapse-item title="高级设置" name="advanced">
              <el-form label-position="top">
                <el-form-item label="预计时长"><el-slider v-model="settings.durationMinutes" :min="10" :max="40" :step="5" show-input /></el-form-item>
                <el-form-item label="单个声明最多追问"><el-slider v-model="settings.maxFollowUpsPerClaim" :min="2" :max="5" show-stops /></el-form-item>
                <el-form-item label="输入方式"><el-radio-group v-model="settings.inputModality"><el-radio-button label="TEXT">文字</el-radio-button><el-radio-button label="VOICE_TRANSCRIPT" disabled>语音（后续）</el-radio-button></el-radio-group></el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          <div class="setup-actions-card__buttons">
            <el-button v-if="profile.analysisStatus === 'READY' && !editingReady" size="large" :disabled="saving || confirming || starting" @click="editingReady = true">编辑档案</el-button>
            <el-button v-if="profile.analysisStatus !== 'READY' || editingReady" size="large" :loading="saving" :disabled="confirming || starting" @click="saveReview">保存修改</el-button>
            <el-button v-if="profile.analysisStatus !== 'READY' || editingReady" type="primary" size="large" :loading="confirming" :disabled="saving || starting" @click="confirmReview">确认并准备面试</el-button>
            <el-button v-if="profile.analysisStatus === 'READY' && !editingReady" type="primary" size="large" :loading="starting" :disabled="saving || confirming" @click="startInterview">开始项目面试</el-button>
          </div>
        </section>
      </template>
    </template>

    <div v-else class="access-lost-state">
      <h2>无法读取这个项目档案</h2><p>档案地址无效，请返回项目深挖后重新选择。</p>
      <RouterLink class="primary-link" to="/project-deep-dive">返回项目深挖</RouterLink>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, useRouter } from 'vue-router'
import ProjectDescriptionForm from '@/features/project-deep-dive/components/ProjectDescriptionForm.vue'
import ProjectExtractionReview from '@/features/project-deep-dive/components/ProjectExtractionReview.vue'
import { useProjectDraft } from '@/features/project-deep-dive/composables/useProjectDraft'
import { analyzeProjectProfile, confirmProjectProfile, createProjectProfile, getProjectProfile, patchProjectProfile } from '@/features/project-deep-dive/api/projectDeepDiveApi'
import { createProjectInterview } from '@/features/project-deep-dive/api/interviewApi'
import type { InputModality, ProjectProfile, ProjectReviewForm } from '@/features/project-deep-dive/model/types'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

const props = defineProps<{ profileId?: string }>()
const router = useRouter()
const store = useInterviewSessionStore()
const { draft, clearDraft } = useProjectDraft()
const profile = ref<ProjectProfile | null>(null)
const reviewForm = ref<ProjectReviewForm | null>(null)
const analyzing = ref(false)
const saving = ref(false)
const confirming = ref(false)
const starting = ref(false)
const accessLost = ref(false)
const editingReady = ref(false)
const profileLoading = ref(false)
const profileLoadError = ref('')
const analysisStalled = ref(false)
const settings = reactive({ durationMinutes: 20, maxFollowUpsPerClaim: 3, inputModality: 'TEXT' as InputModality })
const ANALYSIS_POLL_INTERVAL_MS = 2_500
const ANALYSIS_LEASE_GRACE_MS = 125_000
const SERVER_TIME_ZONE_OFFSET = '+08:00'
let routeEpoch = 0
let profileLoadRequest = 0
let analysisRequest = 0
let analysisPollTimer: number | undefined
let disposed = false

const numericProfileId = computed(() => props.profileId ? Number(props.profileId) : null)
const reviewReadonly = computed(() => profile.value?.analysisStatus === 'READY' && !editingReady.value)
const statusMeta = computed(() => {
  const status = profile.value?.analysisStatus
  if (status === 'READY') return { label: '已确认', type: 'success' as const }
  if (status === 'FAILED') return { label: '分析失败', type: 'danger' as const }
  if (status === 'ANALYZING') return { label: '分析中', type: 'primary' as const }
  return { label: '待确认', type: 'warning' as const }
})

function hydrateReview(value: ProjectProfile) {
  reviewForm.value = {
    projectName: value.projectName || '',
    summary: value.summary || '',
    techStack: [...(value.techStack || [])],
    responsibilities: [...(value.responsibilities || [])],
    metrics: [...(value.metrics || [])],
    architecture: [...(value.architecture || [])],
    uncertainties: [...(value.uncertainties || [])],
    claims: (value.claims || []).map(claim => ({
      claimId: claim.claimId,
      claimType: claim.claimType,
      statement: claim.statement,
      sourceFragment: claim.sourceFragment,
      relatedTechnologies: [...(claim.relatedTechnologies || [])],
    })),
  }
}

function applyProfile(value: ProjectProfile) {
  profile.value = value
  hydrateReview(value)
}

function isCurrentProfile(profileId: number, epoch: number) {
  return !disposed && routeEpoch === epoch && numericProfileId.value === profileId
}

function stopAnalysisPolling() {
  if (analysisPollTimer !== undefined) window.clearTimeout(analysisPollTimer)
  analysisPollTimer = undefined
}

function analysisLeaseStartedAt(value: ProjectProfile, observedAt = Date.now()) {
  const timestamp = /(?:Z|[+-]\d{2}:\d{2})$/i.test(value.updateTime)
    ? value.updateTime
    : `${value.updateTime}${SERVER_TIME_ZONE_OFFSET}`
  const serverUpdateTime = Date.parse(timestamp)
  if (!Number.isFinite(serverUpdateTime) || serverUpdateTime > observedAt) return observedAt
  return serverUpdateTime
}

function scheduleAnalysisPolling(profileId: number, token: string, epoch: number,
  requestId: number, startedAt: number) {
  if (!isCurrentProfile(profileId, epoch) || requestId !== analysisRequest) return
  stopAnalysisPolling()
  if (Date.now() - startedAt >= ANALYSIS_LEASE_GRACE_MS) {
    analysisStalled.value = true
    return
  }
  analysisPollTimer = window.setTimeout(async () => {
    if (!isCurrentProfile(profileId, epoch) || requestId !== analysisRequest) return
    try {
      const latest = await getProjectProfile(profileId, token)
      if (!isCurrentProfile(profileId, epoch) || requestId !== analysisRequest) return
      applyProfile(latest.data.data)
      if (latest.data.data.analysisStatus !== 'ANALYZING') {
        analysisStalled.value = false
        if (latest.data.data.analysisStatus === 'REVIEW_REQUIRED' || latest.data.data.analysisStatus === 'READY') {
          ElMessage.success('项目分析已经完成，已恢复最新档案')
        }
        return
      }
    } catch {
      // Keep polling within the processing lease; the retry state below is the fallback.
    }
    if (!isCurrentProfile(profileId, epoch) || requestId !== analysisRequest) return
    if (Date.now() - startedAt >= ANALYSIS_LEASE_GRACE_MS) {
      analysisStalled.value = true
      return
    }
    scheduleAnalysisPolling(profileId, token, epoch, requestId, startedAt)
  }, ANALYSIS_POLL_INTERVAL_MS)
}

async function loadProfile(epoch = routeEpoch) {
  const profileId = numericProfileId.value
  if (!profileId) return
  const requestId = ++profileLoadRequest
  const token = store.restoreProfileAccess(profileId)
  if (!token) {
    if (isCurrentProfile(profileId, epoch) && requestId === profileLoadRequest) accessLost.value = true
    return
  }
  profileLoading.value = true
  profileLoadError.value = ''
  try {
    const response = await getProjectProfile(profileId, token)
    if (isCurrentProfile(profileId, epoch) && requestId === profileLoadRequest) {
      applyProfile(response.data.data)
      if (response.data.data.analysisStatus === 'ANALYZING') {
        const pollingRequest = ++analysisRequest
        analysisStalled.value = false
        scheduleAnalysisPolling(profileId, token, epoch, pollingRequest,
          analysisLeaseStartedAt(response.data.data))
      }
    }
  } catch {
    if (isCurrentProfile(profileId, epoch) && requestId === profileLoadRequest) profileLoadError.value = '请检查网络连接后重试。'
  } finally {
    if (isCurrentProfile(profileId, epoch) && requestId === profileLoadRequest) profileLoading.value = false
  }
}

async function analyze(profileId: number, token: string) {
  const epoch = routeEpoch
  const requestId = ++analysisRequest
  profileLoadRequest++
  profileLoading.value = false
  stopAnalysisPolling()
  analysisStalled.value = false
  analyzing.value = true
  try {
    const response = await analyzeProjectProfile(profileId, token)
    if (isCurrentProfile(profileId, epoch) && requestId === analysisRequest) applyProfile(response.data.data)
  } catch {
    try {
      const latest = await getProjectProfile(profileId, token)
      if (!isCurrentProfile(profileId, epoch) || requestId !== analysisRequest) return
      applyProfile(latest.data.data)
      if (latest.data.data.analysisStatus === 'REVIEW_REQUIRED' || latest.data.data.analysisStatus === 'READY') {
        ElMessage.success('项目分析已经完成，已恢复最新档案')
      } else if (latest.data.data.analysisStatus === 'ANALYZING') {
        scheduleAnalysisPolling(profileId, token, epoch, requestId,
          analysisLeaseStartedAt(latest.data.data))
      } else {
        ElMessage.error('项目分析没有完成，可以直接重新分析')
      }
    } catch {
      if (isCurrentProfile(profileId, epoch) && requestId === analysisRequest) {
        ElMessage.error('暂时无法确认分析状态，请检查网络后重试')
      }
    }
  } finally {
    if (isCurrentProfile(profileId, epoch) && requestId === analysisRequest) analyzing.value = false
  }
}

async function createAndAnalyze() {
  if (draft.value.trim().length < 20 || analyzing.value) return
  const epoch = routeEpoch
  const requestId = ++analysisRequest
  analyzing.value = true
  try {
    const response = await createProjectProfile(draft.value.trim())
    if (disposed || routeEpoch !== epoch || requestId !== analysisRequest) return
    const created = response.data.data
    store.saveProfileAccess(created.profileId, created.accessToken)
    clearDraft()
    await router.replace({ name: 'project-deep-dive-profile', params: { profileId: created.profileId } })
    if (disposed || numericProfileId.value !== created.profileId) return
    await analyze(created.profileId, created.accessToken)
  } finally {
    if (!disposed && routeEpoch === epoch && requestId === analysisRequest) analyzing.value = false
  }
}

async function retryAnalysis() {
  if (!profile.value || !store.accessToken) return
  await analyze(profile.value.profileId, store.accessToken)
}

async function persistReview() {
  if (!profile.value || !reviewForm.value || !store.accessToken) return null
  const epoch = routeEpoch
  const profileId = profile.value.profileId
  const token = store.accessToken
  profileLoadRequest++
  const response = await patchProjectProfile(profileId, token, {
    version: profile.value.version,
    ...reviewForm.value,
  })
  if (!isCurrentProfile(profileId, epoch)) return null
  applyProfile(response.data.data)
  return response.data.data
}

async function saveReview() {
  const epoch = routeEpoch
  saving.value = true
  try {
    const saved = await persistReview()
    if (!saved || routeEpoch !== epoch) return
    editingReady.value = false
    ElMessage.success('项目档案已保存')
  } finally {
    if (!disposed && routeEpoch === epoch) saving.value = false
  }
}

async function confirmReview() {
  if (!profile.value || !store.accessToken) return
  const epoch = routeEpoch
  const profileId = profile.value.profileId
  const token = store.accessToken
  confirming.value = true
  try {
    const saved = await persistReview()
    if (!saved || !isCurrentProfile(profileId, epoch)) return
    const response = await confirmProjectProfile(saved.profileId, token)
    if (!isCurrentProfile(profileId, epoch)) return
    applyProfile(response.data.data)
    editingReady.value = false
    ElMessage.success('项目档案已确认')
  } finally {
    if (!disposed && routeEpoch === epoch) confirming.value = false
  }
}

async function startInterview() {
  if (!profile.value || !store.accessToken || profile.value.analysisStatus !== 'READY') return
  const epoch = routeEpoch
  const profileId = profile.value.profileId
  const token = store.accessToken
  starting.value = true
  try {
    const response = await createProjectInterview({
      mode: 'PROJECT_DEEP_DIVE',
      projectProfileId: profileId,
      durationMinutes: settings.durationMinutes,
      maxFollowUpsPerClaim: settings.maxFollowUpsPerClaim,
      inputModality: settings.inputModality,
    }, token)
    if (!isCurrentProfile(profileId, epoch)) return
    const session = response.data.data
    store.saveSessionAccess(session.sessionId, profileId, token)
    await router.push({ name: 'project-interview-room', params: { sessionId: session.sessionId } })
  } finally {
    if (!disposed && routeEpoch === epoch) starting.value = false
  }
}

watch(numericProfileId, () => {
  const epoch = ++routeEpoch
  profileLoadRequest++
  analysisRequest++
  stopAnalysisPolling()
  profile.value = null
  reviewForm.value = null
  analyzing.value = false
  saving.value = false
  confirming.value = false
  starting.value = false
  accessLost.value = false
  editingReady.value = false
  analysisStalled.value = false
  profileLoading.value = false
  profileLoadError.value = ''
  void loadProfile(epoch)
}, { immediate: true })

onBeforeUnmount(() => {
  disposed = true
  routeEpoch++
  analysisRequest++
  profileLoadRequest++
  stopAnalysisPolling()
})
</script>
