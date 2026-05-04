<template>
  <div class="min-h-screen bg-warm-muted flex items-center justify-center p-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center text-white text-xl font-bold mx-auto mb-4">D</div>
        <h1 class="text-2xl font-bold text-gray-900">Vérification</h1>
        <p class="text-sm text-gray-500 mt-1">Un code a été envoyé à votre adresse e-mail</p>
      </div>
      <div class="bg-white rounded-xl border border-warm-border shadow-sm p-6 space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Code à 6 chiffres</label>
          <div class="flex gap-2 justify-center">
            <input
              v-for="(_, i) in 6"
              :key="i"
              :ref="el => { if (el) inputs[i] = el }"
              v-model="digits[i]"
              type="text"
              inputmode="numeric"
              maxlength="1"
              @input="onInput(i)"
              @keydown.backspace="onBackspace(i)"
              @paste.prevent="onPaste"
              class="w-10 h-12 text-center text-lg font-mono border border-warm-border rounded-md outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 transition-colors"
              :class="error ? 'border-red-300' : ''"
            />
          </div>
        </div>

        <div class="flex items-start gap-3 group cursor-pointer" @click="trustDevice = !trustDevice">
          <div class="relative flex items-center justify-center w-5 h-5 mt-0.5 border-2 rounded transition-all duration-200 shrink-0"
            :class="trustDevice ? 'bg-accent border-accent' : 'bg-white border-warm-border group-hover:border-gray-400'">
            <svg v-if="trustDevice" class="w-3.5 h-3.5 text-white stroke-[3]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700 cursor-pointer select-none">Faire confiance à cet appareil</label>
            <p class="text-xs text-gray-400 mt-0.5">Ne plus demander de code pendant 30 jours</p>
          </div>
        </div>

        <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>

        <button
          @click="submit"
          :disabled="loading || code.length !== 6"
          class="w-full bg-accent hover:bg-accent-hover text-white rounded-md py-2 text-sm font-medium transition-colors disabled:opacity-50"
        >
          {{ loading ? 'Vérification...' : 'Vérifier' }}
        </button>

        <div class="text-center">
          <button @click="back" class="text-sm text-accent hover:underline">
            ← Retour à la connexion
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import mfaService from '@/services/mfaService'

export default {
  computed: {
    ...mapStores(useAuthStore),
    code() {
      return this.digits.join('')
    },
  },
  data() {
    return {
      digits: ['', '', '', '', '', ''],
      inputs: [],
      trustDevice: false,
      loading: false,
      error: '',
    }
  },
  mounted() {
    if (!sessionStorage.getItem('mfa_challenge_id')) {
      this.$router.replace({ name: 'login' })
      return
    }
    this.$nextTick(() => this.inputs[0]?.focus())
  },
  methods: {
    onInput(index) {
      const val = this.digits[index]
      if (!/^\d$/.test(val)) {
        this.digits[index] = ''
        return
      }
      if (index < 5) {
        this.$nextTick(() => this.inputs[index + 1]?.focus())
      } else {
        this.$nextTick(() => this.inputs[5]?.blur())
        if (this.code.length === 6) this.submit()
      }
    },
    onBackspace(index) {
      if (!this.digits[index] && index > 0) {
        this.digits[index - 1] = ''
        this.$nextTick(() => this.inputs[index - 1]?.focus())
      }
    },
    onPaste(event) {
      const pasted = (event.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6)
      pasted.split('').forEach((char, i) => {
        this.digits[i] = char
      })
      this.$nextTick(() => {
        const next = Math.min(pasted.length, 5)
        this.inputs[next]?.focus()
      })
    },
    async submit() {
      if (this.code.length !== 6 || this.loading) return
      const challengeId = sessionStorage.getItem('mfa_challenge_id')
      if (!challengeId) {
        this.$router.replace({ name: 'login' })
        return
      }
      this.error = ''
      this.loading = true
      try {
        const res = await mfaService.verifyMfa(challengeId, this.code, this.trustDevice)
        sessionStorage.removeItem('mfa_challenge_id')
        this.authStore.handleLoginSuccess(res.data)
      } catch (e) {
        this.error = e.response?.data?.error || 'Code incorrect'
        this.digits = ['', '', '', '', '', '']
        this.$nextTick(() => this.inputs[0]?.focus())
        if (this.error.includes('reconnecter')) {
          sessionStorage.removeItem('mfa_challenge_id')
        }
      } finally {
        this.loading = false
      }
    },
    back() {
      sessionStorage.removeItem('mfa_challenge_id')
      this.$router.push({ name: 'login' })
    },
  },
}
</script>
