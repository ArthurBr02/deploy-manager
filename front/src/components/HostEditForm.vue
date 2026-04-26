<template>
  <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
    <h3 class="font-semibold text-gray-900">Modifier l'hôte</h3>
    <div class="grid grid-cols-2 gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
        <input v-model="form.name" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">IP</label>
        <input v-model="form.ip" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Domaine</label>
        <input v-model="form.domain" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Timeout par défaut (min)</label>
        <input v-model.number="form.defaultTimeout" type="number" min="0" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Commande de déploiement</label>
      <textarea v-model="form.deploymentCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Commande de génération (optionnel)</label>
      <textarea v-model="form.generateCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Commande de livraison (optionnel)</label>
      <textarea v-model="form.deliverCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Commande de logs applicatifs (optionnel)</label>
      <textarea v-model="form.tlogCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="ssh root@{domain} tlog" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Commande de Rollback (optionnel)</label>
      <textarea v-model="form.rollbackCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">URL Healthcheck (optionnel)</label>
      <input v-model="form.healthcheckUrl" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="https://{domain}" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Dossier de dumps SQL (optionnel)</label>
      <input v-model="form.dumpFolder" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="/var/www/dumps/host1" />
      <p class="text-[10px] text-gray-400 mt-0.5">Laisse vide pour utiliser le dossier par défaut des paramètres.</p>
    </div>
    <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
    <div class="flex justify-end">
      <button @click="save" :disabled="saving" class="px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
        {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
      </button>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'

export default {
  props: { host: Object },
  emits: ['saved'],
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      form: { ...this.host },
      saving: false,
      error: '',
    }
  },
  methods: {
    save() {
      this.saving = true
      this.error = ''
      hostsService.update(this.host.id, this.form).then(() => {
        this.toastStore.success('Hôte mis à jour')
        this.$emit('saved')
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur'
      }).finally(() => {
        this.saving = false
      })
    },
  },
}
</script>
