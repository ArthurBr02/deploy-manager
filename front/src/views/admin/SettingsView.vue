<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Paramètres</h1>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div class="max-w-2xl mx-auto space-y-6">
        <!-- General -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Général</h2>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Commande de déploiement par défaut</label>
            <textarea v-model="settings.default_deploy_command" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            <p class="text-xs text-gray-400 mt-1">Variables disponibles : {host}, {ip}, {domain}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Timeout par défaut (minutes)</label>
            <input v-model="settings.default_timeout" type="number" min="0" class="w-40 border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            <span class="text-xs text-gray-400 ml-2">0 = désactivé</span>
          </div>
        </div>

        <!-- SMTP -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Email (SMTP)</h2>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Serveur SMTP</label>
              <input v-model="settings.smtp_host" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Port</label>
              <input v-model="settings.smtp_port" type="number" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Utilisateur</label>
              <input v-model="settings.smtp_username" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Mot de passe</label>
              <input v-model="settings.smtp_password" type="password" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Expéditeur</label>
              <input v-model="settings.smtp_from" type="email" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            </div>
          </div>
        </div>

        <!-- Import Ansible -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Import Ansible</h2>
          <p class="text-sm text-gray-500">Importer un fichier <code class="font-mono text-xs bg-warm-muted px-1 rounded">hosts-all</code> pour mettre à jour les hôtes existants.</p>
          <div class="border-2 border-dashed border-warm-border rounded-lg p-6 text-center">
            <UploadIcon class="w-8 h-8 mx-auto mb-2 text-gray-300" />
            <p class="text-sm text-gray-500 mb-3">Glissez un fichier hosts-all ici ou</p>
            <label class="cursor-pointer">
              <input type="file" @change="handleFile" class="hidden" />
              <span class="px-4 py-2 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted">Parcourir</span>
            </label>
          </div>
          <div v-if="importResult" class="bg-green-50 border border-green-200 rounded-md px-3 py-2 text-sm text-green-700">
            Import terminé : {{ importResult.updated }} hôte(s) mis à jour, {{ importResult.skipped }} ignoré(s).
          </div>
          <div v-if="importError" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ importError }}</div>
        </div>

        <div v-if="saveError" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ saveError }}</div>
        <div class="flex justify-end">
          <button @click="save" :disabled="saving" class="px-6 py-2 bg-accent text-white rounded-md text-sm hover:bg-accent-hover disabled:opacity-50">
            {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { UploadIcon } from '@/components/icons'

const toastStore = useToastStore()
const settings = reactive({
  default_deploy_command: 'sh /root/{host}/liv.sh',
  default_timeout: '10',
  smtp_host: '', smtp_port: '', smtp_username: '', smtp_password: '', smtp_from: ''
})
const saving = ref(false)
const saveError = ref('')
const importResult = ref(null)
const importError = ref('')

async function load() {
  const res = await api.get('/admin/settings')
  Object.assign(settings, res.data.settings)
}

async function save() {
  saving.value = true
  saveError.value = ''
  try {
    await api.put('/admin/settings', { settings: { ...settings } })
    toastStore.success('Paramètres enregistrés')
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Erreur'
  } finally {
    saving.value = false
  }
}

async function handleFile(event) {
  const file = event.target.files[0]
  if (!file) return
  importResult.value = null
  importError.value = ''
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await api.post('/admin/hosts/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    importResult.value = res.data
  } catch (e) {
    importError.value = e.response?.data?.error || "Erreur lors de l'import"
  }
}

onMounted(load)
</script>
