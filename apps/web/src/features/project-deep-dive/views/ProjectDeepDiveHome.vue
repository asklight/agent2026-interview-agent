<template>
  <main class="project-landing page-frame">
    <section class="project-landing__hero">
      <div>
        <p class="page-kicker">PROJECT DEEP DIVE</p>
        <h1>不是再背一遍技术名词，<br />而是把你的项目经得起追问。</h1>
        <p>系统会先提取技术栈、个人职责、关键指标和需要验证的项目声明。确认无误后，再围绕真实性、原理、取舍与故障边界连续追问。</p>
        <div class="project-landing__actions">
          <RouterLink class="primary-link" to="/project-deep-dive/new">开始准备项目</RouterLink>
          <RouterLink v-if="hasDraft" class="secondary-link" to="/project-deep-dive/new">继续上次草稿</RouterLink>
        </div>
      </div>
      <aside class="deep-dive-map">
        <div v-for="(item, index) in flow" :key="item.title"><span>0{{ index + 1 }}</span><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div>
      </aside>
    </section>
    <section class="deep-dive-principles">
      <article><strong>过程不泄题</strong><p>面试中只出现自然对话，分数、命中点和内部决策统一留到结束后复盘。</p></article>
      <article><strong>追问来自你的声明</strong><p>不仅问 Redis、MySQL 原理，还会追问指标怎么测、你负责什么、方案为什么这样选。</p></article>
      <article><strong>为语音面试预留</strong><p>当前先做好文字消息与轮次模型，未来语音转写继续走同一套面试内核。</p></article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { readProjectDraft } from '@/features/project-deep-dive/composables/useProjectDraft'

const hasDraft = computed(() => Boolean(readProjectDraft().trim()))
const flow = [
  { title: '输入项目', description: '粘贴项目经历，敏感信息先脱敏。' },
  { title: '确认提取', description: '修正技术栈、职责、指标和声明。' },
  { title: '沉浸面试', description: '连续回答，不在过程中查看评分。' },
  { title: '证据复盘', description: '结束后按维度、声明和轮次复盘。' },
]
</script>
