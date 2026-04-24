<template>
  <div class="min-h-screen bg-warm-muted flex items-center justify-center p-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center text-white text-xl font-bold mx-auto mb-4">D</div>
        <h1 class="text-2xl font-bold text-gray-900">Deploy Manager</h1>
        <p class="text-sm text-gray-500 mt-1">Connectez-vous pour continuer</p>
      </div>
      <div class="bg-white rounded-xl border border-warm-border shadow-sm p-6">
        <form @submit.prevent="submit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input v-model="email" type="email" required placeholder="admin@example.com"
              class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Mot de passe</label>
            <input v-model="password" type="password" required
              class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
          <button type="submit" :disabled="loading"
            class="w-full bg-accent hover:bg-accent-hover text-white rounded-md py-2 text-sm font-medium transition-colors disabled:opacity-50">
            {{ loading ? 'Connexion...' : 'Se connecter' }}
          </button>
        </form>
        <div class="mt-4 text-center">
          <RouterLink to="/forgot-password" class="text-sm text-accent hover:underline">Mot de passe oublié ?</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useAuthStore } from '@/stores/auth'

export default {
  computed: {
    ...mapStores(useAuthStore),
  },
  data() {
    return {
      email: '',
      password: '',
      loading: false,
      error: '',
    }
  },
  methods: {
    submit() {
      this.error = ''
      this.loading = true
      this.authStore.login(this.email, this.password).catch(e => {
        this.error = e.response?.data?.error || 'Identifiants invalides'
      }).finally(() => {
        this.loading = false
      })
    },
  },
}
</script>
