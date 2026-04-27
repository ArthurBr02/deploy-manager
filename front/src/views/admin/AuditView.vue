<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Logs d'audit</h1>
    </header>
    <div class="flex-1 overflow-auto p-4 lg:p-6">
      <div v-if="loading && !logs.length" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="bg-white border border-warm-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-left min-w-[640px]">
            <thead class="bg-warm-muted border-b border-warm-border">
              <tr>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Date</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Entité</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Action</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Utilisateur</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide hidden sm:table-cell">Ancienne Valeur</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide hidden sm:table-cell">Nouvelle Valeur</th>
                <th class="py-3 px-3 lg:px-4 font-medium text-xs text-gray-500 uppercase tracking-wide text-right">Détail</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id"
                class="border-b border-warm-border/50 hover:bg-warm-muted/40 transition-colors">
                <td class="py-3 px-3 lg:px-4 whitespace-nowrap text-gray-500 text-xs">{{ formatDate(log.createdAt) }}</td>
                <td class="py-3 px-3 lg:px-4">
                  <component :is="entityRoute(log) ? 'RouterLink' : 'span'"
                    v-bind="entityRoute(log) ? { to: entityRoute(log), class: 'font-medium text-accent hover:underline' } : { class: 'font-medium text-gray-900' }">
                    {{ log.entityName }}
                  </component>
                  <div class="text-[10px] text-gray-400 font-mono truncate max-w-[120px] lg:max-w-[160px]">{{ log.entityId }}</div>
                </td>
                <td class="py-3 px-3 lg:px-4 whitespace-nowrap">
                  <span :class="getActionClass(log.action)" class="px-2 py-0.5 rounded text-[10px] font-bold uppercase">
                    {{ actionLabel(log.action) }}
                  </span>
                </td>
                <td class="py-3 px-3 lg:px-4 text-gray-500">
                  <UserBadge :user="{
                    id: log.userId,
                    firstName: log.userFirstName,
                    lastName: log.userLastName,
                    email: log.userEmail,
                    avatar: log.userAvatar
                  }" />
                </td>
                <td class="py-3 px-3 lg:px-4 hidden sm:table-cell">
                  <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-xs text-gray-400" :title="log.oldValue">
                    {{ log.oldValue ? truncate(log.oldValue) : '—' }}
                  </span>
                </td>
                <td class="py-3 px-3 lg:px-4 hidden sm:table-cell">
                  <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-xs text-gray-600" :title="log.newValue">
                    {{ log.newValue ? truncate(log.newValue) : '—' }}
                  </span>
                </td>
                <td class="py-3 px-3 lg:px-4 text-right">
                  <button @click="openDetail(log)" title="Voir le détail"
                    class="p-1.5 rounded-md hover:bg-warm-muted text-gray-400 hover:text-accent transition-colors">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                    </svg>
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && !logs.length">
                <td colspan="7" class="py-20 text-center text-gray-400 italic">Aucun log d'audit trouvé</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="totalPages > 1" class="mt-4 flex justify-between items-center text-sm text-gray-500">
        <span>Page {{ page + 1 }} sur {{ totalPages }}</span>
        <div class="flex gap-2">
          <button @click="prevPage" :disabled="page === 0"
            class="px-3 py-1 border border-warm-border rounded-md hover:bg-white disabled:opacity-30 transition-colors">
            Précédent
          </button>
          <button @click="nextPage" :disabled="page >= totalPages - 1"
            class="px-3 py-1 border border-warm-border rounded-md hover:bg-white disabled:opacity-30 transition-colors">
            Suivant
          </button>
        </div>
      </div>
    </div>

    <AuditDetailModal :show="!!selectedLog" :log="selectedLog" @close="selectedLog = null" />
  </div>
</template>

<script>
import adminAuditService from '@/services/adminAuditService'
import UserBadge from '@/components/UserBadge.vue'
import AuditDetailModal from '@/components/AuditDetailModal.vue'

const ACTION_CLASSES = {
  CREATE: 'bg-green-100 text-green-700',
  UPDATE: 'bg-blue-100 text-blue-700',
  DELETE: 'bg-red-100 text-red-700',
  TERMINAL_CONNECT: 'bg-purple-100 text-purple-700',
  TERMINAL_DISCONNECT: 'bg-gray-100 text-gray-600',
  TERMINAL_COMMAND: 'bg-orange-100 text-orange-700',
}

const ACTION_LABELS = {
  TERMINAL_CONNECT: 'Connexion',
  TERMINAL_DISCONNECT: 'Déconnexion',
  TERMINAL_COMMAND: 'Commande',
}

const ENTITY_ROUTES = {
  Host: (id) => `/hosts/${id}`,
  Terminal: (id) => `/hosts/${id}`,
  User: (id) => `/users/${id}`,
}

export default {
  components: { UserBadge, AuditDetailModal },
  data() {
    return {
      logs: [],
      loading: false,
      page: 0,
      totalPages: 0,
      selectedLog: null,
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
    openDetail(log) {
      this.selectedLog = log
    },
    formatDate(d) {
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    getActionClass(action) {
      return ACTION_CLASSES[action] || 'bg-gray-100 text-gray-700'
    },
    actionLabel(action) {
      return ACTION_LABELS[action] || action
    },
    entityRoute(log) {
      if (!log.entityId) return null
      const fn = ENTITY_ROUTES[log.entityName]
      return fn ? fn(log.entityId) : null
    },
    truncate(str) {
      if (!str) return ''
      try {
        const obj = JSON.parse(str)
        const keys = Object.keys(obj)
        if (keys.length === 0) return str
        const first = keys[0]
        return `${first}: ${obj[first]}`
      } catch {
        return str.length > 40 ? str.slice(0, 40) + '…' : str
      }
    },
    nextPage() {
      if (this.page < this.totalPages - 1) { this.page++; this.load() }
    },
    prevPage() {
      if (this.page > 0) { this.page--; this.load() }
    },
  },
}
</script>
