<template>
  <main class="command-center">
    <aside class="control-rail">
      <div class="brand">
        <span class="brand-mark">北</span>
        <div><strong>北洋面试官</strong><small>Java 后端训练控制台</small></div>
      </div>

      <div class="connection" :class="{ online: healthy }">
        <Connection :size="16" />
        <span>{{ healthy ? 'AI 训练服务已连接' : healthMessage || '正在连接训练服务' }}</span>
      </div>

      <section class="setup-card">
        <p class="eyebrow">训练配置</p>
        <el-form label-position="top">
          <el-form-item label="知识模块">
            <el-select v-model="selectedModule" :loading="moduleLoading" class="full-width" placeholder="选择模块">
              <el-option v-for="module in questionModules" :key="module" :label="moduleNames[module] || module" :value="module" />
            </el-select>
          </el-form-item>
          <el-form-item label="挑战难度">
            <el-radio-group v-model="selectedDifficulty" class="difficulty-switch">
              <el-radio-button label="">混合</el-radio-button>
              <el-radio-button label="easy">基础</el-radio-button>
              <el-radio-button label="medium">进阶</el-radio-button>
              <el-radio-button label="hard">高阶</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="训练题数">
            <el-slider v-model="questionCount" :min="3" :max="10" :show-tooltip="false" />
            <div class="slider-caption"><span>快速热身</span><strong>{{ questionCount }} 题</strong><span>深度模拟</span></div>
          </el-form-item>
          <el-button class="start-button" :loading="sessionLoading" :disabled="!selectedModule" type="primary" @click="startSession">
            <VideoPlay :size="17" />{{ session ? '重新开启训练' : '开始训练' }}
          </el-button>
        </el-form>
      </section>

      <section v-if="session" class="progress-card">
        <div class="progress-title"><span>本次进度</span><strong>{{ session.completedQuestionCount }}/{{ session.questionCount }}</strong></div>
        <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="7" />
        <p>{{ session.status === 'FINISHED' ? '训练已收束，报告已生成' : '保持节奏，先结论后推导' }}</p>
      </section>

      <p class="rail-tip">回答建议：结论 → 原理 → 场景 → 边界</p>
    </aside>

    <section class="training-stage">
      <header class="stage-header">
        <div><p class="eyebrow">AI INTERVIEW STUDIO</p><h1>{{ stageTitle }}</h1></div>
        <el-tag v-if="session" effect="dark" :type="session.status === 'FINISHED' ? 'success' : 'primary'">{{ session.status === 'FINISHED' ? '训练完成' : '进行中' }}</el-tag>
      </header>

      <section v-if="report" class="report-dashboard">
        <div class="report-hero">
          <div><p class="eyebrow">SESSION REVIEW</p><h2>这轮训练，已经沉淀为你的下一步。</h2><p>完成 {{ report.answeredCount }} 轮回答 · {{ report.scoreLevel }}</p></div>
          <div class="score-orbit"><strong>{{ report.totalScore }}</strong><span>综合得分</span></div>
        </div>
        <div class="report-grid">
          <article class="report-card positive"><h3><CircleCheck :size="18" />表现亮点</h3><ul><li v-for="item in report.strengths" :key="item">{{ item }}</li></ul></article>
          <article class="report-card warning"><h3><Warning :size="18" />需要补强</h3><ul><li v-for="item in report.weaknesses" :key="item">{{ item }}</li></ul></article>
          <article class="report-card action"><h3><Compass :size="18" />下一轮建议</h3><ul><li v-for="item in report.recommendations" :key="item">{{ item }}</li></ul></article>
        </div>
        <el-button type="primary" class="restart-button" :loading="sessionLoading" @click="startSession"><RefreshRight :size="17" />再练一轮</el-button>
      </section>

      <section v-else-if="activeQuestion" class="interview-workspace">
        <div class="question-card">
          <div class="question-meta"><el-tag effect="plain">{{ moduleNames[activeQuestion.module] || activeQuestion.module }}</el-tag><span>{{ activeQuestion.questionType === 'FOLLOW_UP' ? '追问 · 深入验证' : '主问题 · 思路展开' }}</span></div>
          <h2>{{ activeQuestion.questionText }}</h2>
          <div class="question-hint"><VideoPlay :size="16" />建议先用一句话给出结论，再拆解原理和真实业务场景。</div>
        </div>
        <div class="answer-card">
          <div class="answer-header"><div><p class="eyebrow">YOUR ANSWER</p><h3>组织你的面试回答</h3></div><span>{{ answerText.length }} 字</span></div>
          <el-input v-model="answerText" class="answer-input" :autosize="{ minRows: 8, maxRows: 15 }" maxlength="2000" placeholder="从结论开始。可以谈机制、取舍、真实项目案例和异常边界……" show-word-limit type="textarea" @keydown.ctrl.enter="submitAnswer" />
          <div class="answer-actions">
            <span>Ctrl + Enter 提交</span>
            <div><el-button :disabled="!session || session.status === 'FINISHED'" @click="finishSession">结束训练</el-button><el-button :disabled="!canSubmit" :loading="submitLoading" type="primary" @click="submitAnswer"><Check :size="16" />提交回答</el-button></div>
          </div>
        </div>

        <section v-if="evaluationText" class="feedback-panel">
          <div class="feedback-head"><div><p class="eyebrow">AI FEEDBACK</p><h3>本轮反馈</h3></div><strong v-if="feedbackScore !== null">{{ feedbackScore }}<small>分</small></strong></div>
          <div class="feedback-grid">
            <div v-if="hitPoints.length"><h4>命中要点</h4><span v-for="item in hitPoints" :key="item">{{ item }}</span></div>
            <div v-if="missingPoints.length"><h4>待补要点</h4><span v-for="item in missingPoints" :key="item">{{ item }}</span></div>
            <div v-if="weaknesses.length"><h4>表达风险</h4><span v-for="item in weaknesses" :key="item">{{ item }}</span></div>
          </div>
          <p class="feedback-summary">{{ evaluationText }}</p>
          <div class="feedback-next"><span>{{ nextActionText }}</span><el-button v-if="canGoNext" :loading="nextLoading" type="primary" @click="goNextQuestion">进入下一题<ArrowRight :size="16" /></el-button></div>
        </section>
      </section>

      <section v-else class="welcome-state">
        <div class="welcome-orb"><VideoPlay :size="36" /></div><p class="eyebrow">READY WHEN YOU ARE</p><h2>把“会背”训练成<br /><em>能讲清楚。</em></h2><p>从左侧选择模块和难度，开启一场带追问、评分与复盘的 Java 后端面试。</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check, CircleCheck, Compass, Connection, RefreshRight, VideoPlay, Warning } from '@element-plus/icons-vue'
import { getHealth } from '@/api/modules/health'
import { getQuestionModules } from '@/api/modules/questionCard'
import { createInterviewSession, finishInterviewSession, getInterviewReport, nextInterviewQuestion, submitInterviewAnswer, type CurrentQuestion, type InterviewReport, type InterviewSession, type SubmitAnswerResult } from '@/api/modules/interview'

const loading = ref(false); const moduleLoading = ref(false); const sessionLoading = ref(false); const submitLoading = ref(false); const nextLoading = ref(false)
const healthMessage = ref(''); const healthy = ref(false); const questionModules = ref<string[]>([]); const selectedModule = ref(''); const selectedDifficulty = ref(''); const questionCount = ref(5)
const session = ref<InterviewSession | null>(null); const activeQuestion = ref<CurrentQuestion | null>(null); const report = ref<InterviewReport | null>(null); const answerText = ref(''); const evaluationText = ref(''); const nextAction = ref<SubmitAnswerResult['nextAction'] | ''>(''); const feedbackScore = ref<number | null>(null); const hitPoints = ref<string[]>([]); const missingPoints = ref<string[]>([]); const weaknesses = ref<string[]>([])
const moduleNames: Record<string, string> = { Java: 'Java 核心', MySQL: 'MySQL', Redis: 'Redis', Spring: 'Spring', Network: '计算机网络', OperatingSystem: '操作系统' }
const stageTitle = computed(() => report.value ? '训练复盘报告' : session.value ? '专注于这一题' : '开始你的面试训练')
const progressPercent = computed(() => session.value ? Math.round(session.value.completedQuestionCount / session.value.questionCount * 100) : 0)
const canSubmit = computed(() => Boolean(session.value && session.value.status !== 'FINISHED' && answerText.value.trim()))
const canGoNext = computed(() => nextAction.value === 'NEXT_QUESTION' && session.value?.status !== 'FINISHED')
const nextActionText = computed(() => nextAction.value === 'ASK_FOLLOW_UP' ? 'AI 已发起追问' : nextAction.value === 'NEXT_QUESTION' ? '本题完成，可以继续' : nextAction.value === 'FINISH_SESSION' ? '训练结束，正在生成报告' : '')

async function checkHealth() { loading.value = true; try { const response = await getHealth(); healthy.value = true; healthMessage.value = response.data.data || '服务运行正常' } catch { healthy.value = false; healthMessage.value = '训练服务暂不可用' } finally { loading.value = false } }
async function loadModules() { moduleLoading.value = true; try { const response = await getQuestionModules(); questionModules.value = response.data.data; if (!selectedModule.value) selectedModule.value = questionModules.value[0] || '' } finally { moduleLoading.value = false } }
function clearFeedback() { evaluationText.value = ''; nextAction.value = ''; feedbackScore.value = null; hitPoints.value = []; missingPoints.value = []; weaknesses.value = [] }
function applySession(value: InterviewSession) { session.value = value; activeQuestion.value = value.currentQuestion ?? null }
async function startSession() { if (!selectedModule.value) return; sessionLoading.value = true; try { const response = await createInterviewSession({ module: selectedModule.value, difficulty: selectedDifficulty.value || undefined, questionCount: questionCount.value }); applySession(response.data.data); report.value = null; answerText.value = ''; clearFeedback() } finally { sessionLoading.value = false } }
async function loadReport() { if (!session.value) return; const response = await getInterviewReport(session.value.sessionId); report.value = response.data.data; activeQuestion.value = null }
async function submitAnswer() { if (!canSubmit.value || !session.value) return; submitLoading.value = true; try { const response = await submitInterviewAnswer(session.value.sessionId, { answerText: answerText.value.trim() }); const result = response.data.data; evaluationText.value = result.evaluationText; nextAction.value = result.nextAction; feedbackScore.value = typeof result.score === 'number' ? result.score : null; hitPoints.value = result.hitPoints ?? []; missingPoints.value = result.missingPoints ?? []; weaknesses.value = result.weaknesses ?? []; session.value.completedQuestionCount = result.completedQuestionCount; session.value.questionCount = result.questionCount; answerText.value = ''; if (result.currentQuestion) { activeQuestion.value = result.currentQuestion; return } activeQuestion.value = null; if (result.nextAction === 'FINISH_SESSION') { session.value.status = 'FINISHED'; await loadReport() } } finally { submitLoading.value = false } }
async function goNextQuestion() { if (!session.value) return; nextLoading.value = true; try { const response = await nextInterviewQuestion(session.value.sessionId); applySession(response.data.data); clearFeedback(); answerText.value = ''; if (session.value.status === 'FINISHED') await loadReport() } finally { nextLoading.value = false } }
async function finishSession() { if (!session.value) return; await finishInterviewSession(session.value.sessionId); session.value.status = 'FINISHED'; await loadReport() }
onMounted(() => { checkHealth(); loadModules() })
</script>
