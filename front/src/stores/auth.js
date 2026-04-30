import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/authService'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null)
  const user = ref(null)
  let _refreshTimer = null

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function _parseJwtExpiry(token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
      return payload.exp ? payload.exp * 1000 : null
    } catch {
      return null
    }
  }

  function _scheduleTokenRefresh(token) {
    clearTimeout(_refreshTimer)
    const expiresAt = _parseJwtExpiry(token)
    if (!expiresAt) return
    const delay = expiresAt - Date.now() - 60_000
    if (delay > 0) {
      _refreshTimer = setTimeout(() => tryRestoreSession(), delay)
    } else {
      tryRestoreSession()
    }
  }

  function login(email, password) {
    return authService.login(email, password).then(res => {
      accessToken.value = res.data.accessToken
      user.value = res.data.user
      _scheduleTokenRefresh(res.data.accessToken)
      router.push({ name: 'hosts' })
    })
  }

  function logout() {
    clearTimeout(_refreshTimer)
    return authService.logout().catch(() => {}).then(() => {
      accessToken.value = null
      user.value = null
      router.push({ name: 'login' })
    })
  }

  function refreshProfile() {
    return authService.getProfile().then(res => {
      user.value = res.data
    })
  }

  function tryRestoreSession() {
    return authService.refresh().then(r => {
      accessToken.value = r.data.accessToken
      _scheduleTokenRefresh(r.data.accessToken)
      if (r.data.user) {
        user.value = r.data.user
      } else {
        return refreshProfile()
      }
    }).catch(() => {
      // No valid refresh token cookie — user stays logged out
    })
  }

  return { accessToken, user, isAuthenticated, isAdmin, login, logout, refreshProfile, tryRestoreSession }
})
