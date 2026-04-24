<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Utilisateurs</h1>
      <div class="flex-1" />
      <button @click="showCreate = true" class="flex items-center gap-1.5 bg-accent text-white px-3 py-1.5 rounded-md text-sm hover:bg-accent-hover">
        <PlusIcon class="w-3.5 h-3.5" /> Nouvel utilisateur
      </button>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="bg-white border border-warm-border rounded-xl overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-warm-muted border-b border-warm-border">
            <tr>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Utilisateur</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Email</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Rôle</th>
              <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Créé le</th>
              <th class="py-3 px-4"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" class="border-b border-warm-border/50 hover:bg-warm-muted/40">
              <td class="py-3 px-4">
                <UserBadge :user="u" />
              </td>
              <td class="py-3 px-4 text-gray-500">{{ u.email }}</td>
              <td class="py-3 px-4">
                <span :class="u.role === 'ADMIN' ? 'text-accent bg-accent-subtle' : 'text-gray-500 bg-gray-100'" class="px-2 py-0.5 rounded text-xs font-medium">{{ u.role }}</span>
              </td>
              <td class="py-3 px-4 text-xs text-gray-400">{{ formatDate(u.createdAt) }}</td>
              <td class="py-3 px-4">
                <div class="flex gap-2 justify-end">
                  <RouterLink :to="`/admin/users/${u.id}`" class="text-gray-400 hover:text-accent">
                    <EditIcon class="w-4 h-4" />
                  </RouterLink>
                  <button @click="deleteUser(u)" class="text-gray-400 hover:text-red-500">
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

  <!-- Create modal -->
  <div v-if="showCreate" class="fixed inset-0 bg-black/40 z-40 flex items-center justify-center p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
      <h2 class="font-semibold text-gray-900">Créer un utilisateur</h2>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Prénom</label>
          <input v-model="newUser.firstName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
          <input v-model="newUser.lastName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input v-model="newUser.email" type="email" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Rôle</label>
        <select v-model="newUser.role" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent">
          <option>USER</option><option>ADMIN</option>
        </select>
      </div>

      <!-- Created password display -->
      <div v-if="createdUser" class="bg-yellow-50 border border-yellow-200 rounded-md p-3 space-y-1">
        <div class="text-xs font-medium text-yellow-800">Mot de passe temporaire (affiché une seule fois)</div>
        <div class="font-mono text-sm bg-white border border-yellow-200 rounded px-2 py-1">{{ createdUser.temporaryPassword }}</div>
      </div>

      <div v-if="createError" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ createError }}</div>
      <div class="flex justify-end gap-2">
        <button @click="closeCreate" class="px-4 py-2 text-sm border border-warm-border rounded-md hover:bg-warm-muted">
          {{ createdUser ? 'Fermer' : 'Annuler' }}
        </button>
        <button v-if="!createdUser" @click="createUser" :disabled="creating" class="px-4 py-2 text-sm bg-accent text-white rounded-md hover:bg-accent-hover disabled:opacity-50">
          {{ creating ? 'Création...' : 'Créer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import adminUsersService from '@/services/adminUsersService'
import { PlusIcon, EditIcon, TrashIcon } from '@/components/icons'
import UserBadge from '@/components/UserBadge.vue'

export default {
  components: { PlusIcon, EditIcon, TrashIcon, UserBadge },
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      users: [],
      loading: false,
      showCreate: false,
      creating: false,
      createError: '',
      createdUser: null,
      newUser: { firstName: '', lastName: '', email: '', role: 'USER' },
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      return adminUsersService.getAll().then(res => {
        this.users = res.data
      }).finally(() => {
        this.loading = false
      })
    },
    createUser() {
      this.creating = true
      this.createError = ''
      adminUsersService.create(this.newUser).then(res => {
        this.createdUser = res.data
        return this.load()
      }).catch(e => {
        this.createError = e.response?.data?.error || 'Erreur'
      }).finally(() => {
        this.creating = false
      })
    },
    closeCreate() {
      this.showCreate = false
      this.createdUser = null
      this.createError = ''
      Object.assign(this.newUser, { firstName: '', lastName: '', email: '', role: 'USER' })
    },
    deleteUser(u) {
      if (!confirm(`Supprimer ${u.firstName} ${u.lastName} ?`)) return
      adminUsersService.delete(u.id).then(() => {
        this.toastStore.success('Utilisateur supprimé')
        return this.load()
      }).catch(e => {
        this.toastStore.error(e.response?.data?.error || 'Erreur')
      })
    },
    formatDate(d) {
      return new Date(d).toLocaleDateString('fr-FR')
    },
  },
}
</script>
