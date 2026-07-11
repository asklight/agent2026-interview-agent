<template>
  <section class="interview-composer">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="5"
      maxlength="5000"
      resize="none"
      :disabled="disabled"
      placeholder="像真实面试一样回答：先说结论，再说明你的做法、依据和取舍……"
      @update:model-value="$emit('update:modelValue', $event)"
      @keydown.ctrl.enter.prevent="$emit('submit')"
    />
    <div class="interview-composer__actions">
      <div><VoiceControl /><span>当前为文字面试 · Ctrl + Enter 提交</span></div>
      <el-button type="primary" :loading="submitting" :disabled="disabled || !modelValue.trim()" @click="$emit('submit')">发送回答</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import VoiceControl from './VoiceControl.vue'

defineProps<{ modelValue: string; submitting?: boolean; disabled?: boolean }>()
defineEmits<{ 'update:modelValue': [value: string]; submit: [] }>()
</script>
