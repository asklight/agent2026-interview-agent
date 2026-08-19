<template>
  <main class="home-page">
    <header class="workspace-heading">
      <div><p class="page-kicker">INTERVIEW WORKBENCH</p><h1>今天练什么？</h1></div>
      <p>八股用来快速校准知识，项目深挖用来练清楚真实经历。两种训练，各自保持合适的重量。</p>
    </header>

    <section v-if="showRecommendation && primaryRecommendation" class="agent-recommendation" aria-label="个性化训练推荐">
      <div class="agent-recommendation__copy">
        <p class="page-kicker">PERSONAL TRAINING AGENT</p>
        <h2>今天最值得练：{{ primaryRecommendation.title }}</h2>
        <span>{{ primaryRecommendation.reason }}</span>
        <small>预计 {{ primaryRecommendation.estimatedMinutes }} 分钟 · 根据最近训练表现生成</small>
      </div>
      <RouterLink class="primary-link" :to="recommendationTarget(primaryRecommendation)">查看训练配置 <ArrowRight /></RouterLink>
    </section>
    <section v-else-if="agentState === 'COLD_START'" class="agent-recommendation agent-recommendation--cold" aria-label="开始训练提示">
      <div class="agent-recommendation__copy">
        <p class="page-kicker">PERSONAL TRAINING AGENT</p>
        <h2>先做一次基础校准</h2>
        <span>完成第一轮训练后，系统会根据你的真实表现安排下一步。</span>
      </div>
      <RouterLink class="primary-link" :to="coldStartTarget">设置校准训练 <ArrowRight /></RouterLink>
    </section>
    <p v-if="agentState === 'DEGRADED' || agentState === 'FAILED'" class="agent-degraded" role="status">
      {{ degradedMessage }}
    </p>

    <section v-if="showOverview" class="agent-overview" aria-label="训练概览">
      <div class="agent-overview__section">
        <p class="page-kicker">CURRENT FOCUS</p>
        <h2>当前训练重点</h2>
        <ul v-if="focusDimensions.length" class="agent-focus-list">
          <li v-for="item in focusDimensions" :key="item.dimensionCode">
            <strong>{{ item.label }}</strong><span>{{ abilityStateLabel(item.abilityState) }}</span>
          </li>
        </ul>
        <p v-else class="agent-overview__empty">继续完成一轮训练后，这里会沉淀新的训练重点。</p>
      </div>

      <div class="agent-overview__section">
        <p class="page-kicker">RECENT PROGRESS</p>
        <h2>最近进展</h2>
        <div v-if="dashboard?.recentProgress" class="agent-progress">
          <strong>{{ dashboard.recentProgress.label }}</strong>
          <p>{{ progressMessage(dashboard.recentProgress.label, dashboard.recentProgress.abilityState) }}</p>
        </div>
        <p v-else class="agent-overview__empty">还没有形成可展示的近期进展，完成下一轮训练后会自动更新。</p>
      </div>

      <div v-if="alternativeRecommendations.length" class="agent-overview__section agent-overview__section--alternatives">
        <p class="page-kicker">OTHER OPTIONS</p>
        <h2>也可以练</h2>
        <RouterLink v-for="item in alternativeRecommendations" :key="`${item.trainingType}:${item.dimensionCode}`"
          class="agent-alternative" :to="recommendationTarget(item)">
          <span><strong>{{ item.title }}</strong><small>约 {{ item.estimatedMinutes }} 分钟</small></span><ArrowRight />
        </RouterLink>
      </div>
    </section>

    <section class="training-entry-list" aria-label="训练模块">
      <RouterLink class="training-entry" to="/practice/knowledge">
        <span class="training-entry__icon training-entry__icon--knowledge"><Collection /></span>
        <div class="training-entry__body">
          <p>KNOWLEDGE PRACTICE</p><h2>八股练习</h2>
          <span>选一个知识模块，连续完成 3 到 10 道题。每题即时反馈，适合热身和查漏补缺。</span>
        </div>
        <div class="training-entry__meta"><span>轻量 · 即时反馈</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry" to="/project-deep-dive">
        <span class="training-entry__icon training-entry__icon--project"><Briefcase /></span>
        <div class="training-entry__body">
          <p>PROJECT DEEP DIVE</p><h2>项目深挖</h2>
          <span>从你的项目事实出发，接受连续追问。面试过程不展示评分，结束后按证据复盘。</span>
        </div>
        <div class="training-entry__meta"><span>沉浸 · 连续追问</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry" to="/practice/algorithm">
        <span class="training-entry__icon training-entry__icon--algorithm"><DataAnalysis /></span>
        <div class="training-entry__body">
          <p>ALGORITHM ORAL PRACTICE</p><h2>算法口述</h2>
          <span>按真实面试节奏讲清题意、方案、优化、复杂度和边界。过程不打断，结束后按原话证据复盘。</span>
        </div>
        <div class="training-entry__meta"><span>专项 · 连续追问</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry training-entry--simulation" to="/simulation/new">
        <span class="training-entry__icon training-entry__icon--simulation"><VideoCamera /></span>
        <div class="training-entry__body">
          <p>FULL INTERVIEW SIMULATION</p><h2>综合模拟</h2>
          <span>把项目深挖、基础问答和算法口述连成一场完整技术面试。过程保持沉浸，结束后统一复盘。</span>
        </div>
        <div class="training-entry__meta"><span>综合 · 面试节奏</span><ArrowRight /></div>
      </RouterLink>
    </section>

    <section class="home-path" aria-label="训练路径">
      <div><span>01</span><strong>知识校准</strong><small>先确认基础概念能讲清楚</small></div>
      <i></i><div><span>02</span><strong>项目表达</strong><small>再把真实经历讲出证据</small></div>
      <i></i><div><span>03</span><strong>复盘改进</strong><small>最后只看可执行的下一步</small></div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, Briefcase, Collection, DataAnalysis, VideoCamera } from '@element-plus/icons-vue'
import { getTrainingAgentDashboard, type TrainingAgentDashboard } from '@/features/training-agent/api/trainingAgentApi'
import { coldStartTarget, recommendationTarget } from '@/features/training-agent/model/recommendationNavigation'

const dashboard = ref<TrainingAgentDashboard | null>(null)
const dashboardLoading = ref(true)
const requestFailed = ref(false)

const agentState = computed(() => {
  if (dashboardLoading.value) return 'LOADING'
  if (requestFailed.value || !dashboard.value) return 'FAILED'
  if (!dashboard.value.enabled || dashboard.value.state === 'DISABLED') return 'DISABLED'
  if (dashboard.value.degraded || dashboard.value.state === 'DEGRADED') return 'DEGRADED'
  if (dashboard.value.state === 'COLD_START') return 'COLD_START'
  if (dashboard.value.state === 'READY') return dashboard.value.primaryRecommendation ? 'READY' : 'DEGRADED'
  return 'FAILED'
})
const primaryRecommendation = computed(() => dashboard.value?.primaryRecommendation ?? null)
const showRecommendation = computed(() => Boolean(primaryRecommendation.value
  && (agentState.value === 'READY' || agentState.value === 'DEGRADED')))
const showOverview = computed(() => showRecommendation.value)
const focusDimensions = computed(() => (dashboard.value?.focusDimensions ?? []).slice(0, 3))
const alternativeRecommendations = computed(() => {
  const primaryType = primaryRecommendation.value?.trainingType
  return (dashboard.value?.alternatives ?? [])
    .filter(item => item.trainingType !== primaryType)
    .slice(0, 2)
})
const degradedMessage = computed(() => {
  if (requestFailed.value) return '个性化推荐暂时没有连接上，四个训练模块仍可正常使用。'
  if (primaryRecommendation.value) return '推荐同步暂时延迟，当前展示最近一次可用建议。'
  return '个性化推荐暂时不可用，四个训练模块仍可正常使用。'
})

function abilityStateLabel(state: string) {
  return {
    NEEDS_WORK: '优先补强',
    DEVELOPING: '继续发展',
    STABLE: '保持状态',
    STRONG: '近期表现稳定',
    UNKNOWN: '等待校准',
  }[state] ?? '持续观察'
}

function progressMessage(label: string, state: string) {
  if (state === 'STRONG') return `“${label}”在近期训练中表现稳定，可以继续保持。`
  if (state === 'STABLE') return `“${label}”已经形成较稳定的表现。`
  if (state === 'DEVELOPING') return `“${label}”正在形成更完整的回答结构。`
  if (state === 'NEEDS_WORK') return `已经识别出“${label}”的明确改进方向。`
  return `“${label}”已经获得新的训练记录。`
}

onMounted(async () => {
  try {
    dashboard.value = (await getTrainingAgentDashboard()).data.data
  } catch {
    requestFailed.value = true
  } finally {
    dashboardLoading.value = false
  }
})
</script>
