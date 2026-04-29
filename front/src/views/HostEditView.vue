<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <RouterLink to="/hosts" class="hover:text-gray-700">Hôtes</RouterLink>
        <span>/</span>
        <RouterLink :to="`/hosts/${$route.params.id}`" class="hover:text-gray-700">{{ host?.name || '…' }}</RouterLink>
        <span>/</span>
        <span class="font-medium text-gray-900">Modifier</span>
      </div>
    </header>

    <div class="flex-1 overflow-auto p-6">
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else-if="!host?.canEdit" class="flex items-center justify-center py-20">
        <div class="text-center">
          <p class="text-gray-500 mb-4">Vous n'avez pas la permission de modifier cet hôte.</p>
          <RouterLink :to="`/hosts/${$route.params.id}`" class="text-accent hover:underline text-sm">← Retour à l'hôte</RouterLink>
        </div>
      </div>
      <div v-else class="max-w-2xl mx-auto space-y-6">
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <div>
            <h2 class="font-semibold text-gray-900">Modifier l'hôte</h2>
            <p class="text-sm text-gray-400 mt-0.5">Variables disponibles dans les commandes : <code class="font-mono text-xs bg-warm-muted px-1 rounded">{host}</code>, <code class="font-mono text-xs bg-warm-muted px-1 rounded">{ip}</code>, <code class="font-mono text-xs bg-warm-muted px-1 rounded">{domain}</code>.</p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Nom de l'hôte</label>
              <input v-model="form.name" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Adresse IP</label>
              <input v-model="form.ip" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">Domaine</label>
              <input v-model="form.domain" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Utilisateur SSH</label>
              <input v-model="form.sshUser" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="root" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Port SSH</label>
              <input v-model.number="form.sshPort" type="number" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="22" />
            </div>
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <RocketIcon class="w-3.5 h-3.5 text-accent" /> Commande de déploiement
            </label>
            <textarea v-model="form.deploymentCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="Laisser vide pour utiliser la commande par défaut" />
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <PackageIcon class="w-3.5 h-3.5 text-gray-400" /> Commande de génération
            </label>
            <textarea v-model="form.generateCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="Laisser vide pour masquer le bouton" />
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <TruckIcon class="w-3.5 h-3.5 text-gray-400" /> Commande de livraison
            </label>
            <textarea v-model="form.deliverCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="Laisser vide pour masquer le bouton" />
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <TerminalIcon class="w-3.5 h-3.5 text-gray-400" /> Commande Tlog
            </label>
            <textarea v-model="form.tlogCommand" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="Laisser vide pour utiliser la commande par défaut" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Timeout spécifique (minutes)</label>
            <div class="flex items-center gap-2">
              <input v-model.number="form.defaultTimeout" type="number" min="0" class="w-24 border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <span class="text-xs text-gray-400">Vide = utiliser le timeout global. <strong>0</strong> = désactivé.</span>
            </div>
          </div>

          <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>

          <div class="flex justify-end gap-2 pt-2">
            <RouterLink :to="`/hosts/${$route.params.id}`" class="px-4 py-2 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted">Annuler</RouterLink>
            <button @click="save" :disabled="saving" class="px-6 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
              {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon } from '@/components/icons'

export default {
  components: { RocketIcon, PackageIcon, TruckIcon, TerminalIcon },
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      host: null,
      form: {
        name: '',
        ip: '',
        domain: '',
        sshUser: '',
        sshPort: null,
        deploymentCommand: '',
        generateCommand: '',
        deliverCommand: '',
        tlogCommand: '',
        defaultTimeout: null,
      },
      loading: true,
      saving: false,
      error: '',
    }
  },
  mounted() {
    hostsService.getById(this.$route.params.id).then(res => {
      this.host = res.data
      if (res.data.canEdit) {
        this.form = {
          name: res.data.name || '',
          ip: res.data.ip || '',
          domain: res.data.domain || '',
          sshUser: res.data.sshUser || '',
          sshPort: res.data.sshPort ?? 22,
          deploymentCommand: res.data.deploymentCommand || '',
          generateCommand: res.data.generateCommand || '',
          deliverCommand: res.data.deliverCommand || '',
          tlogCommand: res.data.tlogCommand || '',
          defaultTimeout: res.data.defaultTimeout ?? null,
        }
      }
    }).finally(() => {
      this.loading = false
    })
  },
  methods: {
    save() {
      this.error = ''
      if (!this.form.name || !this.form.ip) {
        this.error = "Le nom et l'adresse IP sont obligatoires."
        return
      }
      this.saving = true
      hostsService.update(this.$route.params.id, {
        ...this.form,
        deploymentCommand: this.form.deploymentCommand || null,
        generateCommand: this.form.generateCommand || null,
        deliverCommand: this.form.deliverCommand || null,
        tlogCommand: this.form.tlogCommand || null,
        domain: this.form.domain || null,
      }).then(() => {
        this.toastStore.success('Hôte mis à jour')
        this.$router.push(`/hosts/${this.$route.params.id}`)
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur lors de la sauvegarde'
      }).finally(() => {
        this.saving = false
      })
    },
  },
}
</script>
