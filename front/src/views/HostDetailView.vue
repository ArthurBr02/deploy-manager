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
      <div v-else class="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
        <!-- Left Column: Host Info, History & Deployment Logs -->
        <div class="space-y-6">
          <!-- Info card -->
          <div class="bg-white border border-warm-border rounded-xl p-5 shadow-sm">
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
            <div class="mt-4 flex flex-wrap gap-2">
              <button v-if="host.canDeploy" @click="openModal('DEPLOY')" class="flex items-center gap-1.5 px-3 py-1.5 bg-accent text-white rounded-md text-sm hover:bg-accent-hover">
                <RocketIcon class="w-3.5 h-3.5" /> Déployer
              </button>
              <button v-if="host.canDeploy && host.generateCommand" @click="openModal('GENERATE')" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted">
                <PackageIcon class="w-3.5 h-3.5" /> Générer
              </button>
              <button v-if="host.canDeploy && host.deliverCommand" @click="openModal('DELIVER')" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted">
                <TruckIcon class="w-3.5 h-3.5" /> Livrer
              </button>
              <button v-if="host.canDeploy && host.rollbackCommand" @click="openModal('ROLLBACK')" class="flex items-center gap-1.5 px-3 py-1.5 border border-red-200 text-red-600 rounded-md text-sm hover:bg-red-50">
                <RefreshIcon class="w-3.5 h-3.5" /> Rollback
              </button>
              <RouterLink v-if="host.canExecute" :to="`/hosts/${host.id}/terminal`" class="flex items-center gap-1.5 px-3 py-1.5 border border-accent text-accent rounded-md text-sm hover:bg-accent/5">
                <TerminalIcon class="w-3.5 h-3.5" /> Terminal
              </RouterLink>
              <RouterLink v-if="host.canEdit" :to="`/hosts/${host.id}/edit`" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted ml-auto">
                <EditIcon class="w-3.5 h-3.5" /> Modifier
              </RouterLink>
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

          <!-- Logs Content -->
          <div v-if="activeTab === 'logs'">
            <div v-if="!viewedDeployment && !currentDeploymentId" class="text-center py-10 text-gray-400 border-2 border-dashed border-warm-border rounded-xl">
              <TerminalIcon class="w-8 h-8 mx-auto mb-2 opacity-30" />
              <p>Aucun déploiement disponible</p>
            </div>
            <div v-else class="space-y-3">
              <!-- Deployment metadata banner -->
              <div v-if="viewedDeployment || currentDeploymentId" class="bg-white border border-warm-border rounded-xl px-4 py-3 flex flex-wrap items-center gap-x-5 gap-y-1 text-sm shadow-sm">
                <UserBadge v-if="displayDeployment" :user="{ firstName: displayDeployment.userFirstName, lastName: displayDeployment.userLastName, avatar: displayDeployment.userAvatar }" />
                <span class="text-gray-400">{{ formatDate(displayDeployment?.createdAt) }}</span>
                <TypeBadge v-if="displayDeployment?.type" :type="displayDeployment.type" />
                <StatusBadge v-if="displayDeployment?.status" :status="displayDeployment.status" />
                <span v-if="currentDeploymentId" class="ml-auto flex items-center gap-1.5 text-status-progress text-xs animate-pulse">
                  <span class="w-1.5 h-1.5 rounded-full bg-status-progress inline-block"></span>
                  En cours…
                </span>
              </div>
              <div class="dm-term h-80 overflow-auto" ref="logEl">{{ logContent }}</div>
            </div>
          </div>

          <!-- History -->
          <div v-if="activeTab === 'history'">
            <DeploymentTable :deployments="deploymentHistory" :loading="histLoading" @view="viewDeployment" />
          </div>

          <!-- Details (edit) -->
          <div v-if="activeTab === 'details' && host.canEdit">
            <HostEditForm :host="host" @saved="loadHost" />
          </div>
        </div>

        <!-- Right Column: Application Logs (tlog) -->
        <div class="bg-white border border-warm-border rounded-xl flex flex-col overflow-hidden sticky top-6 h-[calc(100vh-140px)] shadow-sm">
          <header class="h-12 border-b border-warm-border bg-warm-muted/30 px-4 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <TerminalIcon class="w-4 h-4 text-gray-400" />
              <span class="text-xs font-bold text-gray-600 uppercase tracking-wider">Logs Application (tlog)</span>
            </div>
            <div class="flex items-center gap-2">
              <button v-if="!tlogActive" @click="startTlog" class="text-xs px-2.5 py-1 bg-accent text-white rounded-md hover:bg-accent-hover font-medium transition-colors">Démarrer</button>
              <button v-else @click="stopTlog" class="text-xs px-2.5 py-1 bg-red-500 text-white rounded-md hover:bg-red-600 font-medium transition-colors">Arrêter</button>
            </div>
          </header>
          <div class="flex-1 bg-[#1a1a1a] p-3 overflow-auto font-mono text-[11px] leading-relaxed selection:bg-accent/30" ref="tlogEl">
            <div v-for="(line, i) in tlogLines" :key="i" class="text-gray-300 border-l border-white/5 pl-2 mb-0.5 whitespace-pre-wrap break-all">{{ line }}</div>
            <div v-if="!tlogActive && !tlogLines.length" class="h-full flex flex-col items-center justify-center text-gray-500 italic text-center p-6">
              <RefreshIcon class="w-8 h-8 mb-3 opacity-20 text-accent animate-pulse" />
              <p>Connectez-vous pour visualiser le flux <code class="bg-white/5 px-1 rounded not-italic">tlog</code> de l'hôte.</p>
            </div>
            <div v-if="tlogActive && !tlogLines.length" class="h-full flex flex-col items-center justify-center text-gray-400 italic">
              <div class="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin mb-3"></div>
              Connexion en cours...
            </div>
          </div>
          <footer class="h-8 border-t border-warm-border bg-warm-muted/10 px-4 flex items-center justify-between text-[10px] text-gray-400">
            <span>{{ tlogActive ? 'Flux actif' : 'Flux déconnecté' }}</span>
            <button v-if="tlogLines.length" @click="tlogLines = []" class="hover:text-gray-600 underline">Effacer la console</button>
          </footer>
        </div>
      </div>
    </div>
  </div>

  <DeployModal v-if="modal.show" :host="host" :type="modal.type" :default-timeout="host?.defaultTimeout ?? 10"
    :default-deploy-command="defaultDeployCommand" @close="modal.show = false" @deployed="onDeployed" />

  <DeploymentLogsModal v-if="logsModal.deployment" :deployment="logsModal.deployment" @close="logsModal.deployment = null" />
</template>

<script>
import { mapStores, mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import deploymentsService from '@/services/deploymentsService'
import adminSettingsService from '@/services/adminSettingsService'
import StatusBadge from '@/components/StatusBadge.vue'
import TypeBadge from '@/components/TypeBadge.vue'
import UserBadge from '@/components/UserBadge.vue'
import DeployModal from '@/components/DeployModal.vue'
import DeploymentLogsModal from '@/components/DeploymentLogsModal.vue'
import DeploymentTable from '@/components/DeploymentTable.vue'
import HostEditForm from '@/components/HostEditForm.vue'
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon, EditIcon, RefreshIcon } from '@/components/icons'

export default {
  components: { StatusBadge, TypeBadge, UserBadge, DeployModal, DeploymentLogsModal, DeploymentTable, HostEditForm, RocketIcon, PackageIcon, TruckIcon, TerminalIcon, EditIcon, RefreshIcon },
  computed: {
    ...mapStores(useToastStore),
    ...mapState(useAuthStore, ['accessToken']),
    displayDeployment() {
      if (this.currentDeploymentId) {
        return this.deploymentHistory.find(d => d.id === this.currentDeploymentId) || this.activeDeployment
      }
      return this.viewedDeployment
    },
  },
  data() {
    return {
      host: null,
      defaultDeployCommand: '',
      activeTab: 'logs',
      tabs: [{ id: 'logs', label: 'Logs' }, { id: 'history', label: 'Historique' }, { id: 'details', label: 'Détails' }],
      modal: { show: false, type: 'DEPLOY' },
      logsModal: { deployment: null },
      logContent: '',
      currentDeploymentId: null,
      activeDeployment: null,
      viewedDeployment: null,
      deploymentHistory: [],
      histLoading: false,
      tlogLines: [],
      tlogActive: false,
      _sseSource: null,
      _eventSrc: null,
      _tlogSse: null,
      _destroyed: false,
    }
  },
  mounted() {
    this.loadHost()
      .then(() => this.loadHistory())
      .then(() => {
        if (this._destroyed) return
        adminSettingsService.get().then(res => {
          this.defaultDeployCommand = res.data.settings?.default_deploy_command || ''
        }).catch(() => {})
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
      })
  },
  unmounted() {
    this._destroyed = true
    if (this._sseSource) this._sseSource.close()
    if (this._eventSrc) this._eventSrc.close()
    if (this._tlogSse) this._tlogSse.close()
  },
  methods: {
    startTlog() {
      if (this._tlogSse) this._tlogSse.close()
      this.tlogActive = true
      const url = hostsService.getTlogStreamUrl(this.$route.params.id, this.accessToken)
      const src = new EventSource(url)
      
      src.addEventListener('log', e => {
        this.tlogLines.push(e.data)
        if (this.tlogLines.length > 500) this.tlogLines.shift()
        this.$nextTick(() => {
          if (this.$refs.tlogEl) this.$refs.tlogEl.scrollTop = this.$refs.tlogEl.scrollHeight
        })
      })
      
      src.onerror = () => {
        this.stopTlog()
        this.toastStore.error('Erreur de connexion au flux tlog')
      }
      
      this._tlogSse = src
    },
    stopTlog() {
      if (this._tlogSse) this._tlogSse.close()
      this._tlogSse = null
      this.tlogActive = false
    },
    loadHost() {
      return hostsService.getById(this.$route.params.id).then(res => {
        this.host = res.data
      })
    },
    loadHistory() {
      this.histLoading = true
      return deploymentsService.getHistory(this.$route.params.id).then(res => {
        this.deploymentHistory = res.data.content
        const inProgress = this.deploymentHistory.find(d => d.status === 'IN_PROGRESS')
        if (inProgress) {
          this.startSse(inProgress.id)
        } else {
          this.currentDeploymentId = null
          this.activeDeployment = null
          if (!this.viewedDeployment && this.deploymentHistory.length) {
            const last = this.deploymentHistory[0]
            this.viewedDeployment = last
            this.logContent = last.logs || ''
          }
        }
      }).finally(() => {
        this.histLoading = false
      })
    },
    viewDeployment(dep) {
      this.logsModal.deployment = dep
    },
    formatDate(d) {
      if (!d) return '—'
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    startSse(deploymentId) {
      if (this._destroyed) return
      this.currentDeploymentId = deploymentId
      this.activeDeployment = { id: deploymentId }
      this.viewedDeployment = null
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
    cancelDeployment() {
      if (!this.currentDeploymentId) return
      deploymentsService.cancel(this.currentDeploymentId).then(() => {
        this.toastStore.info('Déploiement annulé')
        this.loadHistory()
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur')
      })
    },
    openModal(type) {
      this.modal = { show: true, type }
    },
    onDeployed(deployment) {
      this.modal.show = false
      this.toastStore.success('Déploiement lancé')
      this.logContent = ''
      this.viewedDeployment = null
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
