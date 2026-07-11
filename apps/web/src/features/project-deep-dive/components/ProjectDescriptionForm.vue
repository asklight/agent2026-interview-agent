<template>
  <section class="project-input-card">
    <div class="section-heading">
      <div><p class="page-kicker">STEP 01 · PROJECT CONTEXT</p><h2>先把项目原样讲给我听</h2></div>
      <span>{{ modelValue.length }}/20000</span>
    </div>
    <p class="section-intro">可以直接粘贴简历中的项目经历，也可以补充你真实负责的工作。请先删除手机号、邮箱和公司敏感数据。</p>
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="13"
      maxlength="20000"
      resize="none"
      placeholder="例如：负责订单系统核心接口，使用 Redis 缓存热点数据，将接口 P95 从 420ms 降到 180ms……"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <div class="project-input-card__footer">
      <span>草稿仅保存在当前浏览器会话中</span>
      <el-button type="primary" size="large" :loading="loading" :disabled="modelValue.trim().length < 20" @click="$emit('analyze')">
        分析项目经历
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{ modelValue: string; loading?: boolean }>()
defineEmits<{ 'update:modelValue': [value: string]; analyze: [] }>()
</script>
