<template>
  <section class="extraction-review">
    <div class="section-heading">
      <div><p class="page-kicker">STEP 02 · REVIEW</p><h2>{{ readonly ? '已确认的项目档案' : '确认系统理解得对不对' }}</h2></div>
      <el-tag :type="readonly ? 'success' : 'warning'" effect="plain">{{ readonly ? '面试将使用以下内容' : '确认后再开始面试' }}</el-tag>
    </div>
    <p class="section-intro">{{ readonly ? '档案已经锁定为本次面试的事实起点。如需修改，请重新创建项目档案。' : '这里不是最终评分。请修正不准确的信息，面试官会以确认后的内容作为追问起点。' }}</p>

    <div class="review-grid">
      <el-form label-position="top" class="review-main-form">
        <el-form-item label="项目名称"><el-input v-model="form.projectName" maxlength="255" :disabled="readonly" /></el-form-item>
        <el-form-item label="项目概述"><el-input v-model="form.summary" type="textarea" :rows="4" maxlength="4000" :disabled="readonly" /></el-form-item>
        <el-form-item v-for="field in listFields" :key="field.key" :label="field.label">
          <el-select v-model="form[field.key]" multiple filterable allow-create default-first-option :placeholder="field.placeholder" :disabled="readonly">
            <el-option v-for="item in form[field.key]" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <aside class="review-source">
        <p class="page-kicker">SANITIZED SOURCE</p>
        <h3>脱敏后的项目原文</h3>
        <p>{{ sourceDescription }}</p>
      </aside>
    </div>

    <div class="claims-heading"><div><p class="page-kicker">CLAIMS</p><h3>需要在面试中验证的项目声明</h3></div><span>{{ form.claims.length }} 条</span></div>
    <div class="claim-list">
      <ProjectClaimCard v-for="(claim, index) in form.claims" :key="claim.claimId" :claim="claim" :index="index" :readonly="readonly" />
    </div>
  </section>
</template>

<script setup lang="ts">
import ProjectClaimCard from './ProjectClaimCard.vue'
import type { ProjectReviewForm } from '@/features/project-deep-dive/model/types'

withDefaults(defineProps<{ form: ProjectReviewForm; sourceDescription: string; readonly?: boolean }>(), { readonly: false })

const listFields: Array<{
  key: 'techStack' | 'responsibilities' | 'metrics' | 'architecture' | 'uncertainties'
  label: string
  placeholder: string
}> = [
  { key: 'techStack', label: '技术栈', placeholder: '例如 Spring Boot、Redis' },
  { key: 'responsibilities', label: '个人职责', placeholder: '输入职责后回车' },
  { key: 'metrics', label: '关键指标', placeholder: '只保留真实、可解释的指标' },
  { key: 'architecture', label: '架构信息', placeholder: '例如核心链路、上下游系统' },
  { key: 'uncertainties', label: '待确认项', placeholder: '系统不确定、面试中需要澄清的内容' },
]
</script>
