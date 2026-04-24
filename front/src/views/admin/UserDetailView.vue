<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <RouterLink to="/admin/users" class="hover:text-gray-700">Utilisateurs</RouterLink>
        <span>/</span>
        <span class="font-medium text-gray-900">{{ user?.firstName }} {{ user?.lastName }}</span>
      </div>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div v-if="!user" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="max-w-2xl mx-auto space-y-6">
        <!-- User info -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Informations</h2>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Prénom</label>
              <input v-model="form.firstName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
              <input v-model="form.lastName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Rôle</label>
              <select v-model="form.role" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent">
                <option>USER</option><option>ADMIN</option>
              </select>
            </div>
          </div>
          <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
          <button @click="save" :disabled="saving" class="px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
            {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
          </button>
        </div>

        <!-- Permissions -->
        <div class="bg-white border border-warm-border rounded-xl p-5">
          <h2 class="font-semibold text-gray-900 mb-4">Permissions sur les hôtes</h2>
          <div class="space-y-2">
            <div v-for="host in hosts" :key="host.id" class="flex items-center gap-4 py-2 border-b border-warm-border/50 last:border-0">
              <div class="flex-1">
                <div class="font-medium text-sm text-gray-900">{{ host.name }}</div>
                <div class="text-xs text-gray-400 font-mono">{{ host.ip }}</div>
              </div>
              <label class="flex items-center gap-1.5 text-sm cursor-pointer">
                <input type="checkbox" :checked="getPerm(host.id, 'canDeploy')" @change="togglePerm(host.id, 'canDeploy', $event.target.checked)" class="rounded" />
                Déployer
              </label>
              <label class="flex items-center gap-1.5 text-sm cursor-pointer">
                <input type="checkbox" :checked="getPerm(host.id, 'canEdit')" @change="togglePerm(host.id, 'canEdit', $event.target.checked)" class="rounded" />
                Modifier
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import adminUsersService from '@/services/adminUsersService'
import hostsService from '@/services/hostsService'

export default {
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      user: null,
      hosts: [],
      permissions: [],
      form: { firstName: '', lastName: '', role: 'USER' },
      saving: false,
      error: '',
    }
  },
  mounted() {
    const id = this.$route.params.id
    Promise.all([
      adminUsersService.getById(id),
      hostsService.getAll(),
      adminUsersService.getPermissions(id),
    ]).then(([userRes, hostsRes, permsRes]) => {
      this.user = userRes.data
      this.form.firstName = userRes.data.firstName
      this.form.lastName = userRes.data.lastName
      this.form.role = userRes.data.role
      this.hosts = hostsRes.data
      this.permissions = permsRes.data
    })
  },
  methods: {
    getPerm(hostId, field) {
      return this.permissions.find(p => p.hostId === hostId)?.[field] || false
    },
    togglePerm(hostId, field, value) {
      const existing = this.permissions.find(p => p.hostId === hostId) || { hostId, canDeploy: false, canEdit: false }
      const updated = { ...existing, [field]: value }
      adminUsersService.setPermission(this.$route.params.id, { hostId, canDeploy: updated.canDeploy, canEdit: updated.canEdit }).then(() => {
        const idx = this.permissions.findIndex(p => p.hostId === hostId)
        if (idx >= 0) this.permissions[idx] = updated
        else this.permissions.push(updated)
        this.toastStore.success('Permissions mises à jour')
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur')
      })
    },
    save() {
      this.saving = true
      this.error = ''
      adminUsersService.update(this.$route.params.id, this.form).then(res => {
        this.user = res.data
        this.toastStore.success('Utilisateur mis à jour')
      }).catch(e => {
        this.error = e.response?.data?.error || 'Erreur'
      }).finally(() => {
        this.saving = false
      })
    },
  },
}
</script>
