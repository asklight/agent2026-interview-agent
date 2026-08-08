<template>
  <main class="page-frame algorithm-report-page">
    <header class="workspace-heading">
      <div><p class="page-kicker">ALGORITHM REVIEW</p><h1>算法口述复盘</h1></div>
      <RouterLink class="secondary-link" to="/practice/algorithm">继续选题</RouterLink>
    </header>
    <section v-if="loading" class="algorithm-library__state">正在生成复盘...</section>
    <section v-else-if="error" class="algorithm-library__state"><p>{{ error }}</p><el-button @click="load">重新加载</el-button></section>
    <template v-else-if="report">
      <section class="algorithm-report-summary">
        <div><span>{{ report.completionStatus === 'COMPLETE' ? '完整完成' : '提前结束' }}</span><h2>{{ scoreText }}</h2><small>综合表现</small></div>
        <div><h2>{{ report.coverage }}%</h2><small>能力覆盖</small></div>
        <p>分数只用于结束后的复盘。未在对话中实际涉及的能力维度不会计为零分。</p>
      </section>
      <section class="report-section">
        <div class="report-section__heading"><p class="page-kicker">DIMENSIONS</p><h2>能力维度</h2></div>
        <div class="algorithm-dimension-list">
          <article v-for="dimension in report.dimensions" :key="dimension.dimension">
            <span>{{ dimensionName(dimension.dimension) }}</span>
            <strong>{{ dimension.score ?? '未评估' }}</strong>
            <i><b :style="{ width: `${dimension.score ?? 0}%` }"></b></i>
          </article>
        </div>
      </section>
      <section class="report-section algorithm-review-columns">
        <article><h2>做得好的地方</h2><p v-if="!report.strengths.length">暂无足够证据</p><div v-for="item in report.strengths" :key="`${item.evaluationId}-${item.text}`"><strong>{{ item.text }}</strong><small>“{{ item.candidateEvidence }}”</small></div></article>
        <article><h2>需要补强</h2><p v-if="!report.gaps.length">暂无明显缺口</p><div v-for="item in report.gaps" :key="`${item.evaluationId}-${item.text}`"><strong>{{ item.text }}</strong><small>“{{ item.candidateEvidence }}”</small></div></article>
        <article><h2>下一步练习</h2><p v-for="item in report.recommendations" :key="item">{{ item }}</p></article>
      </section>
      <section class="report-section">
        <div class="report-section__heading"><p class="page-kicker">ROUND BY ROUND</p><h2>逐轮回看</h2></div>
        <div class="algorithm-round-list">
          <article v-for="round in report.rounds" :key="round.evaluationId">
            <header><span>{{ round.sequence.toString().padStart(2, '0') }}</span><strong>{{ stageName(round.stage) }}</strong></header>
            <blockquote>{{ round.candidateAnswer }}</blockquote>
            <div v-if="round.strengths.length"><b>有效表达</b><span v-for="item in round.strengths" :key="item">{{ item }}</span></div>
            <div v-if="round.gaps.length"><b>可改进</b><span v-for="item in round.gaps" :key="item">{{ item }}</span></div>
          </article>
        </div>
      </section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getAlgorithmReport } from '../api/algorithmApi'
import type { AlgorithmReport } from '../model/types'

const props = defineProps<{ sessionId: string }>()
const report = ref<AlgorithmReport | null>(null)
const loading = ref(false)
const error = ref('')
const scoreText = computed(() => report.value?.overallScore == null ? '未形成评分' : `${report.value.overallScore} 分`)
onMounted(load)
async function load() {
  loading.value = true; error.value = ''
  try { report.value = (await getAlgorithmReport(Number(props.sessionId))).data.data }
  catch { error.value = '复盘暂时无法读取，请稍后重试。' }
  finally { loading.value = false }
}
function dimensionName(value: string) { return ({ correctness: '正确性', optimization: '优化意识', complexity: '复杂度分析', edgeCases: '边界处理', communication: '表达沟通' } as Record<string, string>)[value] ?? value }
function stageName(value: string) { return ({ CLARIFY: '澄清题意', BASELINE_SOLUTION: '基础方案', OPTIMIZATION: '优化方案', COMPLEXITY: '复杂度', EDGE_CASE: '边界条件', FOLLOW_UP: '变体追问' } as Record<string, string>)[value] ?? value }
</script>
