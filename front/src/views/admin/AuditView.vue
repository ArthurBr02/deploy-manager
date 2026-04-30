<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Logs d'audit</h1>
    </header>
    <div class="flex-1 overflow-auto p-4 lg:p-6 space-y-4">
      <!-- Filters -->
      <div class="bg-white border border-warm-border rounded-xl px-4 py-3 flex flex-wrap gap-2.5 items-center">
        <div class="relative min-w-full sm:min-w-52 flex-1">
          <SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" />
          <input
            v-model="filters.search"
            class="w-full border border-warm-border rounded-md pl-8 pr-3 py-1.5 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            placeholder="Rechercher (ID, snapshots…)"
          />
        </div>
        <select v-model="filters.userId" class="flex-1 sm:flex-none border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent max-w-[200px]">
          <option value="">Tous les utilisateurs</option>
          <option v-for="u in usersList" :key="u.id" :value="u.id">{{ u.firstName }} {{ u.lastName }}</option>
        </select>
        <select v-model="filters.entityName" class="flex-1 sm:flex-none border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="">Toutes les entités</option>
          <option value="Host">Hôtes</option>
          <option value="User">Utilisateurs</option>
          <option value="Terminal">Terminal</option>
          <option value="UserHostPermission">Permissions</option>
          <option value="AppConfig">Paramètres</option>
        </select>
        <select v-model="filters.action" class="flex-1 sm:flex-none border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
          <option value="">Toutes les actions</option>
          <option value="CREATE">Création</option>
          <option value="UPDATE">Modification</option>
          <option value="DELETE">Suppression</option>
          <option value="TERMINAL_CONNECT">Connexion SSH</option>
          <option value="TERMINAL_COMMAND">Commande SSH</option>
        </select>
        <button @click="resetFilters" class="w-full sm:w-auto px-3 py-1.5 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted text-gray-500">
          Réinitialiser
        </button>
      </div>

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
              <template v-for="item in processedLogs" :key="item.id">
                <!-- Single Log -->
                <tr v-if="item.type === 'single'"
                  class="border-b border-warm-border/50 hover:bg-warm-muted/40 transition-colors">
                  <td class="py-3 px-3 lg:px-4 whitespace-nowrap text-gray-500 text-xs">{{ formatDate(item.log.createdAt) }}</td>
                  <td class="py-3 px-3 lg:px-4">
                    <component :is="entityRoute(item.log) ? 'RouterLink' : 'span'"
                      v-bind="entityRoute(item.log) ? { to: entityRoute(item.log), class: 'font-medium text-accent hover:underline' } : { class: 'font-medium text-gray-900' }">
                      {{ item.log.entityName }}
                    </component>
                    <div class="text-[10px] text-gray-400 font-mono truncate max-w-[120px] lg:max-w-[160px]">{{ item.log.entityId }}</div>
                  </td>
                  <td class="py-3 px-3 lg:px-4 whitespace-nowrap">
                    <span :class="getActionClass(item.log.action)" class="px-2 py-0.5 rounded text-[10px] font-bold uppercase">
                      {{ actionLabel(item.log.action) }}
                    </span>
                  </td>
                  <td class="py-3 px-3 lg:px-4 text-gray-500">
                    <UserBadge :user="{
                      id: item.log.userId,
                      firstName: item.log.userFirstName,
                      lastName: item.log.userLastName,
                      email: item.log.userEmail,
                      avatar: item.log.userAvatar
                    }" />
                  </td>
                  <td class="py-3 px-3 lg:px-4 hidden sm:table-cell">
                    <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-xs text-gray-400" :title="item.log.oldValue">
                      {{ item.log.oldValue ? truncate(item.log.oldValue) : '—' }}
                    </span>
                  </td>
                  <td class="py-3 px-3 lg:px-4 hidden sm:table-cell">
                    <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-xs text-gray-600" :title="item.log.newValue">
                      {{ item.log.newValue ? truncate(item.log.newValue) : '—' }}
                    </span>
                  </td>
                  <td class="py-3 px-3 lg:px-4 text-right">
                    <button @click="openDetail(item.log)" title="Voir le détail"
                      class="p-1.5 rounded-md hover:bg-warm-muted text-gray-400 hover:text-accent transition-colors">
                      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                      </svg>
                    </button>
                  </td>
                </tr>

                <!-- Grouped Logs -->
                <template v-else>
                  <tr class="border-b border-warm-border/50 bg-warm-muted/10 hover:bg-warm-muted/30 transition-colors cursor-pointer"
                    @click="toggleGroup(item.contextId)">
                    <td class="py-3 px-3 lg:px-4 whitespace-nowrap text-gray-500 text-xs">{{ formatDate(item.mainLog.createdAt) }}</td>
                    <td class="py-3 px-3 lg:px-4">
                      <div class="flex items-center gap-2">
                        <svg class="w-3.5 h-3.5 text-gray-400 transition-transform" :class="{ 'rotate-90': isExpanded(item.contextId) }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                        </svg>
                        <span class="font-medium text-gray-900">{{ getGroupLabel(item) }}</span>
                      </div>
                      <div class="text-[10px] text-gray-400 font-mono pl-5">{{ item.mainLog.entityId }}</div>
                    </td>
                    <td class="py-3 px-3 lg:px-4 whitespace-nowrap">
                      <span :class="getActionClass(getGroupAction(item))" class="px-2 py-0.5 rounded text-[10px] font-bold uppercase">
                        SESSION
                      </span>
                    </td>
                    <td class="py-3 px-3 lg:px-4 text-gray-500">
                      <UserBadge :user="{
                        id: item.mainLog.userId,
                        firstName: item.mainLog.userFirstName,
                        lastName: item.mainLog.userLastName,
                        email: item.mainLog.userEmail,
                        avatar: item.mainLog.userAvatar
                      }" />
                    </td>
                    <td colspan="2" class="py-3 px-3 lg:px-4 hidden sm:table-cell italic text-gray-400 text-xs">
                      Contient {{ item.logs.length }} événements terminal
                    </td>
                    <td class="py-3 px-3 lg:px-4 text-right">
                      <button class="p-1.5 rounded-md hover:bg-warm-muted text-gray-400 hover:text-accent transition-colors">
                        <svg class="w-4 h-4" :class="{ 'rotate-180': isExpanded(item.contextId) }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                      </button>
                    </td>
                  </tr>
                  
                  <!-- Expanded Group Content -->
                  <tr v-if="isExpanded(item.contextId)" v-for="log in item.logs" :key="log.id"
                    class="border-b border-warm-border/30 bg-warm-muted/5 hover:bg-warm-muted/20 transition-colors">
                    <td class="py-2 px-3 lg:px-4 whitespace-nowrap text-gray-400 text-[10px] pl-6 lg:pl-8">{{ formatDate(log.createdAt) }}</td>
                    <td class="py-2 px-3 lg:px-4 pl-8 lg:pl-10">
                      <span class="text-xs text-gray-500">{{ log.entityName }}</span>
                    </td>
                    <td class="py-2 px-3 lg:px-4 whitespace-nowrap">
                      <span :class="getActionClass(log.action)" class="px-1.5 py-0.5 rounded text-[9px] font-bold uppercase opacity-80">
                        {{ actionLabel(log.action) }}
                      </span>
                    </td>
                    <td class="py-2 px-3 lg:px-4"></td>
                    <td class="py-2 px-3 lg:px-4 hidden sm:table-cell">
                      <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-[11px] text-gray-400">
                        {{ log.oldValue ? truncate(log.oldValue) : '—' }}
                      </span>
                    </td>
                    <td class="py-2 px-3 lg:px-4 hidden sm:table-cell">
                      <span class="block truncate max-w-[140px] lg:max-w-xs font-mono text-[11px] text-gray-600">
                        {{ log.newValue ? truncate(log.newValue) : '—' }}
                      </span>
                    </td>
                    <td class="py-2 px-3 lg:px-4 text-right">
                      <button @click="openDetail(log)"
                        class="p-1 rounded-md hover:bg-warm-muted text-gray-400 hover:text-accent transition-colors">
                        <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                        </svg>
                      </button>
                    </td>
                  </tr>
                </template>
              </template>
              <tr v-if="!loading && !logs.length">
                <td colspan="7" class="py-20 text-center text-gray-400 italic">Aucun log d'audit trouvé</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="totalPages > 1" class="mt-4 flex justify-between items-center text-sm text-gray-500">
        <span>Page {{ filters.page + 1 }} sur {{ totalPages }}</span>
        <div class="flex gap-2">
          <button @click="filters.page--" :disabled="filters.page === 0"
            class="px-3 py-1.5 border border-warm-border rounded-md bg-white hover:bg-warm-muted disabled:opacity-30 transition-colors">
            Précédent
          </button>
          <button @click="filters.page++" :disabled="filters.page >= totalPages - 1"
            class="px-3 py-1.5 border border-warm-border rounded-md bg-accent text-white hover:bg-accent-hover disabled:opacity-30 transition-colors">
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
import adminUsersService from '@/services/adminUsersService'
import UserBadge from '@/components/UserBadge.vue'
import AuditDetailModal from '@/components/AuditDetailModal.vue'
import { SearchIcon } from '@/components/icons'
import { syncQuery } from '@/utils/query'

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
  components: { UserBadge, AuditDetailModal, SearchIcon },
  data() {
    return {
      logs: [],
      usersList: [],
      loading: false,
      totalPages: 0,
      selectedLog: null,
      expandedGroups: new Set(),
      filters: {
        userId: '',
        entityName: '',
        action: '',
        search: '',
        page: 0
      }
    }
  },
  computed: {
    processedLogs() {
      const result = []
      let i = 0
      while (i < this.logs.length) {
        const log = this.logs[i]
        if (log.contextId && log.entityName === 'Terminal') {
          const group = {
            id: log.contextId,
            type: 'group',
            contextId: log.contextId,
            logs: [log],
            mainLog: log
          }
          let j = i + 1
          while (j < this.logs.length && this.logs[j].contextId === log.contextId) {
            group.logs.push(this.logs[j])
            j++
          }
          result.push(group)
          i = j
        } else {
          result.push({ type: 'single', log, id: log.id })
          i++
        }
      }
      return result
    }
  },
  mounted() {
    syncQuery(this, {
      key: 'audit_logs',
      defaultFilters: { userId: '', entityName: '', action: '', search: '', page: 0 },
      onUpdate: () => this.load()
    })
    this.load()
    this.loadUsers()
  },
  methods: {
    toggleGroup(contextId) {
      if (this.expandedGroups.has(contextId)) {
        this.expandedGroups.delete(contextId)
      } else {
        this.expandedGroups.add(contextId)
      }
      // Force reactivity
      this.expandedGroups = new Set(this.expandedGroups)
    },
    isExpanded(contextId) {
      return this.expandedGroups.has(contextId)
    },
    getGroupLabel(group) {
      const commands = group.logs.filter(l => l.action === 'TERMINAL_COMMAND').length
      return `Session Terminal (${commands} commande${commands > 1 ? 's' : ''})`
    },
    getGroupAction(group) {
      if (group.logs.some(l => l.action === 'TERMINAL_CONNECT')) return 'TERMINAL_CONNECT'
      return 'TERMINAL_COMMAND'
    },
    load() {
      this.loading = true
      const params = { 
        page: this.filters.page, 
        size: 20,
        userId: this.filters.userId || null,
        entityName: this.filters.entityName || null,
        action: this.filters.action || null,
        search: this.filters.search || null
      }
      adminAuditService.list(params).then(res => {
        this.logs = res.data.content
        this.totalPages = res.data.totalPages
      }).finally(() => {
        this.loading = false
      })
    },
    loadUsers() {
      adminUsersService.getAll().then(res => {
        this.usersList = res.data
      })
    },
    resetFilters() {
      this.filters.userId = ''
      this.filters.entityName = ''
      this.filters.action = ''
      this.filters.search = ''
      this.filters.page = 0
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
  },
}
</script>
