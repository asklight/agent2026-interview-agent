<template>
  <div ref="container" class="interview-message-list" aria-live="polite">
    <InterviewMessageItem v-for="turn in turns" :key="turn.turnId" :turn="turn" />
    <article v-if="thinking" class="interview-message interview-message--interviewer interview-message--thinking">
      <div class="interview-message__avatar">AI</div>
      <div class="interview-message__body"><strong>面试官正在思考</strong><span class="thinking-dots"><i></i><i></i><i></i></span></div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import InterviewMessageItem from './InterviewMessageItem.vue'
import type { PublicInterviewTurn } from '@/features/project-deep-dive/model/types'

const props = defineProps<{ turns: PublicInterviewTurn[]; thinking?: boolean }>()
const container = ref<HTMLElement | null>(null)

watch(() => [props.turns.length, props.thinking], async () => {
  await nextTick()
  if (container.value) container.value.scrollTop = container.value.scrollHeight
}, { immediate: true })
</script>
