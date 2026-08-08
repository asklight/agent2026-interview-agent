<template>
  <div class="site-shell">
    <aside class="site-sidebar">
      <RouterLink class="site-brand" to="/" aria-label="北洋面试官首页">
        <span class="site-brand__mark"><ChatDotSquare /></span>
        <span><strong>北洋面试官</strong><small>Interview Workbench</small></span>
      </RouterLink>

      <nav class="site-nav" aria-label="主要导航">
        <RouterLink to="/" exact-active-class="router-link-active"><HomeFilled /><span>训练首页</span></RouterLink>
        <RouterLink to="/practice/knowledge"><Collection /><span>八股练习</span></RouterLink>
        <RouterLink to="/practice/algorithm"><DataAnalysis /><span>算法口述</span></RouterLink>
        <RouterLink to="/project-deep-dive"><Briefcase /><span>项目深挖</span></RouterLink>
      </nav>

      <div class="site-sidebar__footer">
        <span class="account-avatar"><UserFilled /></span>
        <div><strong>{{ auth.user?.username }}</strong><small>训练记录已同步</small></div>
        <el-tooltip content="退出登录" placement="top">
          <button class="account-logout" type="button" aria-label="退出登录" @click="signOut"><SwitchButton /></button>
        </el-tooltip>
      </div>
    </aside>

    <div class="site-workspace">
      <header class="mobile-header">
        <RouterLink class="mobile-brand" to="/"><ChatDotSquare /><strong>北洋面试官</strong></RouterLink>
        <nav aria-label="移动端导航">
          <RouterLink to="/practice/knowledge" aria-label="八股练习"><Collection /></RouterLink>
          <RouterLink to="/practice/algorithm" aria-label="算法口述"><DataAnalysis /></RouterLink>
          <RouterLink to="/project-deep-dive" aria-label="项目深挖"><Briefcase /></RouterLink>
          <button type="button" aria-label="退出登录" @click="signOut"><SwitchButton /></button>
        </nav>
      </header>
      <RouterView />
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useRouter } from 'vue-router'
import { Briefcase, ChatDotSquare, Collection, DataAnalysis, HomeFilled, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/features/identity/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function signOut() {
  await auth.signOut()
  await router.replace('/login')
}
</script>
