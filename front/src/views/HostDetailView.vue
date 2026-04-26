<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-4 lg:px-6 gap-4 flex-shrink-0 overflow-x-auto no-scrollbar">
      <div class="flex items-center gap-2 text-sm text-gray-500 whitespace-nowrap">
        <RouterLink to="/hosts" class="hover:text-gray-700">Hôtes</RouterLink>
        <span>/</span>
        <span class="text-gray-900 font-medium">{{ host?.name }}</span>
      </div>
      <div class="flex-1 min-w-[1rem]" />
      <div v-if="activeDeployment" class="flex items-center gap-2 whitespace-nowrap">
        <span class="text-xs text-status-progress sm:text-sm">En cours...</span>
        <button @click="cancelDeployment" class="text-xs sm:text-sm text-red-500 hover:text-red-700 border border-red-200 px-2 sm:px-3 py-1 rounded-md">Annuler</button>
      </div>
    </header>

    <div class="flex-1 overflow-auto p-4 lg:p-6">
      <div v-if="!host" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="max-w-7xl mx-auto space-y-6">
        <div class="space-y-6">
          <!-- Info card -->
          <div class="bg-white border border-warm-border rounded-xl p-4 sm:p-5 shadow-sm">
            <div class="flex items-center justify-between mb-4">
              <div class="min-w-0">
                <h2 class="text-lg font-semibold text-gray-900 truncate">{{ host.name }}</h2>
                <div class="text-sm text-gray-400 font-mono truncate">{{ host.ip }}</div>
              </div>
              <StatusBadge :status="host.lastDeploymentStatus || 'NEVER'" class="shrink-0" />
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
              <div class="min-w-0"><span class="text-gray-400">Domaine</span><div class="font-medium truncate">{{ host.domain || '—' }}</div></div>
              <div class="min-w-0"><span class="text-gray-400">Timeout</span><div class="font-medium truncate">{{ host.defaultTimeout != null ? host.defaultTimeout + ' min' : 'Défaut' }}</div></div>
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

              <!-- SQL Dump -->
              <button v-if="host.isDumpAvailable" @click="downloadDump" :disabled="downloading" class="flex items-center gap-1.5 px-3 py-1.5 border border-green-200 text-green-600 rounded-md text-sm hover:bg-green-50 disabled:opacity-50">
                <DownloadIcon class="w-3.5 h-3.5" /> {{ downloading ? 'Téléchargement...' : 'Télécharger le dump' }}
              </button>
              <button v-else @click="requestDump" :disabled="requesting" class="flex items-center gap-1.5 px-3 py-1.5 border border-amber-200 text-amber-600 rounded-md text-sm hover:bg-amber-50 disabled:opacity-50">
                <DatabaseIcon class="w-3.5 h-3.5" /> {{ requesting ? 'Demande envoyée' : 'Demander un dump' }}
              </button>

              <RouterLink v-if="host.canEdit" :to="`/hosts/${host.id}/edit`" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm hover:bg-warm-muted sm:ml-auto">
                <EditIcon class="w-3.5 h-3.5" /> Modifier
              </RouterLink>
            </div>
          </div>

          <!-- Tabs -->
          <div class="border-b border-warm-border flex gap-0 overflow-x-auto no-scrollbar">
            <button v-for="tab in tabs" :key="tab.id" @click="activeTab = tab.id"
              class="px-4 py-2.5 text-sm font-medium border-b-2 transition-colors flex items-center gap-1.5 whitespace-nowrap"
              :class="activeTab === tab.id ? 'border-accent text-accent' : 'border-transparent text-gray-500 hover:text-gray-700'">
              {{ tab.label }}
              <span v-if="tab.id === 'logs' && tlogActive" class="w-1.5 h-1.5 rounded-full bg-green-500 inline-block animate-pulse"></span>
            </button>
          </div>

          <!-- Logs & Tlog Content (Split View) -->
          <div v-if="activeTab === 'logs'" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- Deployment Logs -->
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider">Logs Déploiement</h3>
                <div v-if="currentDeploymentId" class="flex items-center gap-1.5 text-status-progress text-[10px] animate-pulse font-medium uppercase">
                  <span class="w-1.5 h-1.5 rounded-full bg-status-progress inline-block"></span>
                  En cours…
                </div>
              </div>

              <div v-if="!viewedDeployment && !currentDeploymentId" class="h-[300px] sm:h-[450px] flex flex-col items-center justify-center text-gray-400 border-2 border-dashed border-warm-border rounded-xl bg-white">
                <TerminalIcon class="w-8 h-8 mb-2 opacity-30" />
                <p class="text-sm">Aucun déploiement disponible</p>
              </div>
              <div v-else class="space-y-3">
                <div class="bg-white border border-warm-border rounded-xl px-4 py-2.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] shadow-sm">
                  <UserBadge v-if="displayDeployment" :user="{ id: displayDeployment.userId, firstName: displayDeployment.userFirstName, lastName: displayDeployment.userLastName, avatar: displayDeployment.userAvatar }" />
                  <span class="text-gray-400">{{ formatDate(displayDeployment?.createdAt) }}</span>
                  <div class="flex gap-2">
                    <TypeBadge v-if="displayDeployment?.type" :type="displayDeployment.type" />
                    <StatusBadge v-if="displayDeployment?.status" :status="displayDeployment.status" />
                  </div>
                </div>
                <div class="dm-term h-[300px] sm:h-[400px] overflow-auto text-[11px]" ref="logEl">{{ logContent }}</div>
              </div>
            </div>

            <!-- Logs de l'application (Real-time App Logs) -->
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider">Logs de l'application</h3>
                  <span v-if="tlogActive" class="w-1.5 h-1.5 rounded-full bg-green-500 inline-block animate-pulse"></span>
                </div>
                <div class="flex items-center gap-2">
                  <button v-if="tlogLines.length" @click="tlogLines = []" class="text-[10px] text-gray-400 hover:text-gray-600 underline mr-2">Effacer</button>
                  <button v-if="!tlogActive" @click="startTlog" class="text-[10px] px-2 py-0.5 bg-accent text-white rounded hover:bg-accent-hover font-medium">Démarrer</button>
                  <button v-else @click="stopTlog" class="text-[10px] px-2 py-0.5 bg-red-500 text-white rounded hover:bg-red-600 font-medium">Arrêter</button>
                </div>
              </div>

              <div class="bg-[#1a1a1a] border border-black rounded-xl overflow-hidden shadow-lg flex flex-col h-[300px] sm:h-[450px]">
                <div class="flex-1 p-3 overflow-auto font-mono text-[10px] leading-relaxed selection:bg-accent/30" ref="tlogEl">
                  <div v-for="(line, i) in tlogLines" :key="i" class="text-gray-300 border-l border-white/5 pl-2 mb-0.5 whitespace-pre-wrap break-all hover:bg-white/5">{{ line }}</div>
                  
                  <div v-if="!tlogActive && !tlogLines.length" class="h-full flex flex-col items-center justify-center text-gray-500 italic text-center p-6">
                    <RefreshIcon class="w-8 h-8 mb-3 opacity-20 text-accent animate-pulse" />
                    <p>Démarrez le flux pour voir les logs applicatifs en direct.</p>
                  </div>
                  
                  <div v-if="tlogActive && !tlogLines.length" class="h-full flex flex-col items-center justify-center text-gray-400 italic">
                    <div class="w-4 h-4 border-2 border-accent border-t-transparent rounded-full animate-spin mb-3"></div>
                    Connexion au flux...
                  </div>
                </div>
                <div class="h-6 bg-black/40 border-t border-white/5 px-3 flex items-center justify-between text-[9px] text-gray-500 uppercase tracking-tight">
                  <span>{{ tlogActive ? 'Connecté' : 'Déconnecté' }}</span>
                  <span>{{ tlogLines.length }} lignes</span>
                </div>
              </div>
            </div>
          </div>

          <!-- History -->
          <div v-if="activeTab === 'history'">
            <DeploymentTable :deployments="deploymentHistory" :loading="histLoading" :hide-host="true" @view="viewDeployment" />
          </div>

          <!-- Details (read-only) -->
          <div v-if="activeTab === 'details'">
            <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
              <h3 class="font-semibold text-gray-900">Détails de l'hôte</h3>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
                  <div class="w-full border border-warm-border rounded-md px-3 py-2 text-sm bg-warm-muted text-gray-800">{{ host.name || '—' }}</div>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">IP</label>
                  <div class="w-full border border-warm-border rounded-md px-3 py-2 text-sm bg-warm-muted font-mono text-gray-800">{{ host.ip || '—' }}</div>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Domaine</label>
                  <div class="w-full border border-warm-border rounded-md px-3 py-2 text-sm bg-warm-muted font-mono text-gray-800">{{ host.domain || '—' }}</div>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Timeout par défaut (min)</label>
                  <div class="w-full border border-warm-border rounded-md px-3 py-2 text-sm bg-warm-muted text-gray-800">{{ host.defaultTimeout != null ? host.defaultTimeout + ' min' : 'Défaut' }}</div>
                </div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Commande de déploiement</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800 whitespace-pre-wrap min-h-[3rem]">{{ host.deploymentCommand || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Commande de génération</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800 whitespace-pre-wrap min-h-[3rem]">{{ host.generateCommand || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Commande de livraison</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800 whitespace-pre-wrap min-h-[3rem]">{{ host.deliverCommand || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Commande Tlog</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800 whitespace-pre-wrap min-h-[3rem]">{{ host.tlogCommand || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Commande de Rollback</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800 whitespace-pre-wrap min-h-[3rem]">{{ host.rollbackCommand || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">URL Healthcheck</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800">{{ host.healthcheckUrl || '—' }}</div>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Dossier de dumps SQL</label>
                <div class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono bg-warm-muted text-gray-800">{{ host.dumpFolder || '—' }}</div>
              </div>
              <div v-if="host.canEdit" class="flex justify-end">
                <RouterLink :to="`/hosts/${host.id}/edit`" class="flex items-center gap-1.5 px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover">
                  <EditIcon class="w-3.5 h-3.5" /> Modifier
                </RouterLink>
              </div>
            </div>
          </div>
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
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon, EditIcon, RefreshIcon, DatabaseIcon, DownloadIcon } from '@/components/icons'

export default {
  components: { StatusBadge, TypeBadge, UserBadge, DeployModal, DeploymentLogsModal, DeploymentTable, RocketIcon, PackageIcon, TruckIcon, TerminalIcon, EditIcon, RefreshIcon, DatabaseIcon, DownloadIcon },
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
      downloading: false,
      requesting: false,
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
      this._tlogEnded = false
      const url = hostsService.getTlogStreamUrl(this.$route.params.id, this.accessToken)
      const src = new EventSource(url)

      src.addEventListener('log', e => {
        this.tlogLines.push(e.data)
        if (this.tlogLines.length > 500) this.tlogLines.shift()
        this.$nextTick(() => {
          if (this.$refs.tlogEl) this.$refs.tlogEl.scrollTop = this.$refs.tlogEl.scrollHeight
        })
      })

      // Fin propre du flux (processus terminé côté serveur)
      src.addEventListener('end', () => {
        this._tlogEnded = true
        src.close()
        this._tlogSse = null
        this.tlogActive = false
      })

      // Événement SSE "error" envoyé explicitement par le serveur (distinct des erreurs réseau)
      src.addEventListener('appError', e => {
        this.tlogLines.push(`[ERREUR] ${e.data}`)
        this._tlogEnded = true
        src.close()
        this._tlogSse = null
        this.tlogActive = false
      })

      // Erreur réseau / connexion perdue — ne pas déclencher si le flux s'est déjà fermé proprement
      src.onerror = () => {
        if (this._tlogEnded || src.readyState === EventSource.CLOSED) return
        this.stopTlog()
        this.toastStore.error('Connexion au flux de logs perdue')
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
    downloadDump() {
      this.downloading = true
      hostsService.downloadDump(this.host.id).then(res => {
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `${this.host.name}.sql`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        this.toastStore.success('Téléchargement démarré')
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur lors du téléchargement')
      }).finally(() => {
        this.downloading = false
      })
    },
    requestDump() {
      this.requesting = true
      hostsService.requestDump(this.host.id).then(() => {
        this.toastStore.success('Demande envoyée aux administrateurs')
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur lors de la demande')
        this.requesting = false
      })
    },
  },
}
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
