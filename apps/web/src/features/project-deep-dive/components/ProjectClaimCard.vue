<template>
  <article class="claim-card">
    <div class="claim-card__head">
      <span>{{ index + 1 }}</span>
      <div><strong>{{ claimTypeLabel[claim.claimType] }}</strong><small>面试中会围绕这条声明验证证据</small></div>
    </div>
    <el-form label-position="top">
      <el-form-item label="项目声明">
        <el-input v-model="claim.statement" type="textarea" :rows="2" maxlength="2000" :disabled="readonly" />
      </el-form-item>
      <el-form-item label="原文依据">
        <el-input v-model="claim.sourceFragment" type="textarea" :rows="2" maxlength="2000" :disabled="readonly" />
      </el-form-item>
      <el-form-item label="关联技术">
        <el-select v-model="claim.relatedTechnologies" multiple filterable allow-create default-first-option placeholder="输入后回车添加" :disabled="readonly">
          <el-option v-for="item in claim.relatedTechnologies" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
    </el-form>
  </article>
</template>

<script setup lang="ts">
import type { PatchProjectClaim, ProjectClaimType } from '@/features/project-deep-dive/model/types'

withDefaults(defineProps<{ claim: PatchProjectClaim; index: number; readonly?: boolean }>(), { readonly: false })

const claimTypeLabel: Record<ProjectClaimType, string> = {
  RESPONSIBILITY: '个人职责',
  TECHNICAL_CHOICE: '技术选型',
  PERFORMANCE_IMPROVEMENT: '性能指标',
  ARCHITECTURE_DESIGN: '架构设计',
  INCIDENT_HANDLING: '故障处理',
  BUSINESS_RESULT: '业务结果',
}
</script>
