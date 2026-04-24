<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Audit Logs</h1>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div class="max-w-6xl mx-auto">
        <div class="bg-white border border-warm-border rounded-xl shadow-sm overflow-hidden">
          <table class="w-full text-sm text-left">
            <thead class="bg-warm-muted/50 border-b border-warm-border text-gray-500 uppercase text-[10px] font-bold tracking-wider">
              <tr>
                <th class="px-6 py-3">Date</th>
                <th class="px-6 py-3">Entité</th>
                <th class="px-6 py-3">Action</th>
                <th class="px-6 py-3">Utilisateur</th>
                <th class="px-6 py-3">Ancienne Valeur</th>
                <th class="px-6 py-3">Nouvelle Valeur</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-warm-border">
              <tr v-if="loading && !logs.length" class="text-center py-10">
                <td colspan="6" class="px-6 py-10">Chargement...</td>
              </tr>
              <tr v-else v-for="log in logs" :key="log.id" class="hover:bg-warm-muted/20 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap text-gray-500">{{ formatDate(log.createdAt) }}</td>
                <td class="px-6 py-4">
                  <span class="font-medium text-gray-900">{{ log.entityName }}</span>
                  <div class="text-[10px] text-gray-400 font-mono">{{ log.entityId }}</div>
                </td>
                <td class="px-6 py-4">
                  <span :class="getActionClass(log.action)" class="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase">
                    {{ log.action }}
                  </span>
                </td>
                <td class="px-6 py-4 text-gray-500">{{ log.userId || 'Système' }}</td>
                <td class="px-6 py-4 truncate max-w-xs font-mono text-xs text-gray-400" :title="log.oldValue">{{ log.oldValue || '—' }}</td>
                <td class="px-6 py-4 truncate max-w-xs font-mono text-xs text-gray-600" :title="log.newValue">{{ log.newValue || '—' }}</td>
              </tr>
              <tr v-if="!loading && !logs.length">
                <td colspan="6" class="px-6 py-20 text-center text-gray-400 italic">Aucun log d'audit trouvé</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="totalPages > 1" class="mt-4 flex justify-between items-center text-sm text-gray-500">
          <span>Page {{ page + 1 }} sur {{ totalPages }}</span>
          <div class="flex gap-2">
            <button @click="prevPage" :disabled="page === 0" class="px-3 py-1 border border-warm-border rounded-md hover:bg-white disabled:opacity-30">Précédent</button>
            <button @click="nextPage" :disabled="page >= totalPages - 1" class="px-3 py-1 border border-warm-border rounded-md hover:bg-white disabled:opacity-30">Suivant</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import adminAuditService from '@/services/adminAuditService'

export default {
  data() {
    return {
      logs: [],
      loading: false,
      page: 0,
      totalPages: 0,
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      adminAuditService.list(this.page).then(res => {
        this.logs = res.data.content
        this.totalPages = res.data.totalPages
      }).finally(() => {
        this.loading = false
      })
    },
    formatDate(d) {
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    getActionClass(action) {
      return {
        CREATE: 'bg-green-100 text-green-700',
        UPDATE: 'bg-blue-100 text-blue-700',
        DELETE: 'bg-red-100 text-red-700',
      }[action] || 'bg-gray-100 text-gray-700'
    },
    nextPage() {
      if (this.page < this.totalPages - 1) {
        this.page++
        this.load()
      }
    },
    prevPage() {
      if (this.page > 0) {
        this.page--
        this.load()
      }
    },
  },
}
</script>
