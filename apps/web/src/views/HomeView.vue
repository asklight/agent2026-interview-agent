<template>
  <main class="home-page">
    <header class="workspace-heading">
      <div><p class="page-kicker">INTERVIEW WORKBENCH</p><h1>今天练什么？</h1></div>
      <p>八股用来快速校准知识，项目深挖用来练清楚真实经历。两种训练，各自保持合适的重量。</p>
    </header>

    <section class="training-entry-list" aria-label="训练模块">
      <RouterLink class="training-entry" to="/practice/knowledge">
        <span class="training-entry__icon training-entry__icon--knowledge"><Collection /></span>
        <div class="training-entry__body">
          <p>KNOWLEDGE PRACTICE</p><h2>八股练习</h2>
          <span>选一个知识模块，连续完成 3 到 10 道题。每题即时反馈，适合热身和查漏补缺。</span>
        </div>
        <div class="training-entry__meta"><span>轻量 · 即时反馈</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry" to="/project-deep-dive">
        <span class="training-entry__icon training-entry__icon--project"><Briefcase /></span>
        <div class="training-entry__body">
          <p>PROJECT DEEP DIVE</p><h2>项目深挖</h2>
          <span>从你的项目事实出发，接受连续追问。面试过程不展示评分，结束后按证据复盘。</span>
        </div>
        <div class="training-entry__meta"><span>沉浸 · 连续追问</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry" to="/practice/algorithm">
        <span class="training-entry__icon training-entry__icon--algorithm"><DataAnalysis /></span>
        <div class="training-entry__body">
          <p>ALGORITHM ORAL PRACTICE</p><h2>算法口述</h2>
          <span>按真实面试节奏讲清题意、方案、优化、复杂度和边界。过程不打断，结束后按原话证据复盘。</span>
        </div>
        <div class="training-entry__meta"><span>专项 · 连续追问</span><ArrowRight /></div>
      </RouterLink>
      <RouterLink class="training-entry training-entry--simulation" to="/simulation/new">
        <span class="training-entry__icon training-entry__icon--simulation"><VideoCamera /></span>
        <div class="training-entry__body">
          <p>FULL INTERVIEW SIMULATION</p><h2>综合模拟</h2>
          <span>把项目深挖、基础问答和算法口述连成一场完整技术面试。过程保持沉浸，结束后统一复盘。</span>
        </div>
        <div class="training-entry__meta"><span>综合 · 面试节奏</span><ArrowRight /></div>
      </RouterLink>
    </section>

    <section class="home-path" aria-label="训练路径">
      <div><span>01</span><strong>知识校准</strong><small>先确认基础概念能讲清楚</small></div>
      <i></i><div><span>02</span><strong>项目表达</strong><small>再把真实经历讲出证据</small></div>
      <i></i><div><span>03</span><strong>复盘改进</strong><small>最后只看可执行的下一步</small></div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowRight, Briefcase, Collection, DataAnalysis, VideoCamera } from '@element-plus/icons-vue'
</script>
