<template>
  <article class="turn-review">
    <header><span>第 {{ review.sequenceNo }} 轮 · 回答 #{{ review.candidateTurnId }}</span><strong>{{ dimensionLabel }}</strong></header>
    <p><b>你的回答：</b>{{ review.candidateAnswer }}</p>
    <div v-if="review.hitPoints?.length"><b>有效表达</b><span v-for="item in review.hitPoints" :key="item">{{ item }}</span></div>
    <div v-if="review.missingPoints?.length"><b>可以补强</b><span v-for="item in review.missingPoints" :key="item">{{ item }}</span></div>
    <div v-if="review.riskFlags?.length"><b>需要验证</b><span v-for="item in review.riskFlags" :key="item">{{ item }}</span></div>
    <footer>证据引用：Claim #{{ review.claimId }} · Evaluation #{{ review.evaluationId }}</footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TurnReviewItem } from '@/features/project-deep-dive/model/types'
const props = defineProps<{ review: TurnReviewItem }>()
const labels: Record<string, string> = {
  OWNERSHIP: '个人职责', AUTHENTICITY: '项目真实性', PRINCIPLE: '技术原理', TRADEOFF: '方案取舍',
}
const dimensionLabel = computed(() => labels[props.review.dimension] || props.review.dimension || '逐轮复盘')
</script>
