<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="register-title">
      <RouterLink class="auth-brand" to="/"><ChatDotSquare /><span>北洋面试官</span></RouterLink>
      <div class="auth-heading">
        <p class="section-kicker">创建账号</p>
        <h1 id="register-title">保存你的每一次进步</h1>
      </div>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" maxlength="32" autofocus />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" autocomplete="new-password" show-password type="password" />
        </el-form-item>
        <el-form-item label="确认密码" :error="mismatch ? '两次输入的密码不一致' : ''">
          <el-input v-model="confirmation" autocomplete="new-password" show-password type="password" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="full-width" type="primary" :loading="submitting" @click="submit">创建账号</el-button>
      </el-form>
      <p class="auth-switch">已有账号？<RouterLink :to="loginTarget">返回登录</RouterLink></p>
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
const confirmation = ref('')
const submitting = ref(false)
const mismatch = computed(() => Boolean(confirmation.value && password.value !== confirmation.value))
const redirect = computed(() => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
  ? route.query.redirect : '/')
const loginTarget = computed(() => ({ path: '/login', query: { redirect: redirect.value } }))

async function submit() {
  if (!username.value.trim() || !password.value || mismatch.value || submitting.value) return
  submitting.value = true
  try {
    await auth.signUp(username.value, password.value)
    await router.replace(redirect.value)
  } finally {
    submitting.value = false
  }
}
</script>
