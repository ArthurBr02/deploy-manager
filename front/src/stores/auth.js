import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(email, password) {
    const res = await api.post('/auth/login', { email, password })
    accessToken.value = res.data.accessToken
    user.value = res.data.user
    router.push({ name: 'hosts' })
  }

  async function logout() {
    try { await api.post('/auth/logout') } catch {}
    accessToken.value = null
    user.value = null
    router.push({ name: 'login' })
  }

  async function refreshProfile() {
    const res = await api.get('/profile')
    user.value = res.data
  }

  return { accessToken, user, isAuthenticated, isAdmin, login, logout, refreshProfile }
})
