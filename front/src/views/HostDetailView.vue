<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <RouterLink to="/hosts" class="hover:text-gray-700">Hôtes</RouterLink>
        <span>/</span>
        <span class="text-gray-900 font-medium">{{ host?.name }}</span>
      </div>
      <div class="flex-1" />
      <div v-if="activeDeployment" class="flex items-center gap-2">
        <span class="text-sm text-status-progress">Déploiement en cours...</span>
        <button @click="cancelDeployment" class="text-sm text-red-500 hover:text-red-700 border border-red-200 px-3 py-1 rounded-md">Annuler</button>
      </div>
    </header>

    <div class="flex-1 overflow-auto p-6">
      <div v-if="!host" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="max-w-5xl mx-auto space-y-6">
        <!-- Info card -->
        <div class="bg-white border border-warm-border rounded-xl p-5">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900">{{ host.name }}</h2>
              <div class="text-sm text-gray-400 font-mono">{{ host.ip }}</div>
            </div>
            <StatusBadge :status="host.lastDeploymentStatus || 'NEVER'" />
          </div>
          <div class="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
            <div><span class="text-gray-400">Domaine</span><div class="font-medium">{{ host.domain || '—' }}</div></div>
            <div><span class="text-gray-400">Timeout</span><div class="font-medium">{{ host.defaultTimeout != null ? host.defaultTimeout + ' min' : 'Défaut' }}</div></div>
          </div>
          <div class="mt-4 flex gap-2">
            <button v-if="host.canDeploy" @click="openModal('DEPLOY')" class="flex items-center gap-1.5 px-3 py-1.5 bg-accent text-white rounded-md text-sm hover:bg-accent-hover">
              <RocketIcon class="w-3.5 h-3.5" /> Déployer
            </button>
            <button v-if="host.canDeploy && host.generateCommand" @click="openModal('GENERATE')" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted">
              <PackageIcon class="w-3.5 h-3.5" /> Générer
            </button>
            <button v-if="host.canDeploy && host.deliverCommand" @click="openModal('DELIVER')" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted">
              <TruckIcon class="w-3.5 h-3.5" /> Livrer
            </button>
          </div>
        </div>

        <!-- Tabs -->
        <div class="border-b border-warm-border flex gap-0">
          <button v-for="tab in tabs" :key="tab.id" @click="activeTab = tab.id"
            class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === tab.id ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-gray-700'">
            {{ tab.label }}
          </button>
        </div>

        <!-- Logs -->
        <div v-if="activeTab === 'logs'">
          <div v-if="!currentDeploymentId" class="text-center py-10 text-gray-400 border-2 border-dashed border-warm-border rounded-xl">
            <TerminalIcon class="w-8 h-8 mx-auto mb-2 opacity-30" />
            <p>Aucun déploiement actif</p>
          </div>
          <div v-else>
            <div class="dm-term h-96 overflow-auto" ref="logEl">{{ logContent }}</div>
          </div>
        </div>

        <!-- History -->
        <div v-if="activeTab === 'history'">
          <DeploymentTable :deployments="deploymentHistory" :loading="histLoading" />
        </div>

        <!-- Details (edit) -->
        <div v-if="activeTab === 'details' && host.canEdit">
          <HostEditForm :host="host" @saved="loadHost" />
        </div>
        <div v-else-if="activeTab === 'details' && !host.canEdit" class="text-center py-10 text-gray-400">
          Vous n'avez pas la permission de modifier cet hôte.
        </div>
      </div>
    </div>
  </div>

  <DeployModal v-if="modal.show" :host="host" :type="modal.type" :default-timeout="host?.defaultTimeout ?? 10"
    @close="modal.show = false" @deployed="onDeployed" />
</template>

<script>
import { mapStores, mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import deploymentsService from '@/services/deploymentsService'
import StatusBadge from '@/components/StatusBadge.vue'
import DeployModal from '@/components/DeployModal.vue'
import DeploymentTable from '@/components/DeploymentTable.vue'
import HostEditForm from '@/components/HostEditForm.vue'
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon } from '@/components/icons'

export default {
  components: { StatusBadge, DeployModal, DeploymentTable, HostEditForm, RocketIcon, PackageIcon, TruckIcon, TerminalIcon },
  computed: {
    ...mapStores(useToastStore),
    ...mapState(useAuthStore, ['accessToken']),
  },
  data() {
    return {
      host: null,
      activeTab: 'logs',
      tabs: [{ id: 'logs', label: 'Logs' }, { id: 'history', label: 'Historique' }, { id: 'details', label: 'Détails' }],
      modal: { show: false, type: 'DEPLOY' },
      logContent: '',
      currentDeploymentId: null,
      activeDeployment: null,
      deploymentHistory: [],
      histLoading: false,
      _sseSource: null,
      _eventSrc: null,
    }
  },
  async mounted() {
    await this.loadHost()
    await this.loadHistory()

    const src = new EventSource(`/api/deployments/events?token=${this.accessToken}`)
    src.addEventListener('deployment.status', e => {
      try {
        const { hostId, deploymentId, status } = JSON.parse(e.data)
        if (hostId !== this.$route.params.id) return
        this.loadHost()
        if (status === 'IN_PROGRESS') {
          if (this.currentDeploymentId !== deploymentId) {
            this.logContent = ''
            this.activeTab = 'logs'
            this.startSse(deploymentId)
          }
        } else {
          this.loadHistory()
        }
      } catch {}
    })
    this._eventSrc = src
  },
  unmounted() {
    if (this._sseSource) this._sseSource.close()
    if (this._eventSrc) this._eventSrc.close()
  },
  methods: {
    async loadHost() {
      const res = await hostsService.getById(this.$route.params.id)
      this.host = res.data
    },
    async loadHistory() {
      this.histLoading = true
      try {
        const res = await deploymentsService.getHistory(this.$route.params.id)
        this.deploymentHistory = res.data.content
        const inProgress = this.deploymentHistory.find(d => d.status === 'IN_PROGRESS')
        if (inProgress) this.startSse(inProgress.id)
        else { this.currentDeploymentId = null; this.activeDeployment = null }
      } finally {
        this.histLoading = false
      }
    },
    startSse(deploymentId) {
      this.currentDeploymentId = deploymentId
      this.activeDeployment = { id: deploymentId }
      if (this._sseSource) this._sseSource.close()
      const src = new EventSource(`/api/deployments/${deploymentId}/logs?token=${this.accessToken}`)
      src.addEventListener('log', e => {
        this.logContent += e.data
        this.$nextTick(() => {
          if (this.$refs.logEl) this.$refs.logEl.scrollTop = this.$refs.logEl.scrollHeight
        })
      })
      src.addEventListener('end', () => {
        src.close()
        this.activeDeployment = null
        this.loadHistory()
        this.loadHost()
      })
      this._sseSource = src
    },
    async cancelDeployment() {
      if (!this.currentDeploymentId) return
      try {
        await deploymentsService.cancel(this.currentDeploymentId)
        this.toastStore.info('Déploiement annulé')
        this.loadHistory()
      } catch (e) {
        this.toastStore.error(e.response?.data?.error || 'Erreur')
      }
    },
    openModal(type) {
      this.modal = { show: true, type }
    },
    onDeployed(deployment) {
      this.modal.show = false
      this.toastStore.success('Déploiement lancé')
      this.logContent = ''
      this.activeTab = 'logs'
      if (deployment?.id) {
        this.startSse(deployment.id)
      } else {
        setTimeout(() => this.loadHistory(), 500)
      }
    },
  },
}
</script>
