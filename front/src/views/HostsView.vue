<template>
  <div class="flex flex-col h-full">
    <!-- Topbar -->
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-4 lg:px-6 gap-2 lg:gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900 hidden sm:block">Hôtes</h1>
      <div class="flex-1 sm:hidden"></div>
      <div class="relative flex-1 sm:flex-none">
        <SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" />
        <input v-model="search" placeholder="Rechercher..." class="w-full sm:w-52 pl-8 pr-3 py-1.5 text-sm border border-warm-border rounded-md outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
      <RouterLink v-if="authStore.isAdmin" to="/admin/hosts/new">
        <button class="flex items-center gap-1.5 bg-accent hover:bg-accent-hover text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors">
          <PlusIcon class="w-3.5 h-3.5" /> <span class="hidden xs:inline">Nouvel hôte</span>
        </button>
      </RouterLink>
    </header>

    <!-- Content -->
    <div class="flex-1 overflow-auto p-4 lg:p-6">
      <div v-if="loading" class="flex-1 flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else-if="!filtered.length" class="text-center py-20 border-2 border-dashed border-warm-border rounded-xl text-gray-400">
        <ServerIcon class="w-10 h-10 mx-auto mb-3 opacity-30" />
        <p>Aucun hôte accessible</p>
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <HostCard
          v-for="host in filtered"
          :key="host.id"
          :host="host"
          @deploy="openDeployModal(host, 'DEPLOY')"
          @generate="openDeployModal(host, 'GENERATE')"
          @deliver="openDeployModal(host, 'DELIVER')"
        />
      </div>
    </div>
  </div>

  <!-- Deploy Modal -->
  <DeployModal
    v-if="modal.show"
    :host="modal.host"
    :type="modal.type"
    :default-timeout="modal.defaultTimeout"
    :default-deploy-command="defaultDeployCommand"
    @close="modal.show = false"
    @deployed="onDeployed"
  />
</template>

<script>
import { mapStores, mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import adminSettingsService from '@/services/adminSettingsService'
import HostCard from '@/components/HostCard.vue'
import DeployModal from '@/components/DeployModal.vue'
import { SearchIcon, PlusIcon, ServerIcon } from '@/components/icons'

export default {
  components: { HostCard, DeployModal, SearchIcon, PlusIcon, ServerIcon },
  computed: {
    ...mapStores(useAuthStore, useToastStore),
    ...mapState(useAuthStore, ['accessToken']),
    filtered() {
      return this.hosts.filter(h =>
        h.name?.toLowerCase().includes(this.search.toLowerCase()) || h.ip?.includes(this.search)
      )
    },
  },
  data() {
    return {
      hosts: [],
      loading: true,
      search: '',
      modal: { show: false, host: null, type: 'DEPLOY', defaultTimeout: 10 },
      defaultDeployCommand: '',
      _eventSrc: null,
    }
  },
  mounted() {
    this.load()
    adminSettingsService.get().then(res => {
      this.defaultDeployCommand = res.data.settings?.default_deploy_command || ''
    }).catch(() => {})
    const src = new EventSource(`/api/deployments/events?token=${this.accessToken}`)
    src.addEventListener('deployment.status', () => { this.load() })
    this._eventSrc = src
  },
  unmounted() {
    if (this._eventSrc) { this._eventSrc.close(); this._eventSrc = null }
  },
  methods: {
    load() {
      hostsService.getAll().then(res => {
        this.hosts = res.data
      }).finally(() => {
        this.loading = false
      })
    },
    openDeployModal(host, type) {
      this.modal = { show: true, host, type, defaultTimeout: host.defaultTimeout ?? 10 }
    },
    onDeployed() {
      this.modal.show = false
      this.toastStore.success('Déploiement lancé')
    },
  },
}
</script>
