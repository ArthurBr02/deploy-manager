<template>
  <div class="min-h-screen bg-warm-muted flex items-center justify-center p-4">
    <div class="w-full max-w-sm">
      <div class="bg-white rounded-xl border border-warm-border shadow-sm p-6">
        <h1 class="text-xl font-bold text-gray-900 mb-4">Nouveau mot de passe</h1>
        <div v-if="done" class="text-center py-4">
          <div class="text-status-success font-medium mb-2">Mot de passe modifié !</div>
          <RouterLink to="/login" class="text-sm text-accent hover:underline">Se connecter</RouterLink>
        </div>
        <form v-else @submit.prevent="submit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nouveau mot de passe</label>
            <input v-model="password" type="password" required minlength="8" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
          <button type="submit" :disabled="loading" class="w-full bg-accent text-white rounded-md py-2 text-sm font-medium disabled:opacity-50">
            {{ loading ? 'Modification...' : 'Modifier le mot de passe' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const password = ref('')
const loading = ref(false)
const error = ref('')
const done = ref(false)

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await api.post('/auth/reset-password', { token: route.query.token, newPassword: password.value })
    done.value = true
  } catch (e) {
    error.value = e.response?.data?.error || 'Token invalide'
  } finally {
    loading.value = false
  }
}
</script>
