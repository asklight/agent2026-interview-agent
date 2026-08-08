<template>
  <el-tooltip content="语音输入尚未开放" placement="top">
    <button class="voice-control" type="button" disabled aria-label="语音输入暂未开放">
      <el-icon><Microphone /></el-icon>
    </button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { Microphone } from '@element-plus/icons-vue'
</script>
