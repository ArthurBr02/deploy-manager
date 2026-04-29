<template>
  <div class="flex flex-col h-full bg-warm-muted/20">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0 shadow-sm z-10">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <RouterLink v-if="isAdmin" to="/admin/users" class="hover:text-gray-700 transition-colors">Utilisateurs</RouterLink>
        <span v-else>Utilisateur</span>
        <ChevronRightIcon class="w-3 h-3 opacity-30" />
        <span class="font-semibold text-gray-900">{{ targetUser?.firstName }} {{ targetUser?.lastName }}</span>
      </div>
    </header>

    <div class="flex-1 overflow-auto">
      <div v-if="!targetUser" class="flex items-center justify-center py-32">
        <div class="w-8 h-8 border-3 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="max-w-7xl mx-auto p-6 grid grid-cols-1 gap-6 items-start" :class="isAdmin ? 'lg:grid-cols-2' : ''">
        
        <!-- Left Column: User Profile & Deployments -->
        <div class="space-y-6">
          <!-- Profile Card -->
          <div class="bg-white border border-warm-border rounded-2xl p-6 shadow-sm">
            <div class="flex items-start gap-6 mb-8">
              <div class="relative group">
                <UserAvatar :user="targetUser" size="80px" text-class="text-2xl" />
                <div v-if="isAdmin" class="absolute -bottom-1 -right-1 bg-white border border-warm-border rounded-full p-1.5 shadow-sm text-gray-400 group-hover:text-accent transition-colors cursor-pointer">
                  <EditIcon class="w-3.5 h-3.5" />
                </div>
              </div>
              <div class="flex-1 min-w-0 pt-1">
                <div class="flex items-center gap-3 mb-1">
                  <h2 class="text-xl font-bold text-gray-900 truncate">{{ targetUser.firstName }} {{ targetUser.lastName }}</h2>
                  <span :class="targetUser.role === 'ADMIN' ? 'text-accent bg-accent/10 border-accent/20' : 'text-gray-500 bg-gray-100 border-gray-200'" 
                        class="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase border tracking-wider">
                    {{ targetUser.role }}
                  </span>
                </div>
                <div class="text-gray-500 font-mono text-sm flex items-center gap-1.5">
                  <MailIcon class="w-3.5 h-3.5 opacity-40" />
                  {{ targetUser.email }}
                </div>
                <div class="mt-3 text-xs text-gray-400">Membre depuis le {{ formatDate(targetUser.createdAt) }}</div>
              </div>
            </div>

            <!-- Admin Edit Form -->
            <div v-if="isAdmin" class="space-y-5 pt-6 border-t border-warm-border/60">
              <h3 class="text-xs font-bold text-gray-400 uppercase tracking-widest">Informations utilisateur</h3>
              <div class="grid grid-cols-2 gap-4">
                <div class="space-y-1.5">
                  <label class="block text-[11px] font-bold text-gray-500 uppercase tracking-wider ml-1">Prénom</label>
                  <input v-model="form.firstName" class="w-full bg-warm-muted/30 border border-warm-border rounded-xl px-4 py-2.5 text-sm outline-none focus:bg-white focus:border-accent focus:ring-4 focus:ring-accent/5 transition-all" />
                </div>
                <div class="space-y-1.5">
                  <label class="block text-[11px] font-bold text-gray-500 uppercase tracking-wider ml-1">Nom</label>
                  <input v-model="form.lastName" class="w-full bg-warm-muted/30 border border-warm-border rounded-xl px-4 py-2.5 text-sm outline-none focus:bg-white focus:border-accent focus:ring-4 focus:ring-accent/5 transition-all" />
                </div>
                <div class="space-y-1.5 col-span-2 md:col-span-1">
                  <label class="block text-[11px] font-bold text-gray-500 uppercase tracking-wider ml-1">Rôle système</label>
                  <select v-model="form.role" class="w-full bg-warm-muted/30 border border-warm-border rounded-xl px-4 py-2.5 text-sm outline-none focus:bg-white focus:border-accent transition-all appearance-none cursor-pointer">
                    <option value="USER">Utilisateur standard</option>
                    <option value="ADMIN">Administrateur</option>
                  </select>
                </div>
              </div>
              
              <div v-if="error" class="text-xs font-medium text-status-failure bg-status-failure-bg border border-status-failure/10 rounded-xl px-4 py-3">{{ error }}</div>
              
              <div class="flex justify-end pt-2">
                <button @click="save" :disabled="saving" class="flex items-center gap-2 px-5 py-2.5 bg-accent text-white rounded-xl text-sm font-bold hover:bg-accent-hover disabled:opacity-50 transition-all shadow-sm shadow-accent/20">
                  <CheckIcon v-if="!saving" class="w-4 h-4" />
                  {{ saving ? 'Enregistrement...' : 'Mettre à jour' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Tabs View -->
          <div class="bg-white border border-warm-border rounded-2xl shadow-sm overflow-hidden">
            <div class="border-b border-warm-border flex bg-warm-muted/20 p-1">
              <button v-for="tab in visibleTabs" :key="tab.id" @click="activeTab = tab.id"
                class="flex-1 py-2.5 text-[11px] font-bold uppercase tracking-wider rounded-lg transition-all"
                :class="activeTab === tab.id ? 'bg-white text-accent shadow-sm' : 'text-gray-400 hover:text-gray-600'">
                {{ tab.label }}
              </button>
            </div>

            <div class="min-h-[400px]">
              <!-- History -->
              <div v-if="activeTab === 'deployments'" class="animate-in fade-in duration-300">
                <div v-if="!deploymentsLoading && !deployments.length" class="flex flex-col items-center justify-center py-20 px-10 text-center">
                  <div class="w-16 h-16 bg-warm-muted/50 rounded-full flex items-center justify-center mb-4 border border-dashed border-warm-border">
                    <RocketIcon class="w-8 h-8 text-gray-300" />
                  </div>
                  <h4 class="text-gray-900 font-bold text-sm mb-1">Aucun déploiement</h4>
                  <p class="text-gray-400 text-xs max-w-[200px]">Cet utilisateur n'a pas encore effectué de déploiement.</p>
                </div>
                <template v-else>
                  <DeploymentTable :deployments="deployments" :loading="deploymentsLoading" @view="viewDeployment" />
                  <div v-if="deploymentsTotalPages > 1" class="p-4 border-t border-warm-border bg-warm-muted/10 flex justify-between items-center text-[10px] font-bold text-gray-500 uppercase tracking-widest">
                    <span>Page {{ deploymentsPage + 1 }} / {{ deploymentsTotalPages }}</span>
                    <div class="flex gap-2">
                      <button @click="changeDeploymentsPage(-1)" :disabled="deploymentsPage === 0" class="px-3 py-1.5 bg-white border border-warm-border rounded-lg hover:bg-gray-50 disabled:opacity-30 transition-colors shadow-sm">Précédent</button>
                      <button @click="changeDeploymentsPage(1)" :disabled="deploymentsPage >= deploymentsTotalPages - 1" class="px-3 py-1.5 bg-white border border-warm-border rounded-lg hover:bg-gray-50 disabled:opacity-30 transition-colors shadow-sm">Suivant</button>
                    </div>
                  </div>
                </template>
              </div>

              <!-- Permissions -->
              <div v-if="activeTab === 'permissions' && isAdmin" class="divide-y divide-warm-border/50 animate-in fade-in duration-300">
                <div v-if="!hosts.length" class="p-20 text-center text-gray-400 italic text-sm">Aucun hôte configuré</div>
                <div v-for="host in hosts" :key="host.id" class="flex items-center gap-4 p-4 hover:bg-warm-muted/20 transition-colors group">
                  <div class="w-10 h-10 bg-warm-muted/50 rounded-xl flex items-center justify-center text-gray-400 group-hover:bg-accent/10 group-hover:text-accent transition-colors">
                    <ServerIcon class="w-5 h-5" />
                  </div>
                  <div class="flex-1">
                    <div class="font-bold text-sm text-gray-900">{{ host.name }}</div>
                    <div class="text-[10px] text-gray-400 font-mono tracking-tight">{{ host.ip }}</div>
                  </div>
                  <div class="flex gap-3">
                    <button v-for="p in [{f: 'canDeploy', l: 'Deploy'}, {f: 'canEdit', l: 'Edit'}, {f: 'canExecute', l: 'SSH'}]" :key="p.f"
                      @click="togglePerm(host.id, p.f, !getPerm(host.id, p.f))"
                      class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-tighter border transition-all"
                      :class="getPerm(host.id, p.f) ? 'bg-accent border-accent text-white shadow-sm shadow-accent/20' : 'bg-white border-warm-border text-gray-400 hover:border-gray-300'">
                      {{ p.l }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Audit Logs (Admin only) -->
        <div v-if="isAdmin" class="bg-white border border-warm-border rounded-2xl flex flex-col overflow-hidden sticky top-6 h-[calc(100vh-140px)] shadow-sm">
          <header class="h-12 border-b border-warm-border bg-warm-muted/30 px-5 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <ClipboardIcon class="w-4 h-4 text-gray-400" />
              <span class="text-xs font-black text-gray-600 uppercase tracking-widest">Journal d'audit</span>
            </div>
            <div v-if="auditTotalPages > 1" class="flex items-center gap-3">
              <button @click="changeAuditPage(-1)" :disabled="auditPage === 0" class="p-1.5 rounded-lg hover:bg-white hover:shadow-sm border border-transparent hover:border-warm-border disabled:opacity-30 transition-all">
                <ChevronLeftIcon class="w-4 h-4" />
              </button>
              <span class="text-[10px] font-black text-gray-400 uppercase tabular-nums">{{ auditPage + 1 }} / {{ auditTotalPages }}</span>
              <button @click="changeAuditPage(1)" :disabled="auditPage >= auditTotalPages - 1" class="p-1.5 rounded-lg hover:bg-white hover:shadow-sm border border-transparent hover:border-warm-border disabled:opacity-30 transition-all">
                <ChevronRightIcon class="w-4 h-4" />
              </button>
            </div>
          </header>
          
          <div class="flex-1 overflow-auto bg-warm-muted/5">
            <div v-if="auditLoading && !auditLogs.length" class="flex items-center justify-center py-20">
              <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
            </div>
            <div v-else-if="!auditLogs.length" class="flex flex-col items-center justify-center h-full text-gray-400 p-12 text-center">
              <div class="w-12 h-12 bg-white rounded-2xl shadow-sm flex items-center justify-center mb-4 border border-warm-border/50">
                <ClipboardIcon class="w-6 h-6 opacity-20" />
              </div>
              <p class="text-xs font-bold uppercase tracking-widest opacity-60">Aucune activité</p>
            </div>
            <div v-else class="divide-y divide-warm-border/40">
              <div v-for="log in auditLogs" :key="log.id" class="p-5 hover:bg-white transition-colors group">
                <div class="flex items-center justify-between mb-3">
                  <span :class="getActionClass(log.action)" class="px-2 py-0.5 rounded text-[9px] font-black uppercase tracking-widest border border-current border-opacity-10">
                    {{ log.action }}
                  </span>
                  <span class="text-[10px] font-bold text-gray-400 group-hover:text-gray-600 transition-colors tabular-nums">{{ formatDate(log.createdAt) }}</span>
                </div>
                <div class="text-[11px] mb-3 leading-relaxed">
                  <span class="text-gray-400 font-bold uppercase tracking-tighter mr-2">Entité</span>
                  <span class="text-gray-900 font-bold">{{ log.entityName }}</span>
                  <span class="text-[10px] text-gray-400 font-mono ml-2 opacity-0 group-hover:opacity-100 transition-opacity">#{{ log.entityId ? log.entityId.slice(0, 8) : '—' }}</span>
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div class="bg-white border border-warm-border/60 rounded-xl p-3 shadow-xs">
                    <div class="text-[9px] font-black text-gray-400 uppercase tracking-widest mb-1.5 opacity-60">Avant</div>
                    <div class="text-[10px] font-mono break-all line-clamp-2 text-gray-400" :title="log.oldValue">{{ log.oldValue || '—' }}</div>
                  </div>
                  <div class="bg-white border border-warm-border/60 rounded-xl p-3 shadow-xs">
                    <div class="text-[9px] font-black text-accent uppercase tracking-widest mb-1.5 opacity-60">Après</div>
                    <div class="text-[10px] font-mono break-all line-clamp-2 text-gray-900 font-semibold" :title="log.newValue">{{ log.newValue || '—' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <DeploymentLogsModal v-if="viewedDeployment" :deployment="viewedDeployment" @close="viewedDeployment = null" />
</template>

<script>
import { mapStores, mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import adminUsersService from '@/services/adminUsersService'
import adminAuditService from '@/services/adminAuditService'
import deploymentsService from '@/services/deploymentsService'
import hostsService from '@/services/hostsService'
import UserAvatar from '@/components/UserAvatar.vue'
import DeploymentTable from '@/components/DeploymentTable.vue'
import DeploymentLogsModal from '@/components/DeploymentLogsModal.vue'
import { 
  ClipboardIcon, ChevronLeftIcon, ChevronRightIcon, 
  MailIcon, EditIcon, RocketIcon, ServerIcon, CheckIcon 
} from '@/components/icons'

export default {
  components: { 
    UserAvatar, DeploymentTable, DeploymentLogsModal, 
    ClipboardIcon, ChevronLeftIcon, ChevronRightIcon, 
    MailIcon, EditIcon, RocketIcon, ServerIcon, CheckIcon 
  },
  computed: {
    ...mapStores(useToastStore),
    ...mapState(useAuthStore, ['user']),
    isAdmin() {
      return this.user?.role === 'ADMIN'
    },
    visibleTabs() {
      return this.tabs.filter(t => t.id !== 'permissions' || this.isAdmin)
    }
  },
  data() {
    return {
      targetUser: null,
      hosts: [],
      permissions: [],
      deployments: [],
      auditLogs: [],
      
      form: { firstName: '', lastName: '', role: 'USER' },
      saving: false,
      error: '',
      
      activeTab: 'deployments',
      tabs: [
        { id: 'deployments', label: 'Historique' },
        { id: 'permissions', label: 'Droits d\'accès' }
      ],
      
      deploymentsLoading: false,
      deploymentsPage: 0,
      deploymentsTotalPages: 0,
      
      auditLoading: false,
      auditPage: 0,
      auditTotalPages: 0,
      
      viewedDeployment: null
    }
  },
  mounted() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
      const id = this.$route.params.id
      try {
        const fetchUser = this.isAdmin ? adminUsersService.getById(id) : adminUsersService.getPublicById(id)
        const promises = [fetchUser]
        
        if (this.isAdmin) {
          promises.push(hostsService.getAll())
          promises.push(adminUsersService.getPermissions(id))
        }
        
        const results = await Promise.all(promises)
        const userRes = results[0]
        
        this.targetUser = userRes.data
        this.form.firstName = userRes.data.firstName
        this.form.lastName = userRes.data.lastName
        this.form.role = userRes.data.role
        
        if (this.isAdmin) {
          this.hosts = results[1].data
          this.permissions = results[2].data
        }
        
        this.loadDeployments()
        if (this.isAdmin) {
          this.loadAudit()
        }
      } catch (e) {
        this.toastStore.error("Erreur lors du chargement des données")
      }
    },
    loadDeployments() {
      this.deploymentsLoading = true
      deploymentsService.list({ 
        userId: this.$route.params.id, 
        page: this.deploymentsPage, 
        size: 15 
      }).then(res => {
        this.deployments = res.data.content
        this.deploymentsTotalPages = res.data.totalPages
      }).finally(() => {
        this.deploymentsLoading = false
      })
    },
    loadAudit() {
      if (!this.isAdmin) return
      this.auditLoading = true
      adminAuditService.getByUserId(this.$route.params.id, this.auditPage, 20).then(res => {
        this.auditLogs = res.data.content
        this.auditTotalPages = res.data.totalPages
      }).finally(() => {
        this.auditLoading = false
      })
    },
    changeDeploymentsPage(delta) {
      this.deploymentsPage += delta
      this.loadDeployments()
    },
    changeAuditPage(delta) {
      this.auditPage += delta
      this.loadAudit()
    },
    viewDeployment(dep) {
      this.viewedDeployment = dep
    },
    getPerm(hostId, field) {
      return this.permissions.find(p => p.hostId === hostId)?.[field] || false
    },
    togglePerm(hostId, field, value) {
      if (!this.isAdmin) return
      const existing = this.permissions.find(p => p.hostId === hostId) || { hostId, canDeploy: false, canEdit: false, canExecute: false }
      const updated = { ...existing, [field]: value }
      adminUsersService.setPermission(this.$route.params.id, { 
        hostId, 
        canDeploy: updated.canDeploy, 
        canEdit: updated.canEdit,
        canExecute: updated.canExecute 
      }).then(() => {
        const idx = this.permissions.findIndex(p => p.hostId === hostId)
        if (idx >= 0) this.permissions[idx] = updated
        else this.permissions.push(updated)
        this.toastStore.success('Permissions mises à jour')
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur')
      })
    },
    save() {
      if (!this.isAdmin) return
      this.saving = true
      this.error = ''
      adminUsersService.update(this.$route.params.id, this.form).then(res => {
        this.targetUser = res.data
        this.toastStore.success('Utilisateur mis à jour')
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur'
      }).finally(() => {
        this.saving = false
      })
    },
    formatDate(d) {
      if (!d) return '—'
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    getActionClass(action) {
      return {
        CREATE: 'text-green-600 bg-green-50',
        UPDATE: 'text-blue-600 bg-blue-50',
        DELETE: 'text-red-600 bg-red-50',
      }[action] || 'text-gray-600 bg-gray-50'
    }
  },
}
</script>
