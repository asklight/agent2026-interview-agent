<template>
  <main class="app-shell">
    <section class="topbar">
      <div>
        <h1>北洋 Java 面试官</h1>
        <p>Java 后端技术面试训练智能体</p>
      </div>
      <el-button :icon="Connection" :loading="loading" type="primary" @click="checkHealth">
        检查后端
      </el-button>
    </section>

    <el-alert
      class="status-band"
      :closable="false"
      :title="statusTitle"
      :type="healthy ? 'success' : 'warning'"
      show-icon
    />

    <section class="workspace">
      <aside class="setup-panel">
        <div class="panel-header">
          <h2>训练设置</h2>
          <el-button :loading="moduleLoading" text type="primary" @click="loadModules">刷新</el-button>
        </div>

        <el-form label-position="top">
          <el-form-item label="题库模块">
            <el-select v-model="selectedModule" class="full-width" placeholder="选择模块">
              <el-option v-for="module in questionModules" :key="module" :label="module" :value="module" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度">
            <el-select v-model="selectedDifficulty" class="full-width" clearable placeholder="不限">
              <el-option label="easy" value="easy" />
              <el-option label="medium" value="medium" />
              <el-option label="hard" value="hard" />
            </el-select>
          </el-form-item>

          <el-form-item label="题目数">
            <el-input-number v-model="questionCount" :max="20" :min="1" class="full-width" />
          </el-form-item>
        </el-form>

        <el-button
          :disabled="!selectedModule"
          :icon="VideoPlay"
          :loading="sessionLoading"
          class="full-width"
          type="primary"
          @click="startSession"
        >
          开始训练
        </el-button>

        <div v-if="session" class="session-meta">
          <div>
            <span>Session</span>
            <strong>#{{ session.sessionId }}</strong>
          </div>
          <div>
            <span>状态</span>
            <strong>{{ session.status }}</strong>
          </div>
          <div>
            <span>进度</span>
            <strong>{{ progressText }}</strong>
          </div>
        </div>
      </aside>

      <section class="training-panel">
        <div v-if="activeQuestion" class="question-block">
          <div class="question-meta">
            <el-tag effect="plain">{{ questionTypeText(activeQuestion.questionType) }}</el-tag>
            <el-tag effect="plain" type="info">{{ activeQuestion.module }}</el-tag>
            <el-tag effect="plain" type="warning">{{ activeQuestion.difficulty }}</el-tag>
          </div>

          <h2>{{ activeQuestion.questionText }}</h2>

          <el-input
            v-model="answerText"
            :autosize="{ minRows: 7, maxRows: 12 }"
            placeholder="输入你的回答"
            type="textarea"
          />

          <div class="action-row">
            <el-button
              :disabled="!canSubmit"
              :icon="Check"
              :loading="submitLoading"
              type="primary"
              @click="submitAnswer"
            >
              提交回答
            </el-button>
            <el-button
              :disabled="!canGoNext"
              :icon="ArrowRight"
              :loading="nextLoading"
              @click="goNextQuestion"
            >
              下一题
            </el-button>
            <el-button
              :disabled="!session || session.status === 'FINISHED'"
              :icon="CircleClose"
              @click="finishSession"
            >
              结束
            </el-button>
          </div>
        </div>

        <el-empty v-else-if="!session" :image-size="92" description="选择模块后开始训练" />

        <div v-else class="question-block question-block--done">
          <h2>{{ session.status === 'FINISHED' ? '本次训练已结束' : '当前题目已完成' }}</h2>
          <div class="action-row">
            <el-button
              v-if="session.status !== 'FINISHED'"
              :icon="ArrowRight"
              :loading="nextLoading"
              type="primary"
              @click="goNextQuestion"
            >
              下一题
            </el-button>
            <el-button v-if="session.status !== 'FINISHED'" :icon="CircleClose" @click="finishSession">
              结束
            </el-button>
          </div>
        </div>

        <section v-if="evaluationText" class="evaluation-panel">
          <div class="panel-header">
            <h2>本轮反馈</h2>
            <el-tag v-if="nextAction" effect="plain" type="success">{{ nextActionText }}</el-tag>
          </div>
          <div v-if="hasStructuredFeedback" class="feedback-grid">
            <div v-if="feedbackScore !== null" class="feedback-score">
              <span>建议分数</span>
              <strong>{{ feedbackScore }}</strong>
            </div>
            <div v-if="hitPoints.length" class="feedback-list">
              <h3>命中要点</h3>
              <ul>
                <li v-for="item in hitPoints" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="missingPoints.length" class="feedback-list">
              <h3>遗漏要点</h3>
              <ul>
                <li v-for="item in missingPoints" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="weaknesses.length" class="feedback-list">
              <h3>薄弱表现</h3>
              <ul>
                <li v-for="item in weaknesses" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
          <pre>{{ evaluationText }}</pre>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check, CircleClose, Connection, VideoPlay } from '@element-plus/icons-vue'
import { getHealth } from '@/api/modules/health'
import { getQuestionModules } from '@/api/modules/questionCard'
import {
  createInterviewSession,
  finishInterviewSession,
  nextInterviewQuestion,
  submitInterviewAnswer,
  type CurrentQuestion,
  type InterviewSession,
  type SubmitAnswerResult,
} from '@/api/modules/interview'

const loading = ref(false)
const moduleLoading = ref(false)
const sessionLoading = ref(false)
const submitLoading = ref(false)
const nextLoading = ref(false)

const healthMessage = ref('')
const healthy = ref(false)
const questionModules = ref<string[]>([])
const selectedModule = ref('')
const selectedDifficulty = ref('')
const questionCount = ref(5)

const session = ref<InterviewSession | null>(null)
const activeQuestion = ref<CurrentQuestion | null>(null)
const answerText = ref('')
const evaluationText = ref('')
const nextAction = ref<SubmitAnswerResult['nextAction'] | ''>('')
const feedbackScore = ref<number | null>(null)
const hitPoints = ref<string[]>([])
const missingPoints = ref<string[]>([])
const weaknesses = ref<string[]>([])

const statusTitle = computed(() => {
  if (healthy.value) return `后端已连接：${healthMessage.value}`
  if (healthMessage.value) return healthMessage.value
  return '等待后端健康检查'
})

const progressText = computed(() => {
  if (!session.value) return '0/0'
  return `${session.value.completedQuestionCount}/${session.value.questionCount}`
})

const canSubmit = computed(() => {
  return Boolean(
    session.value?.status === 'IN_PROGRESS'
      && activeQuestion.value
      && answerText.value.trim()
      && !submitLoading.value,
  )
})

const canGoNext = computed(() => {
  return Boolean(session.value?.status === 'IN_PROGRESS' && nextAction.value === 'NEXT_QUESTION')
})

const nextActionText = computed(() => {
  if (nextAction.value === 'ASK_FOLLOW_UP') return '进入追问'
  if (nextAction.value === 'NEXT_QUESTION') return '可进入下一题'
  if (nextAction.value === 'FINISH_SESSION') return '训练结束'
  return ''
})

const hasStructuredFeedback = computed(() => {
  return Boolean(
    feedbackScore.value !== null
      || hitPoints.value.length
      || missingPoints.value.length
      || weaknesses.value.length,
  )
})

function questionTypeText(type: CurrentQuestion['questionType']) {
  if (type === 'FOLLOW_UP') return '追问'
  if (type === 'EVALUATED') return '已点评'
  return '主问题'
}

async function checkHealth() {
  loading.value = true
  try {
    const response = await getHealth()
    healthMessage.value = response.data.data
    healthy.value = true
  } catch {
    healthMessage.value = '后端暂不可用，请确认 Spring Boot 已启动'
    healthy.value = false
  } finally {
    loading.value = false
  }
}

async function loadModules() {
  moduleLoading.value = true
  try {
    const response = await getQuestionModules()
    questionModules.value = response.data.data
    if (!selectedModule.value && questionModules.value.length > 0) {
      selectedModule.value = questionModules.value[0]
    }
  } finally {
    moduleLoading.value = false
  }
}

async function startSession() {
  if (!selectedModule.value) return

  sessionLoading.value = true
  try {
    const response = await createInterviewSession({
      module: selectedModule.value,
      difficulty: selectedDifficulty.value || undefined,
      questionCount: questionCount.value,
    })
    applySession(response.data.data)
    clearFeedback()
    answerText.value = ''
  } finally {
    sessionLoading.value = false
  }
}

async function submitAnswer() {
  if (!session.value || !answerText.value.trim()) {
    ElMessage.warning('回答不能为空')
    return
  }

  submitLoading.value = true
  try {
    const response = await submitInterviewAnswer(session.value.sessionId, {
      answerText: answerText.value.trim(),
    })
    const result = response.data.data
    applyFeedback(result)
    session.value.completedQuestionCount = result.completedQuestionCount
    session.value.questionCount = result.questionCount

    if (result.currentQuestion) {
      activeQuestion.value = result.currentQuestion
      answerText.value = ''
      return
    }

    activeQuestion.value = null
    answerText.value = ''
    if (result.nextAction === 'FINISH_SESSION') {
      session.value.status = 'FINISHED'
    }
  } finally {
    submitLoading.value = false
  }
}

async function goNextQuestion() {
  if (!session.value) return

  nextLoading.value = true
  try {
    const response = await nextInterviewQuestion(session.value.sessionId)
    applySession(response.data.data)
    clearFeedback()
    answerText.value = ''
  } finally {
    nextLoading.value = false
  }
}

async function finishSession() {
  if (!session.value) return

  const response = await finishInterviewSession(session.value.sessionId)
  applySession(response.data.data)
  activeQuestion.value = null
  nextAction.value = 'FINISH_SESSION'
}

function applySession(nextSession: InterviewSession) {
  session.value = nextSession
  activeQuestion.value = nextSession.currentQuestion ?? null
}

function applyFeedback(result: SubmitAnswerResult) {
  evaluationText.value = result.evaluationText
  nextAction.value = result.nextAction
  feedbackScore.value = typeof result.score === 'number' ? result.score : null
  hitPoints.value = result.hitPoints ?? []
  missingPoints.value = result.missingPoints ?? []
  weaknesses.value = result.weaknesses ?? []
}

function clearFeedback() {
  evaluationText.value = ''
  nextAction.value = ''
  feedbackScore.value = null
  hitPoints.value = []
  missingPoints.value = []
  weaknesses.value = []
}

onMounted(() => {
  checkHealth()
  loadModules()
})
</script>
