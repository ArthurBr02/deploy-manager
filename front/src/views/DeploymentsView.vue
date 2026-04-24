<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Déploiements</h1>
      <div class="flex-1" />
      <button @click="exportCsv" class="flex items-center gap-1.5 px-3 py-1.5 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted">
        <DownloadIcon class="w-3.5 h-3.5" /> Exporter CSV
      </button>
    </header>

    <div class="flex-1 overflow-auto p-6 space-y-4">
      <p class="text-sm text-gray-400 -mt-2">Historique global — tous les hôtes, tous les utilisateurs.</p>

      <!-- Filters -->
      <div class="bg-white border border-warm-border rounded-xl px-4 py-3 flex flex-wrap gap-2.5 items-center">
        <div class="relative min-w-52 flex-1">
          <SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" />
          <input
            v-model="filters.search"
            class="w-full border border-warm-border rounded-md pl-8 pr-3 py-1.5 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            placeholder="Rechercher (ID, hôte, utilisateur…)"
          />
        </div>
        <select v-model="filters.hostId" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="">Tous les hôtes</option>
          <option v-for="h in hosts" :key="h.id" :value="h.id">{{ h.name }}</option>
        </select>
        <select v-model="filters.status" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="">Tous les statuts</option>
          <option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <select v-model="filters.type" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="">Tous les types</option>
          <option value="DEPLOY">Déployer</option>
          <option value="GENERATE">Générer</option>
          <option value="DELIVER">Livrer</option>
        </select>
        <select v-model="filters.period" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="24h">Dernières 24 h</option>
          <option value="7d">7 derniers jours</option>
          <option value="30d">30 derniers jours</option>
          <option value="">Depuis toujours</option>
        </select>
        <button @click="resetFilters" class="px-3 py-1.5 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted text-gray-500">
          Réinitialiser
        </button>
      </div>

      <!-- Stats strip -->
      <div class="grid grid-cols-4 gap-3">
        <div v-for="s in statsCards" :key="s.label" class="bg-white border border-warm-border rounded-xl p-4">
          <div class="text-xs text-gray-400 uppercase tracking-wide">{{ s.label }}</div>
          <div class="text-2xl font-semibold mt-1 tracking-tight" :style="s.color ? `color: ${s.color}` : ''">
            {{ stats ? s.value(stats) : '—' }}
          </div>
          <div class="text-xs text-gray-400 mt-0.5">{{ stats ? s.sub(stats) : '' }}</div>
        </div>
      </div>

      <!-- Table -->
      <DeploymentTable :deployments="deployments" :loading="loading" @view="viewDeployment" />

      <!-- Pagination -->
      <div class="flex justify-between items-center text-xs text-gray-400">
        <span v-if="stats">{{ paginationLabel }}</span>
        <span v-else></span>
        <div v-if="total > 1" class="flex items-center gap-1">
          <button @click="page--" :disabled="page === 0" class="px-3 py-1.5 text-sm border border-warm-border rounded-md bg-white hover:bg-warm-muted disabled:opacity-40">← Précédent</button>
          <span class="px-2 text-sm text-gray-500">Page <strong>{{ page + 1 }}</strong> / {{ total }}</span>
          <button @click="page++" :disabled="page >= total - 1" class="px-3 py-1.5 text-sm border border-warm-border rounded-md bg-accent text-white hover:bg-accent-hover disabled:opacity-40">Suivant →</button>
        </div>
      </div>
    </div>
  </div>

  <DeploymentLogsModal v-if="logsModal.deployment" :deployment="logsModal.deployment" @close="logsModal.deployment = null" />
</template>

<script>
import deploymentsService from '@/services/deploymentsService'
import hostsService from '@/services/hostsService'
import DeploymentTable from '@/components/DeploymentTable.vue'
import DeploymentLogsModal from '@/components/DeploymentLogsModal.vue'
import { SearchIcon, DownloadIcon } from '@/components/icons'

export default {
  components: { DeploymentTable, DeploymentLogsModal, SearchIcon, DownloadIcon },
  data() {
    return {
      deployments: [],
      hosts: [],
      loading: false,
      logsModal: { deployment: null },
      page: 0,
      total: 1,
      totalElements: 0,
      stats: null,
      filters: { search: '', hostId: '', status: '', type: '', period: '7d' },
      statuses: [
        { value: 'SUCCESS', label: 'Succès' },
        { value: 'FAILURE', label: 'Échec' },
        { value: 'IN_PROGRESS', label: 'En cours' },
        { value: 'CANCELLED', label: 'Annulé' },
      ],
    }
  },
  computed: {
    periodLabel() {
      const map = { '24h': 'Dernières 24 h', '7d': '7 derniers jours', '30d': '30 derniers jours', '': 'Depuis toujours' }
      return map[this.filters.period] ?? ''
    },
    paginationLabel() {
      const from = this.page * 20 + 1
      const to = Math.min((this.page + 1) * 20, this.totalElements)
      return `${from} – ${to} sur ${this.totalElements} déploiements`
    },
    statsCards() {
      return [
        { label: 'Total', value: s => s.total, sub: () => this.periodLabel },
        { label: 'Réussis', value: s => s.success, sub: s => `${s.total > 0 ? Math.round(s.success / s.total * 100) : 0} % de succès`, color: 'var(--color-status-success, #16a34a)' },
        { label: 'En échec', value: s => s.failure, sub: s => `${s.total > 0 ? Math.round(s.failure / s.total * 100) : 0} %`, color: 'var(--color-status-failure, #dc2626)' },
        { label: 'Durée médiane', value: s => s.medianDuration ?? '—', sub: () => '' },
      ]
    },
  },
  watch: {
    page() { this.load(); this.loadStats() },
    filters: {
      deep: true,
      handler() { this.page = 0; this.load(); this.loadStats() },
    },
  },
  mounted() {
    this.load()
    this.loadStats()
    this.loadHosts()
  },
  methods: {
    load() {
      this.loading = true
      const params = { page: this.page, size: 20 }
      if (this.filters.search) params.search = this.filters.search
      if (this.filters.hostId) params.hostId = this.filters.hostId
      if (this.filters.status) params.status = this.filters.status
      if (this.filters.type) params.type = this.filters.type
      if (this.filters.period) params.period = this.filters.period
      deploymentsService.list(params).then(res => {
        this.deployments = res.data.content
        this.total = res.data.totalPages
        this.totalElements = res.data.totalElements ?? 0
      }).finally(() => {
        this.loading = false
      })
    },
    loadStats() {
      const params = {}
      if (this.filters.period) params.period = this.filters.period
      if (this.filters.hostId) params.hostId = this.filters.hostId
      if (this.filters.type) params.type = this.filters.type
      deploymentsService.getStats(params).then(res => {
        this.stats = res.data
      }).catch(() => {
        this.stats = null
      })
    },
    loadHosts() {
      hostsService.getAll().then(res => {
        this.hosts = res.data.content ?? res.data
      }).catch(() => {
        this.hosts = []
      })
    },
    resetFilters() {
      this.filters.search = ''
      this.filters.hostId = ''
      this.filters.status = ''
      this.filters.type = ''
      this.filters.period = '7d'
      this.page = 0
    },
    viewDeployment(dep) {
      this.logsModal.deployment = dep
    },
    exportCsv() {
      const params = {}
      if (this.filters.search) params.search = this.filters.search
      if (this.filters.hostId) params.hostId = this.filters.hostId
      if (this.filters.status) params.status = this.filters.status
      if (this.filters.type) params.type = this.filters.type
      if (this.filters.period) params.period = this.filters.period
      deploymentsService.exportCsv(params).then(res => {
        const url = URL.createObjectURL(res.data)
        const a = document.createElement('a')
        a.href = url
        a.download = 'deployments.csv'
        a.click()
        URL.revokeObjectURL(url)
      })
    },
  },
}
</script>
