<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Hôtes</h1>
      <div class="flex-1" />
      <div class="relative">
        <SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" />
        <input v-model="filters.search" placeholder="Rechercher..." class="w-full sm:w-52 pl-8 pr-3 py-1.5 text-sm border border-warm-border rounded-md outline-none focus:border-accent" />
      </div>
      <RouterLink to="/admin/hosts/new">
        <button class="flex items-center gap-1.5 bg-accent text-white px-3 py-1.5 rounded-md text-sm hover:bg-accent-hover">
          <PlusIcon class="w-3.5 h-3.5" /> Nouvel hôte
        </button>
      </RouterLink>
    </header>

    <div class="flex-1 overflow-auto p-6">
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else-if="!filtered.length" class="text-center py-20 border-2 border-dashed border-warm-border rounded-xl text-gray-400">
        <ServerIcon class="w-10 h-10 mx-auto mb-3 opacity-30" />
        <p>{{ filters.search ? 'Aucun hôte trouvé' : 'Aucun hôte' }}</p>
      </div>
      <div v-else class="bg-white border border-warm-border rounded-xl overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-warm-muted border-b border-warm-border">
            <tr>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Nom</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">IP</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Domaine</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Statut</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Timeout</th>
              <th class="py-3 px-4 w-20"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="host in filtered" :key="host.id" class="border-b border-warm-border/50 hover:bg-warm-muted/40">
              <td class="py-3 px-4 font-medium text-gray-900">{{ host.name }}</td>
              <td class="py-3 px-4 font-mono text-xs text-gray-500">{{ host.ip }}</td>
              <td class="py-3 px-4 text-gray-500">{{ host.domain || '—' }}</td>
              <td class="py-3 px-4">
                <StatusBadge :status="host.lastDeploymentStatus || 'NEVER'" />
              </td>
              <td class="py-3 px-4 text-gray-500 text-xs">{{ host.defaultTimeout != null ? host.defaultTimeout + ' min' : 'Défaut' }}</td>
              <td class="py-3 px-4">
                <div class="flex gap-2 justify-end">
                  <RouterLink :to="`/hosts/${host.id}/edit`" class="text-gray-400 hover:text-accent">
                    <EditIcon class="w-4 h-4" />
                  </RouterLink>
                  <button @click="deleteHost(host)" class="text-gray-400 hover:text-red-500">
                    <TrashIcon class="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import hostsService from '@/services/hostsService'
import StatusBadge from '@/components/StatusBadge.vue'
import { PlusIcon, EditIcon, TrashIcon, ServerIcon, SearchIcon } from '@/components/icons'
import { syncQuery } from '@/utils/query'

export default {
  components: { StatusBadge, PlusIcon, EditIcon, TrashIcon, ServerIcon, SearchIcon },
  computed: {
    ...mapStores(useToastStore),
    filtered() {
      return this.hosts
        .filter(h =>
          h.name?.toLowerCase().includes(this.filters.search.toLowerCase()) ||
          h.ip?.includes(this.filters.search) ||
          (h.domain && h.domain.toLowerCase().includes(this.filters.search.toLowerCase()))
        )
        .sort((a, b) => (a.name || '').localeCompare(b.name || '', 'fr'))
    }
  },
  data() {
    return {
      hosts: [],
      loading: true,
      filters: { search: '' }
    }
  },
  mounted() {
    syncQuery(this, {
      key: 'admin_hosts',
      defaultFilters: { search: '' },
      onUpdate: () => {}
    })
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      hostsService.getAll().then(res => {
        this.hosts = res.data
      }).finally(() => {
        this.loading = false
      })
    },
    deleteHost(host) {
      if (!confirm(`Supprimer l'hôte "${host.name}" ? Cette action est irréversible.`)) return
      hostsService.delete(host.id).then(() => {
        this.toastStore.success('Hôte supprimé')
        return this.load()
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur lors de la suppression')
      })
    },
  },
}
</script>
