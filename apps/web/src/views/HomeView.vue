<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="home-hero__copy">
        <p class="page-kicker">JAVA INTERVIEW PRACTICE</p>
        <h1>把知识答对，<br />也把项目讲透。</h1>
        <p>八股练习保持轻量、即时反馈；项目深挖模拟真实面试，围绕你的职责、指标、技术原理和方案取舍连续追问。</p>
      </div>
      <div class="home-hero__signal" aria-hidden="true">
        <span>项目声明</span><i></i><span>证据追问</span><i></i><span>面试复盘</span>
      </div>
    </section>

    <section class="training-entry-grid" aria-label="训练模块">
      <RouterLink class="training-entry training-entry--practice" to="/practice/knowledge">
        <span class="training-entry__number">01</span>
        <div>
          <p>KNOWLEDGE PRACTICE</p>
          <h2>八股专项练习</h2>
          <span>快速选题、即时评分、一次针对性追问。适合查漏补缺和面试前热身。</span>
        </div>
        <strong>开始练习 →</strong>
      </RouterLink>
      <RouterLink class="training-entry training-entry--project" to="/project-deep-dive">
        <span class="training-entry__number">02</span>
        <div>
          <p>PROJECT DEEP DIVE</p>
          <h2>项目经历深挖</h2>
          <span>粘贴真实项目，确认系统提取的信息，进入连续追问的沉浸式文字面试。</span>
        </div>
        <strong>进入项目面试 →</strong>
      </RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
