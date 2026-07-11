<template>
  <main class="project-setup page-frame">
    <header class="page-heading">
      <RouterLink to="/project-deep-dive">← 项目深挖</RouterLink>
      <div><p class="page-kicker">PROJECT SETUP</p><h1>{{ profile ? '确认项目档案' : '准备你的项目面试' }}</h1></div>
      <el-tag v-if="profile" :type="statusMeta.type" effect="plain">{{ statusMeta.label }}</el-tag>
    </header>

    <div v-if="accessLost" class="access-lost-state">
      <h2>当前浏览器会话中没有这个项目的访问令牌</h2>
      <p>为了保护项目内容，令牌只在创建时返回一次，并保存在 sessionStorage。请从创建项目时使用的标签页继续，或重新创建档案。</p>
      <RouterLink class="primary-link" to="/project-deep-dive/new">重新创建项目</RouterLink>
    </div>

    <ProjectDescriptionForm v-else-if="!profile" v-model="draft" :loading="analyzing" @analyze="createAndAnalyze" />

    <template v-else>
      <div v-if="analyzing || profile.analysisStatus === 'ANALYZING'" class="analysis-state">
        <span class="analysis-state__orb"></span><p class="page-kicker">ANALYZING</p><h2>正在拆解项目事实与可追问声明</h2><p>系统只会基于你提供的内容提取，不会为你编造不存在的指标。</p>
      </div>

      <div v-else-if="profile.analysisStatus === 'FAILED' || profile.analysisStatus === 'DRAFT'" class="access-lost-state">
        <h2>{{ profile.analysisStatus === 'FAILED' ? '上一次分析没有完成' : '项目档案还没有开始分析' }}</h2>
        <p>脱敏后的项目描述已经安全保存，可以直接重新分析，不需要再次粘贴。</p>
        <el-button type="primary" size="large" :loading="analyzing" @click="retryAnalysis">重新分析项目</el-button>
      </div>

      <template v-else-if="reviewForm">
        <ProjectExtractionReview :form="reviewForm" :source-description="profile.sanitizedDescription" :readonly="profile.analysisStatus === 'READY'" />
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
            <el-button v-if="profile.analysisStatus !== 'READY'" size="large" :loading="saving" @click="saveReview">保存修改</el-button>
            <el-button v-if="profile.analysisStatus !== 'READY'" type="primary" size="large" :loading="confirming" @click="confirmReview">确认并准备面试</el-button>
            <el-button v-else type="primary" size="large" :loading="starting" @click="startInterview">开始项目面试</el-button>
          </div>
        </section>
      </template>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
const settings = reactive({ durationMinutes: 20, maxFollowUpsPerClaim: 3, inputModality: 'TEXT' as InputModality })

const numericProfileId = computed(() => props.profileId ? Number(props.profileId) : null)
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

async function loadProfile() {
  if (!numericProfileId.value) return
  const token = store.restoreProfileAccess(numericProfileId.value)
  if (!token) { accessLost.value = true; return }
  const response = await getProjectProfile(numericProfileId.value, token)
  applyProfile(response.data.data)
}

async function analyze(profileId: number, token: string) {
  analyzing.value = true
  try {
    const response = await analyzeProjectProfile(profileId, token)
    applyProfile(response.data.data)
  } finally {
    analyzing.value = false
  }
}

async function createAndAnalyze() {
  if (draft.value.trim().length < 20 || analyzing.value) return
  analyzing.value = true
  try {
    const response = await createProjectProfile(draft.value.trim())
    const created = response.data.data
    store.saveProfileAccess(created.profileId, created.accessToken)
    clearDraft()
    await router.replace({ name: 'project-deep-dive-profile', params: { profileId: created.profileId } })
    await analyze(created.profileId, created.accessToken)
  } finally {
    analyzing.value = false
  }
}

async function retryAnalysis() {
  if (!profile.value || !store.accessToken) return
  await analyze(profile.value.profileId, store.accessToken)
}

async function persistReview() {
  if (!profile.value || !reviewForm.value || !store.accessToken) return null
  const response = await patchProjectProfile(profile.value.profileId, store.accessToken, {
    version: profile.value.version,
    ...reviewForm.value,
  })
  applyProfile(response.data.data)
  return response.data.data
}

async function saveReview() {
  saving.value = true
  try { await persistReview(); ElMessage.success('项目档案已保存') } finally { saving.value = false }
}

async function confirmReview() {
  if (!profile.value || !store.accessToken) return
  confirming.value = true
  try {
    const saved = await persistReview()
    if (!saved) return
    const response = await confirmProjectProfile(saved.profileId, store.accessToken)
    applyProfile(response.data.data)
    ElMessage.success('项目档案已确认')
  } finally {
    confirming.value = false
  }
}

async function startInterview() {
  if (!profile.value || !store.accessToken || profile.value.analysisStatus !== 'READY') return
  starting.value = true
  try {
    const response = await createProjectInterview({
      mode: 'PROJECT_DEEP_DIVE',
      projectProfileId: profile.value.profileId,
      durationMinutes: settings.durationMinutes,
      maxFollowUpsPerClaim: settings.maxFollowUpsPerClaim,
      inputModality: settings.inputModality,
    }, store.accessToken)
    const session = response.data.data
    store.saveSessionAccess(session.sessionId, profile.value.profileId, store.accessToken)
    await router.push({ name: 'project-interview-room', params: { sessionId: session.sessionId } })
  } finally {
    starting.value = false
  }
}

watch(numericProfileId, () => { if (!profile.value) loadProfile() })
onMounted(loadProfile)
</script>
