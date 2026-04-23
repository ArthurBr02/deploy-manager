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
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const authStore = useAuthStore()
const toastStore = useToastStore()
const form = reactive({ firstName: '', lastName: '', avatar: '' })
const pwForm = reactive({ currentPassword: '', newPassword: '' })
const savingProfile = ref(false)
const savingPw = ref(false)
const profileError = ref('')
const pwError = ref('')

onMounted(() => {
  form.firstName = authStore.user?.firstName || ''
  form.lastName = authStore.user?.lastName || ''
  form.avatar = authStore.user?.avatar || ''
})

async function saveProfile() {
  savingProfile.value = true
  profileError.value = ''
  try {
    await api.put('/profile', form)
    await authStore.refreshProfile()
    toastStore.success('Profil mis à jour')
  } catch (e) {
    profileError.value = e.response?.data?.error || 'Erreur'
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  savingPw.value = true
  pwError.value = ''
  try {
    await api.post('/profile/change-password', pwForm)
    toastStore.success('Mot de passe modifié')
    pwForm.currentPassword = ''
    pwForm.newPassword = ''
  } catch (e) {
    pwError.value = e.response?.data?.error || 'Erreur'
  } finally {
    savingPw.value = false
  }
}
</script>
