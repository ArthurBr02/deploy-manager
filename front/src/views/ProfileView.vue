<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Mon profil</h1>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div class="max-w-xl mx-auto space-y-6">
        <!-- Profile form -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Informations personnelles</h2>

          <!-- Avatar -->
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-full flex-shrink-0 overflow-hidden flex items-center justify-center bg-gradient-to-br from-purple-500 to-accent text-white text-lg font-semibold">
              <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="w-full h-full object-cover" alt="avatar" />
              <span v-else>{{ userInitials }}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="cursor-pointer">
                <input type="file" accept="image/*" class="hidden" @change="uploadAvatar" :disabled="uploadingAvatar" />
                <span class="px-3 py-1.5 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted" :class="uploadingAvatar ? 'opacity-50 pointer-events-none' : ''">
                  {{ uploadingAvatar ? 'Envoi...' : "Changer l'avatar" }}
                </span>
              </label>
              <button v-if="authStore.user?.avatar" @click="deleteAvatar" :disabled="deletingAvatar" class="px-3 py-1.5 border border-warm-border rounded-md text-sm text-red-500 hover:bg-red-50 disabled:opacity-50 text-left">
                {{ deletingAvatar ? 'Suppression...' : 'Supprimer' }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Prénom</label>
              <input v-model="form.firstName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
              <input v-model="form.lastName" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input :value="authStore.user?.email" disabled class="w-full border border-warm-border rounded-md px-3 py-2 text-sm bg-warm-muted text-gray-400" />
          </div>
          <div v-if="profileError" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ profileError }}</div>
          <button @click="saveProfile" :disabled="savingProfile" class="px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
            {{ savingProfile ? 'Enregistrement...' : 'Enregistrer' }}
          </button>
        </div>

        <!-- Password form -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Changer le mot de passe</h2>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Mot de passe actuel</label>
            <input v-model="pwForm.currentPassword" type="password" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nouveau mot de passe</label>
            <input v-model="pwForm.newPassword" type="password" minlength="8" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
          </div>
          <div v-if="pwError" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ pwError }}</div>
          <button @click="changePassword" :disabled="savingPw" class="px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
            {{ savingPw ? 'Modification...' : 'Modifier le mot de passe' }}
          </button>
        </div>

        <!-- PAT form -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Personal Access Tokens</h2>
          <p class="text-xs text-gray-500">Utilisez ces tokens pour vous authentifier via le serveur MCP ou d'autres scripts.</p>

          <!-- List existing tokens -->
          <div v-if="tokens.length > 0" class="space-y-2">
            <div v-for="t in tokens" :key="t.id" class="flex items-center justify-between p-3 border border-warm-border rounded-lg bg-warm-muted/30">
              <div class="flex flex-col">
                <span class="text-sm font-medium text-gray-900">{{ t.name }}</span>
                <span class="text-xs text-gray-500">Créé le {{ formatDate(t.createdAt) }} {{ t.lastUsedAt ? '• Utilisé le ' + formatDate(t.lastUsedAt) : '' }}</span>
              </div>
              <button @click="deleteToken(t.id)" class="p-1.5 text-red-500 hover:bg-red-50 rounded-md transition-colors">
                <TrashIcon class="w-4 h-4" />
              </button>
            </div>
          </div>
          <div v-else class="text-sm text-gray-500 italic py-2">Aucun token actif.</div>

          <!-- New token created display -->
          <div v-if="newToken" class="p-3 bg-status-success-bg border border-status-success-border rounded-lg space-y-2">
            <p class="text-sm font-medium text-status-success">Nouveau token créé !</p>
            <p class="text-xs text-gray-600">Copiez-le maintenant, il ne sera plus affiché ensuite.</p>
            <div class="flex gap-2">
              <input :value="newToken" readonly class="flex-1 bg-white border border-warm-border rounded-md px-3 py-1.5 text-xs font-mono outline-none" />
              <button @click="copyNewToken" class="px-3 py-1.5 bg-white border border-warm-border rounded-md text-xs hover:bg-warm-muted">Copier</button>
            </div>
          </div>

          <!-- Create token form -->
          <div class="pt-2 border-t border-warm-border space-y-3">
            <h3 class="text-sm font-medium text-gray-900">Générer un nouveau token</h3>
            <div class="flex gap-2">
              <input v-model="tokenForm.name" placeholder="Nom du token (ex: Claude Desktop)" class="flex-1 border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <button @click="createToken" :disabled="creatingToken || !tokenForm.name" class="px-4 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
                Générer
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import profileService from '@/services/profileService'
import tokensService from '@/services/tokensService'
import { TrashIcon } from '@/components/icons'

export default {
  components: { TrashIcon },
  computed: {
    ...mapStores(useAuthStore, useToastStore),
    userInitials() {
      const u = this.authStore.user
      if (!u) return '?'
      return ((u.firstName?.[0] || '') + (u.lastName?.[0] || '')).toUpperCase()
    },
  },
  data() {
    return {
      form: { firstName: '', lastName: '' },
      pwForm: { currentPassword: '', newPassword: '' },
      tokenForm: { name: '' },
      savingProfile: false,
      savingPw: false,
      creatingToken: false,
      profileError: '',
      pwError: '',
      uploadingAvatar: false,
      deletingAvatar: false,
      tokens: [],
      newToken: null,
    }
  },
  mounted() {
    this.form.firstName = this.authStore.user?.firstName || ''
    this.form.lastName = this.authStore.user?.lastName || ''
    this.loadTokens()
  },
  methods: {
    async loadTokens() {
      try {
        const res = await tokensService.list()
        this.tokens = res.data
      } catch (e) {
        console.error('Failed to load tokens', e)
      }
    },
    async createToken() {
      this.creatingToken = true
      this.newToken = null
      try {
        const res = await tokensService.create(this.tokenForm)
        this.newToken = res.data.token
        this.tokenForm.name = ''
        await this.loadTokens()
        this.toastStore.success('Token généré')
      } catch (e) {
        this.toastStore.error("Erreur lors de la génération du token")
      } finally {
        this.creatingToken = false
      }
    },
    async deleteToken(id) {
      if (!confirm('Voulez-vous vraiment révoquer ce token ?')) return
      try {
        await tokensService.delete(id)
        await this.loadTokens()
        this.toastStore.success('Token révoqué')
      } catch (e) {
        this.toastStore.error("Erreur lors de la révocation")
      }
    },
    copyNewToken() {
      navigator.clipboard.writeText(this.newToken)
      this.toastStore.success('Copié dans le presse-papier')
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    },
    async saveProfile() {
      this.savingProfile = true
      this.profileError = ''
      try {
        await profileService.update(this.form)
        await this.authStore.refreshProfile()
        this.toastStore.success('Profil mis à jour')
      } catch (e) {
        this.profileError = e.response?.data?.error || 'Erreur'
      } finally {
        this.savingProfile = false
      }
    },
    async changePassword() {
      this.savingPw = true
      this.pwError = ''
      try {
        await profileService.changePassword(this.pwForm)
        this.toastStore.success('Mot de passe modifié')
        this.pwForm.currentPassword = ''
        this.pwForm.newPassword = ''
      } catch (e) {
        this.pwError = e.response?.data?.error || 'Erreur'
      } finally {
        this.savingPw = false
      }
    },
    async uploadAvatar(event) {
      const file = event.target.files[0]
      if (!file) return
      this.uploadingAvatar = true
      try {
        await profileService.uploadAvatar(file)
        await this.authStore.refreshProfile()
        this.toastStore.success('Avatar mis à jour')
      } catch (e) {
        this.toastStore.error(e.response?.data?.error || "Erreur lors de l'upload")
      } finally {
        this.uploadingAvatar = false
        event.target.value = ''
      }
    },
    async deleteAvatar() {
      this.deletingAvatar = true
      try {
        await profileService.deleteAvatar()
        await this.authStore.refreshProfile()
        this.toastStore.success('Avatar supprimé')
      } catch (e) {
        this.toastStore.error(e.response?.data?.error || 'Erreur lors de la suppression')
      } finally {
        this.deletingAvatar = false
      }
    },
  },
}
</script>
