<template>
  <article class="dimension-card">
    <div><span>{{ label }}</span><strong>{{ dimension.score === null ? '未覆盖' : dimension.score }}</strong></div>
    <el-progress v-if="dimension.score !== null" :percentage="dimension.score" :show-text="false" :stroke-width="7" />
    <p>{{ dimension.score === null ? '本轮面试没有覆盖该维度，不按零分计算。' : '结合本轮回答证据确定性聚合，不展示模型原始判断。' }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ReportDimension } from '@/features/project-deep-dive/model/types'

const props = defineProps<{ dimension: ReportDimension }>()
const labels: Record<string, string> = {
  AUTHENTICITY: '项目真实性', OWNERSHIP: '个人职责', PRINCIPLE: '技术深度', TRADEOFF: '方案取舍',
  METRIC: '指标可信度', INCIDENT: '故障意识', SCALE: '扩展判断', COMMUNICATION: '沟通表达',
  authenticity: '项目真实性', ownership: '个人职责', technicalDepth: '技术深度',
  tradeoffReasoning: '方案取舍', engineeringAwareness: '工程意识', communication: '沟通表达',
}
const label = computed(() => labels[props.dimension.dimension] || props.dimension.dimension)
</script>
