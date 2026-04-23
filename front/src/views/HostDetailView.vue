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
      <div v-if="!host" class="text-center py-20 text-gray-400">Chargement...</div>
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

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import StatusBadge from '@/components/StatusBadge.vue'
import DeployModal from '@/components/DeployModal.vue'
import DeploymentTable from '@/components/DeploymentTable.vue'
import HostEditForm from '@/components/HostEditForm.vue'
import { RocketIcon, PackageIcon, TruckIcon, TerminalIcon } from '@/components/icons'

const route = useRoute()
const toastStore = useToastStore()
const host = ref(null)
const activeTab = ref('logs')
const tabs = [{ id: 'logs', label: 'Logs' }, { id: 'history', label: 'Historique' }, { id: 'details', label: 'Détails' }]
const modal = ref({ show: false, type: 'DEPLOY' })
const logContent = ref('')
const logEl = ref(null)
const currentDeploymentId = ref(null)
const activeDeployment = ref(null)
const deploymentHistory = ref([])
const histLoading = ref(false)
let sseSource = null

async function loadHost() {
  const res = await api.get(`/hosts/${route.params.id}`)
  host.value = res.data
}

async function loadHistory() {
  histLoading.value = true
  try {
    const res = await api.get(`/deployments?hostId=${route.params.id}&size=20`)
    deploymentHistory.value = res.data.content
    const inProgress = deploymentHistory.value.find(d => d.status === 'IN_PROGRESS')
    if (inProgress) startSse(inProgress.id)
    else { currentDeploymentId.value = null; activeDeployment.value = null }
  } finally {
    histLoading.value = false
  }
}

function startSse(deploymentId) {
  currentDeploymentId.value = deploymentId
  activeDeployment.value = { id: deploymentId }
  if (sseSource) sseSource.close()
  sseSource = new EventSource(`/api/deployments/${deploymentId}/logs`)
  sseSource.addEventListener('log', e => {
    logContent.value += e.data
    if (logEl.value) logEl.value.scrollTop = logEl.value.scrollHeight
  })
  sseSource.addEventListener('end', () => {
    sseSource.close()
    activeDeployment.value = null
    loadHistory()
    loadHost()
  })
}

async function cancelDeployment() {
  if (!currentDeploymentId.value) return
  try {
    await api.post(`/deployments/${currentDeploymentId.value}/cancel`)
    toastStore.info('Déploiement annulé')
    loadHistory()
  } catch (e) {
    toastStore.error(e.response?.data?.error || 'Erreur')
  }
}

function openModal(type) {
  modal.value = { show: true, type }
}

function onDeployed() {
  modal.value.show = false
  toastStore.success('Déploiement lancé')
  logContent.value = ''
  activeTab.value = 'logs'
  setTimeout(loadHistory, 500)
}

onMounted(async () => {
  await loadHost()
  await loadHistory()
})

onUnmounted(() => { if (sseSource) sseSource.close() })
</script>
