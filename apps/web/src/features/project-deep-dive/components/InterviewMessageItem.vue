<template>
  <article class="interview-message" :class="`interview-message--${turn.role.toLowerCase()}`">
    <div class="interview-message__avatar">{{ avatar }}</div>
    <div class="interview-message__body">
      <div class="interview-message__meta"><strong>{{ roleLabel }}</strong><time>{{ formattedTime }}</time></div>
      <p>{{ turn.content }}</p>
      <span v-if="turn.inputModality === 'VOICE_TRANSCRIPT'" class="message-modality">语音转写</span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PublicInterviewTurn } from '@/features/project-deep-dive/model/types'

const props = defineProps<{ turn: PublicInterviewTurn }>()
const roleLabel = computed(() => props.turn.role === 'INTERVIEWER' ? '面试官' : props.turn.role === 'CANDIDATE' ? '我' : '系统提示')
const avatar = computed(() => props.turn.role === 'INTERVIEWER' ? 'AI' : props.turn.role === 'CANDIDATE' ? '我' : '·')
const formattedTime = computed(() => {
  const value = props.turn.createTime
  if (!value) return ''
  return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
})
</script>
