<template>
  <main class="project-landing page-frame">
    <header class="workspace-heading project-landing__heading">
      <div><p class="page-kicker">PROJECT DEEP DIVE</p><h1>项目深挖</h1></div>
      <p>把一段真实项目经历整理成可验证的事实，再进入连续追问。这里不是知识测验，而是一场完整面试。</p>
    </header>

    <section class="project-start-band">
      <div>
        <span class="project-start-band__icon"><Briefcase /></span>
        <div><strong>{{ hasDraft ? '上次的项目草稿还在' : '准备一段真实项目经历' }}</strong><p>{{ hasDraft ? '可以直接从本浏览器保存的内容继续。' : '建议包含背景、职责、方案、指标和一次具体难点。' }}</p></div>
      </div>
      <RouterLink class="primary-link" to="/project-deep-dive/new">{{ hasDraft ? '继续准备' : '开始准备' }}<ArrowRight /></RouterLink>
    </section>

    <section class="deep-dive-workflow" aria-labelledby="workflow-title">
      <div class="section-title"><p class="page-kicker">WORKFLOW</p><h2 id="workflow-title">一次完整的项目面试</h2></div>
      <ol>
        <li v-for="(item, index) in flow" :key="item.title">
          <span>0{{ index + 1 }}</span><div><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div>
        </li>
      </ol>
    </section>

    <section class="project-rules">
      <div><View /><span><strong>面试过程不展示评分</strong><small>避免提示答案，也不打断表达。</small></span></div>
      <div><Aim /><span><strong>每次追问都有项目依据</strong><small>围绕职责、指标、原理与取舍验证。</small></span></div>
      <div><Microphone /><span><strong>语音能力已预留</strong><small>后续接入转写，不改变面试内核。</small></span></div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Aim, ArrowRight, Briefcase, Microphone, View } from '@element-plus/icons-vue'
import { readProjectDraft } from '@/features/project-deep-dive/composables/useProjectDraft'

const hasDraft = computed(() => Boolean(readProjectDraft().trim()))
const flow = [
  { title: '输入项目', description: '粘贴项目经历，先移除公司与个人敏感信息。' },
  { title: '确认事实', description: '修正技术栈、个人职责、指标和待验证声明。' },
  { title: '连续追问', description: '像真实面试一样回答，不在过程中查看评分。' },
  { title: '证据复盘', description: '结束后按能力维度、项目声明和回答轮次复盘。' },
]
</script>
