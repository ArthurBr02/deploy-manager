<template>
  <div class="fixed inset-0 bg-black/40 z-40 flex items-center justify-center p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
      <div class="p-5 border-b border-warm-border">
        <h2 class="font-semibold text-gray-900">Lancer — {{ typeLabel }}</h2>
        <p class="text-sm text-gray-500 mt-0.5">{{ host.name }} ({{ host.ip }})</p>
      </div>
      <div class="p-5 space-y-4">
        <div class="bg-warm-muted rounded-md p-3 font-mono text-xs text-gray-600 break-all">
          {{ resolvedCommand }}
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Timeout (minutes) — <span class="text-gray-400 font-normal">0 = désactivé</span>
          </label>
          <input v-model.number="timeout" type="number" min="0"
            class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
        </div>
        <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
      </div>
      <div class="p-4 bg-warm-muted border-t border-warm-border flex justify-end gap-2">
        <button @click="$emit('close')" class="px-4 py-2 rounded-md text-sm border border-warm-border bg-white hover:bg-warm-muted">Annuler</button>
        <button @click="launch" :disabled="loading"
          class="px-4 py-2 rounded-md text-sm bg-accent text-white hover:bg-accent-hover disabled:opacity-50">
          {{ loading ? 'Lancement...' : 'Lancer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import deploymentsService from '@/services/deploymentsService'

export default {
  props: {
    host: Object,
    type: String,
    defaultTimeout: { type: Number, default: 10 },
    defaultDeployCommand: { type: String, default: '' },
  },
  emits: ['close', 'deployed'],
  data() {
    return {
      timeout: this.defaultTimeout,
      loading: false,
      error: '',
    }
  },
  computed: {
    typeLabel() {
      return { DEPLOY: 'Déploiement', GENERATE: 'Génération', DELIVER: 'Livraison', ROLLBACK: 'Rollback' }[this.type] || this.type
    },
    resolvedCommand() {
      let cmd = this.type === 'DEPLOY' ? this.host.deploymentCommand
        : this.type === 'GENERATE' ? this.host.generateCommand
        : this.type === 'DELIVER' ? this.host.deliverCommand
        : this.host.rollbackCommand
      if (!cmd && this.type === 'DEPLOY') cmd = this.defaultDeployCommand
      
      if (!cmd) return '(aucune commande définie)'
      
      return cmd
        .replace('{host}', this.host.name || '')
        .replace('{ip}', this.host.ip || '')
        .replace('{domain}', this.host.domain || '')
    },
  },
  methods: {
    launch() {
      this.loading = true
      this.error = ''
      deploymentsService.launch(this.host.id, { type: this.type, timeout: this.timeout }).then(res => {
        this.$emit('deployed', res.data)
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur lors du lancement'
      }).finally(() => {
        this.loading = false
      })
    },
  },
}
</script>
