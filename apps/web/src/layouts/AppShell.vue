<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink class="site-brand" to="/">
        <span class="site-brand__mark">北</span>
        <span><strong>北洋面试官</strong><small>Java 后端求职训练</small></span>
      </RouterLink>
      <nav class="site-nav" aria-label="主要导航">
        <RouterLink to="/practice/knowledge">八股练习</RouterLink>
        <RouterLink to="/project-deep-dive">项目深挖</RouterLink>
      </nav>
    </header>
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>
