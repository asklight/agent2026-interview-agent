<template>
  <main class="home-page">
    <header class="workspace-heading">
      <div><p class="page-kicker">INTERVIEW WORKBENCH</p><h1>今天练什么？</h1></div>
      <p>八股用来快速校准知识，项目深挖用来练清楚真实经历。两种训练，各自保持合适的重量。</p>
    </header>

    <section v-if="dashboard?.primary" class="agent-recommendation" aria-label="个性化训练推荐">
      <div class="agent-recommendation__copy">
        <p class="page-kicker">PERSONAL TRAINING AGENT</p>
        <h2>今天最值得练：{{ dashboard.primary.title }}</h2>
        <span>{{ dashboard.primary.reason }}</span>
        <small>预计 {{ dashboard.primary.estimatedMinutes }} 分钟 · 根据最近训练证据生成</small>
        <div v-if="dashboard.focus.length" class="agent-focus">
          <span>当前重点</span>
          <em v-for="item in dashboard.focus" :key="item.dimensionCode">{{ item.label }}</em>
        </div>
      </div>
      <RouterLink class="primary-link" :to="primaryPath">开始训练 <ArrowRight /></RouterLink>
    </section>
    <section v-else-if="dashboard?.state === 'COLD_START'" class="agent-recommendation agent-recommendation--cold" aria-label="开始训练提示">
      <div class="agent-recommendation__copy">
        <p class="page-kicker">PERSONAL TRAINING AGENT</p>
        <h2>先做一次基础校准</h2>
        <span>完成第一轮训练后，系统会根据你的真实表现安排下一步。</span>
      </div>
      <RouterLink class="primary-link" to="/practice/knowledge">开始校准 <ArrowRight /></RouterLink>
    </section>
    <p v-if="dashboard?.degraded" class="agent-degraded">个性化推荐暂时不可用，四个训练模块仍可正常使用。</p>

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

const dashboard = ref<TrainingAgentDashboard | null>(null)
const primaryPath = computed(() => {
  const type = dashboard.value?.primary?.trainingType
  if (type === 'PROJECT_DEEP_DIVE') return '/project-deep-dive'
  if (type === 'ALGORITHM') return '/practice/algorithm'
  if (type === 'COMPREHENSIVE_SIMULATION') return '/simulation/new'
  return '/practice/knowledge'
})

onMounted(async () => {
  try {
    dashboard.value = (await getTrainingAgentDashboard()).data.data
  } catch {
    // 首页不因推荐服务故障而阻塞四个训练入口。
  }
})
</script>
