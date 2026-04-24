<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Nouvel hôte</h1>
    </header>

    <div class="flex-1 overflow-auto p-6">
      <div class="max-w-2xl mx-auto space-y-6">
        <!-- Informations -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <div>
            <h2 class="font-semibold text-gray-900">Informations de l'hôte</h2>
            <p class="text-sm text-gray-400 mt-0.5">Renseignez l'identité et les points d'accès du serveur.</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Nom de l'hôte</label>
              <input v-model="form.name" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="ex: vpn" />
              <p class="text-xs text-gray-400 mt-1">Utilisé dans <code class="font-mono text-xs bg-warm-muted px-1 rounded">{host}</code>.</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Adresse IP</label>
              <input v-model="form.ip" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="10.42.1.12" />
            </div>
            <div class="col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">Domaine</label>
              <input v-model="form.domain" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="vpn.arthurbr02.fr" />
              <p class="text-xs text-gray-400 mt-1">Obligatoire pour les créations manuelles.</p>
            </div>
          </div>
        </div>

        <!-- Commandes -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <div>
            <h2 class="font-semibold text-gray-900">Commandes personnalisées</h2>
            <p class="text-sm text-gray-400 mt-0.5">Laissez vide pour utiliser la commande globale par défaut. Variables : <code class="font-mono text-xs bg-warm-muted px-1 rounded">{host}</code>, <code class="font-mono text-xs bg-warm-muted px-1 rounded">{ip}</code>, <code class="font-mono text-xs bg-warm-muted px-1 rounded">{domain}</code>.</p>
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <RocketIcon class="w-3.5 h-3.5 text-accent" /> Commande de déploiement
            </label>
            <input v-model="form.deployCommand" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" :placeholder="defaultDeployCommand || 'sh /root/{host}/liv.sh'" />
            <p class="text-xs text-gray-400 mt-1">Par défaut : <code class="font-mono text-xs">{{ defaultDeployCommand || 'sh /root/{host}/liv.sh' }}</code></p>
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <PackageIcon class="w-3.5 h-3.5 text-gray-400" /> Commande de génération
            </label>
            <input v-model="form.generateCommand" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="laisser vide pour masquer le bouton" />
            <p class="text-xs text-gray-400 mt-1">Le bouton « Générer » ne s'affiche pas si ce champ est vide.</p>
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <TruckIcon class="w-3.5 h-3.5 text-gray-400" /> Commande de livraison
            </label>
            <input v-model="form.deliverCommand" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="laisser vide pour masquer le bouton" />
          </div>

          <div>
            <label class="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1">
              <TerminalIcon class="w-3.5 h-3.5 text-gray-400" /> Commande Tlog
            </label>
            <input v-model="form.tlogCommand" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" :placeholder="defaultTlogCommand || 'ssh root@{domain} tlog'" />
            <p class="text-xs text-gray-400 mt-1">Par défaut : <code class="font-mono text-xs">{{ defaultTlogCommand || 'ssh root@{domain} tlog' }}</code></p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Timeout spécifique (minutes)</label>
            <div class="flex items-center gap-2">
              <input v-model="form.timeout" type="number" min="0" class="w-24 border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" placeholder="10" />
              <span class="text-xs text-gray-400">Vide = utiliser le timeout global. <strong>0</strong> = désactivé.</span>
            </div>
          </div>
        </div>

        <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>

        <div class="flex justify-end gap-2">
          <button @click="$router.push('/hosts')" class="px-4 py-2 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted">Annuler</button>
          <button @click="submit" :disabled="saving" class="px-6 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
            {{ saving ? 'Création...' : "Créer l'hôte" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import adminSettingsService from '@/services/adminSettingsService'
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon } from '@/components/icons'

export default {
  components: { RocketIcon, PackageIcon, TruckIcon, TerminalIcon },
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      defaultDeployCommand: '',
      defaultTlogCommand: '',
      form: {
        name: '',
        ip: '',
        domain: '',
        deployCommand: '',
        generateCommand: '',
        deliverCommand: '',
        tlogCommand: '',
        timeout: '',
      },
      saving: false,
      error: '',
    }
  },
  mounted() {
    adminSettingsService.get().then(res => {
      this.defaultDeployCommand = res.data.settings?.default_deploy_command || ''
      this.defaultTlogCommand = res.data.settings?.default_tlog_command || ''
    }).catch(() => {})
  },
  methods: {
    submit() {
      this.error = ''
      if (!this.form.name || !this.form.ip) {
        this.error = "Le nom et l'adresse IP sont obligatoires."
        return
      }
      this.saving = true
      hostsService.create({
        name: this.form.name,
        ip: this.form.ip,
        domain: this.form.domain || null,
        deploymentCommand: this.form.deployCommand || null,
        generateCommand: this.form.generateCommand || null,
        deliverCommand: this.form.deliverCommand || null,
        tlogCommand: this.form.tlogCommand || null,
        defaultTimeout: this.form.timeout !== '' ? Number(this.form.timeout) : null,
      }).then(() => {
        this.toastStore.success('Hôte créé avec succès')
        this.$router.push('/hosts')
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur lors de la création'
      }).finally(() => {
        this.saving = false
      })
    },
  },
}
</script>
