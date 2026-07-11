<template>
  <main class="project-report-page page-frame">
    <header class="page-heading report-page-heading">
      <RouterLink to="/">← 返回首页</RouterLink>
      <div><p class="page-kicker">PROJECT INTERVIEW REVIEW</p><h1>项目面试复盘</h1></div>
      <RouterLink class="secondary-link" to="/project-deep-dive">再练一个项目</RouterLink>
    </header>

    <section v-if="!accessToken" class="access-lost-state"><h2>无法读取这份报告</h2><p>当前浏览器会话中没有资源访问令牌。</p></section>
    <section v-else-if="loading" class="analysis-state"><span class="analysis-state__orb"></span><h2>正在生成基于回答证据的复盘</h2><p>报告只会引用这场面试中真实出现的回答。</p></section>
    <section v-else-if="!report" class="access-lost-state"><h2>报告暂时还没有准备好</h2><p>面试记录已经保存。可以稍后刷新，或先返回项目深挖首页。</p><el-button @click="loadReport">重新获取</el-button></section>

    <template v-else>
      <section class="project-report-hero">
        <div><p class="page-kicker">OVERALL</p><h2>这场项目面试已经沉淀为可追溯的改进方向。</h2><p>完成 {{ report.rounds.length }} 轮回答 · 维度覆盖率 {{ report.coverageRate }}%</p></div>
        <div v-if="typeof report.totalScore === 'number'" class="score-orbit"><strong>{{ report.totalScore }}</strong><span>综合得分</span></div>
      </section>

      <section v-if="dimensions.length" class="report-section"><div class="report-section__heading"><p class="page-kicker">DIMENSIONS</p><h2>能力维度</h2></div><div class="dimension-grid"><ReportDimensionCard v-for="item in dimensions" :key="item.dimension" :dimension="item" /></div></section>

      <section v-if="report.claimReviews.length" class="report-section"><div class="report-section__heading"><p class="page-kicker">CLAIMS</p><h2>项目声明复盘</h2></div><div class="claim-review-list"><article v-for="claim in report.claimReviews" :key="claim.claimId"><span>Claim #{{ claim.claimId }}</span><h3>围绕这条项目声明的证据汇总</h3><div><b v-for="item in claim.strengths" :key="keyOf(item)">{{ item.text }}</b><b v-for="item in [...claim.weaknesses, ...claim.risks]" :key="keyOf(item)">{{ item.text }}</b></div><p v-for="item in claim.recommendations" :key="keyOf(item)">{{ item.text }}</p></article></div></section>

      <section v-if="report.rounds.length" class="report-section"><div class="report-section__heading"><p class="page-kicker">EVIDENCE</p><h2>逐轮回答证据</h2></div><div class="turn-review-list"><TurnReview v-for="item in report.rounds" :key="item.candidateTurnId" :review="item" /></div></section>

      <section class="report-summary-grid">
        <article><h3>表现亮点</h3><p v-for="item in report.strengths" :key="keyOf(item)"><span>{{ item.text }}</span><small>{{ evidenceOf(item) }}</small></p></article>
        <article><h3>需要补强</h3><p v-for="item in [...report.weaknesses, ...report.risks]" :key="keyOf(item)"><span>{{ item.text }}</span><small>{{ evidenceOf(item) }}</small></p></article>
        <article><h3>下一轮建议</h3><p v-for="item in report.recommendations" :key="keyOf(item)"><span>{{ item.text }}</span><small>{{ evidenceOf(item) }}</small></p></article>
      </section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import ReportDimensionCard from '@/features/project-deep-dive/components/ReportDimensionCard.vue'
import TurnReview from '@/features/project-deep-dive/components/TurnReview.vue'
import { getProjectInterviewReport } from '@/features/project-deep-dive/api/interviewApi'
import type { ProjectInterviewReport, ReportConclusion, ReportDimension } from '@/features/project-deep-dive/model/types'
import { useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

const props = defineProps<{ sessionId: string }>()
const sessionId = Number(props.sessionId)
const store = useInterviewSessionStore()
const accessToken = computed(() => store.accessToken || store.restoreSessionAccess(sessionId)?.accessToken || '')
const report = ref<ProjectInterviewReport | null>(null)
const loading = ref(false)
const dimensions = computed<ReportDimension[]>(() => report.value?.dimensions || [])
const keyOf = (item: ReportConclusion) => `${item.evaluationId}-${item.candidateTurnId}-${item.text}`
const evidenceOf = (item: ReportConclusion) => `Claim #${item.claimId} · 回答 #${item.candidateTurnId} · 评价 #${item.evaluationId}`

async function loadReport() {
  if (!accessToken.value) return
  loading.value = true
  try {
    const response = await getProjectInterviewReport(sessionId, accessToken.value)
    report.value = response.data.data
  } catch {
    report.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>
