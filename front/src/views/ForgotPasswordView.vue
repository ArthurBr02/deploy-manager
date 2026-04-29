<template>
  <div class="min-h-screen bg-warm-muted flex items-center justify-center p-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center text-white text-xl font-bold mx-auto mb-4">D</div>
        <h1 class="text-xl font-bold text-gray-900">Mot de passe oublié</h1>
        <p class="text-sm text-gray-500 mt-1">Recevez un lien de réinitialisation</p>
      </div>
      <div class="bg-white rounded-xl border border-warm-border shadow-sm p-6">
        <div v-if="sent" class="text-center py-4">
          <div class="text-status-success font-medium mb-2">Email envoyé !</div>
          <p class="text-sm text-gray-500">Vérifiez votre boîte mail.</p>
        </div>
        <form v-else @submit.prevent="submit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input v-model="email" type="email" required class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
          <button type="submit" :disabled="loading" class="w-full bg-accent text-white rounded-md py-2 text-sm font-medium transition-colors disabled:opacity-50">
            {{ loading ? 'Envoi...' : 'Envoyer le lien' }}
          </button>
        </form>
        <div class="mt-4 text-center">
          <RouterLink to="/login" class="text-sm text-accent hover:underline">Retour à la connexion</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import authService from '@/services/authService'

export default {
  data() {
    return {
      email: '',
      loading: false,
      error: '',
      sent: false,
    }
  },
  methods: {
    submit() {
      this.loading = true
      this.error = ''
      authService.forgotPassword(this.email).then(() => {
        this.sent = true
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur'
      }).finally(() => {
        this.loading = false
      })
    },
  },
}
</script>
