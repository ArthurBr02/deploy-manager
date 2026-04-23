import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/authService'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(email, password) {
    const res = await authService.login(email, password)
    accessToken.value = res.data.accessToken
    user.value = res.data.user
    router.push({ name: 'hosts' })
  }

  async function logout() {
    try { await authService.logout() } catch {}
    accessToken.value = null
    user.value = null
    router.push({ name: 'login' })
  }

  async function refreshProfile() {
    const res = await authService.getProfile()
    user.value = res.data
  }

  async function tryRestoreSession() {
    try {
      const r = await authService.refresh()
      accessToken.value = r.data.accessToken
      if (r.data.user) {
        user.value = r.data.user
      } else {
        await refreshProfile()
      }
    } catch {
      // No valid refresh token cookie — user stays logged out
    }
  }

  return { accessToken, user, isAuthenticated, isAdmin, login, logout, refreshProfile, tryRestoreSession }
})
