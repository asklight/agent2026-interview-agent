import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { setAccessToken, setRefreshHandler } from '@/api/http'
import * as authApi from '../api/authApi'
import type { AuthPayload, AuthUser } from '../model/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const token = ref<string | null>(null)
  const initialized = ref(false)
  let refreshPromise: Promise<string | null> | null = null

  const isAuthenticated = computed(() => Boolean(user.value && token.value))

  function accept(payload: AuthPayload) {
    user.value = payload.user
    token.value = payload.accessToken
    setAccessToken(payload.accessToken)
    return payload.accessToken
  }

  function clear() {
    user.value = null
    token.value = null
    setAccessToken(null)
  }

  async function refresh() {
    if (refreshPromise) return refreshPromise
    refreshPromise = authApi.refreshSession()
      .then(response => accept(response.data.data))
      .catch(() => {
        clear()
        return null
      })
      .finally(() => { refreshPromise = null })
    return refreshPromise
  }

  async function initialize() {
    if (initialized.value) return
    setRefreshHandler(refresh)
    await refresh()
    initialized.value = true
  }

  async function signIn(username: string, password: string) {
    const response = await authApi.login(username, password)
    return accept(response.data.data)
  }

  async function signUp(username: string, password: string) {
    const response = await authApi.register(username, password)
    return accept(response.data.data)
  }

  async function signOut() {
    try {
      await authApi.logout()
    } catch {
      // Local logout must still complete when the server session is already unavailable.
    } finally {
      clear()
    }
  }

  return { user, initialized, isAuthenticated, initialize, refresh, signIn, signUp, signOut }
})
