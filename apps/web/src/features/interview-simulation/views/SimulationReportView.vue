<template>
  <main class="simulation-report-page">
    <header class="simulation-report-header"><RouterLink to="/history">← 返回训练历史</RouterLink><div><p class="page-kicker">INTERVIEW REVIEW</p><h1>综合模拟复盘</h1><p>{{ report?.completionStatus === 'COMPLETE' ? '三个阶段均已完成，下面的结论都来自本场真实回答。' : '本场提前结束，只复盘已经完成的部分；未覆盖内容不计为失分。' }}</p></div><RouterLink class="primary-link" to="/simulation/new">再模拟一场</RouterLink></header>
    <section v-if="loading" class="simulation-options-state">正在整理本场证据...</section>
    <section v-else-if="error" class="simulation-options-state"><p>{{ error }}</p><el-button @click="load">重新加载</el-button></section>
    <template v-else-if="report">
      <section class="simulation-report-summary"><span :class="report.completionStatus.toLowerCase()">{{ report.completionStatus === 'COMPLETE' ? '完整完成' : '部分完成' }}</span><div><strong>{{ completedCount }}/3 个阶段形成复盘</strong><small>{{ formatTime(report.generatedAt) }}</small></div></section>
      <section class="simulation-report-stages">
        <article v-for="stage in report.stages" :key="stage.stageType">
          <header><span>{{ stageIndex(stage.stageType) }}</span><div><p>{{ stageName(stage.stageType) }}</p><h2>{{ stage.report ? stageHeadline(stage) : '本场未覆盖' }}</h2></div><i :class="stage.report ? 'ready' : 'missing'">{{ stage.report ? '已复盘' : '未评估' }}</i></header>
          <div v-if="stage.report" class="simulation-report-evidence">
            <template v-if="stage.stageType === 'PROJECT'">
              <p><strong>覆盖度</strong>{{ percent((stage.report as any).coverageRate) }}</p><ul><li v-for="item in takeTexts((stage.report as any).strengths)" :key="item">{{ item }}</li><li v-for="item in takeTexts((stage.report as any).recommendations)" :key="item">下一步：{{ item }}</li></ul>
            </template>
            <template v-else-if="stage.stageType === 'KNOWLEDGE'">
              <p><strong>完成回答</strong>{{ (stage.report as any).answeredCount }} 轮</p><ul><li v-for="item in takeStrings((stage.report as any).strengths)" :key="item">{{ item }}</li><li v-for="item in takeStrings((stage.report as any).recommendations)" :key="item">下一步：{{ item }}</li></ul>
            </template>
            <template v-else>
              <p><strong>覆盖度</strong>{{ percent((stage.report as any).coverage) }}</p><ul><li v-for="item in takeTexts((stage.report as any).strengths)" :key="item">{{ item }}</li><li v-for="item in takeStrings((stage.report as any).recommendations)" :key="item">下一步：{{ item }}</li></ul>
            </template>
          </div>
          <p v-else class="simulation-report-missing">没有足够的真实回答证据，因此不生成推测性结论。</p>
        </article>
      </section>
      <section class="simulation-next-actions"><p class="page-kicker">NEXT ROUND</p><h2>下一轮训练顺序</h2><ol><li v-for="(item, index) in report.recommendations" :key="item"><span>0{{ index + 1 }}</span>{{ item }}</li></ol></section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getSimulationReport } from '../api/simulationApi'
import type { SimulationReport, SimulationStageReport } from '../model/types'
const props = defineProps<{ sessionId: string }>(); const id = Number(props.sessionId)
const report = ref<SimulationReport | null>(null); const loading = ref(false); const error = ref('')
const completedCount = computed(() => report.value?.stages.filter(stage => stage.report).length ?? 0)
onMounted(load)
async function load() { loading.value = true; error.value = ''; try { report.value = (await getSimulationReport(id)).data.data } catch { error.value = '复盘报告暂时无法读取，请稍后重试。' } finally { loading.value = false } }
function stageName(value: string) { return ({ PROJECT: '项目交流', KNOWLEDGE: '基础问答', ALGORITHM: '算法讨论' } as Record<string, string>)[value] ?? value }
function stageIndex(value: string) { return ({ PROJECT: '01', KNOWLEDGE: '02', ALGORITHM: '03' } as Record<string, string>)[value] ?? '00' }
function stageHeadline(stage: SimulationStageReport) { return ({ PROJECT: '项目表达与证据', KNOWLEDGE: '核心知识掌握', ALGORITHM: '思路与复杂度意识' } as Record<string, string>)[stage.stageType] }
function percent(value: number | null | undefined) { if (value == null) return '未统计'; return `${Math.round(value <= 1 ? value * 100 : value)}%` }
function takeStrings(value: unknown) { return Array.isArray(value) ? value.filter(item => typeof item === 'string').slice(0, 3) : [] }
function takeTexts(value: unknown) { return Array.isArray(value) ? value.map(item => typeof item === 'string' ? item : item?.text).filter(Boolean).slice(0, 3) : [] }
function formatTime(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) }
</script>
