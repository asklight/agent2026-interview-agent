<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="login-title">
      <RouterLink class="auth-brand" to="/"><ChatDotSquare /><span>北洋面试官</span></RouterLink>
      <div class="auth-heading">
        <p class="section-kicker">账号登录</p>
        <h1 id="login-title">继续你的面试训练</h1>
      </div>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" maxlength="32" autofocus />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" autocomplete="current-password" show-password type="password" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="full-width" type="primary" :loading="submitting" @click="submit">登录</el-button>
      </el-form>
      <p class="auth-switch">还没有账号？<RouterLink :to="registerTarget">创建账号</RouterLink></p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ChatDotSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const submitting = ref(false)
const redirect = computed(() => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
  ? route.query.redirect : '/')
const registerTarget = computed(() => ({ path: '/register', query: { redirect: redirect.value } }))

async function submit() {
  if (!username.value.trim() || !password.value || submitting.value) return
  submitting.value = true
  try {
    await auth.signIn(username.value, password.value)
    await router.replace(redirect.value)
  } finally {
    submitting.value = false
  }
}
</script>
