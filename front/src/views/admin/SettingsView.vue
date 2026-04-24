<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Paramètres</h1>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <div v-if="pageLoading" class="flex items-center justify-center py-20">
        <div class="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin"></div>
      </div>
      <div v-else class="max-w-2xl mx-auto space-y-6">
        <!-- General -->
        <div class="bg-white border border-warm-border rounded-xl p-5 space-y-4">
          <h2 class="font-semibold text-gray-900">Général</h2>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Commande de déploiement par défaut</label>
            <textarea v-model="settings.default_deploy_command" rows="2" class="w-full border border-warm-border rounded-md px-3 py-2 text-xs font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
            <p class="text-xs text-gray-400 mt-1">Variables disponibles : {host}, {ip}, {domain}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Système d'exploitation du serveur</label>
            <select v-model="settings.server_os" class="w-48 border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20">
              <option value="linux">Linux</option>
              <option value="windows">Windows</option>
            </select>
            <p class="text-xs text-gray-400 mt-1">Détermine le shell utilisé pour exécuter les commandes de déploiement.</p>
          </div>
          <div v-if="settings.server_os === 'linux'" class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Shell Linux — exécutable</label>
              <input v-model="settings.shell_linux_bin" placeholder="/bin/sh" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <p class="text-xs text-gray-400 mt-1">Défaut : <code class="font-mono">/bin/sh</code></p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Shell Linux — argument</label>
              <input v-model="settings.shell_linux_arg" placeholder="-c" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <p class="text-xs text-gray-400 mt-1">Défaut : <code class="font-mono">-c</code></p>
            </div>
          </div>
          <div v-else-if="settings.server_os === 'windows'" class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Shell Windows — exécutable</label>
              <input v-model="settings.shell_windows_bin" placeholder="cmd.exe" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <p class="text-xs text-gray-400 mt-1">Défaut : <code class="font-mono">cmd.exe</code></p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Shell Windows — argument</label>
              <input v-model="settings.shell_windows_arg" placeholder="/c" class="w-full border border-warm-border rounded-md px-3 py-2 text-sm font-mono outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
              <p class="text-xs text-gray-400 mt-1">Défaut : <code class="font-mono">/c</code></p>
            </div>
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
          <div
            class="border-2 border-dashed rounded-lg p-6 text-center transition-colors cursor-pointer"
            :class="isDragging ? 'border-accent bg-accent/5' : 'border-warm-border hover:border-gray-400'"
            @dragover.prevent
            @dragenter.prevent="onDragEnter"
            @dragleave.prevent="onDragLeave"
            @drop.prevent="handleDrop"
            @click="triggerFilePicker"
          >
            <UploadIcon class="w-8 h-8 mx-auto mb-2" :class="isDragging ? 'text-accent' : 'text-gray-300'" />
            <p class="text-sm text-gray-500 mb-3">
              {{ isDragging ? 'Relâchez pour importer' : 'Glissez un fichier hosts-all ici ou' }}
            </p>
            <span v-if="!isDragging" class="px-4 py-2 border border-warm-border rounded-md text-sm bg-white hover:bg-warm-muted pointer-events-none">Parcourir</span>
            <input ref="fileInputRef" type="file" @change="handleFile" class="hidden" />
          </div>
          <div v-if="importResult" class="bg-green-50 border border-green-200 rounded-md px-3 py-2 text-sm text-green-700">
            Import terminé : {{ importResult.created }} hôte(s) créé(s), {{ importResult.updated }} mis à jour, {{ importResult.skipped }} ignoré(s).
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

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'
import adminSettingsService from '@/services/adminSettingsService'
import hostsService from '@/services/hostsService'
import { UploadIcon } from '@/components/icons'

export default {
  components: { UploadIcon },
  computed: {
    ...mapStores(useToastStore),
  },
  data() {
    return {
      settings: {
        default_deploy_command: 'sh /root/{host}/liv.sh',
        default_timeout: '10',
        server_os: 'linux',
        shell_linux_bin: '/bin/sh',
        shell_linux_arg: '-c',
        shell_windows_bin: 'cmd.exe',
        shell_windows_arg: '/c',
        smtp_host: '', smtp_port: '', smtp_username: '', smtp_password: '', smtp_from: '',
      },
      pageLoading: false,
      saving: false,
      saveError: '',
      importResult: null,
      importError: '',
      isDragging: false,
      dragCounter: 0,
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.pageLoading = true
      try {
        const res = await adminSettingsService.get()
        Object.assign(this.settings, res.data.settings)
      } finally {
        this.pageLoading = false
      }
    },
    async save() {
      this.saving = true
      this.saveError = ''
      try {
        await adminSettingsService.update({ ...this.settings })
        this.toastStore.success('Paramètres enregistrés')
      } catch (e) {
        this.saveError = e.response?.data?.error || 'Erreur'
      } finally {
        this.saving = false
      }
    },
    onDragEnter() {
      this.dragCounter++
      this.isDragging = true
    },
    onDragLeave() {
      this.dragCounter--
      if (this.dragCounter === 0) this.isDragging = false
    },
    triggerFilePicker() {
      this.$refs.fileInputRef?.click()
    },
    handleDrop(event) {
      this.dragCounter = 0
      this.isDragging = false
      const file = event.dataTransfer.files[0]
      if (file) this.uploadFile(file)
    },
    handleFile(event) {
      const file = event.target.files[0]
      if (file) this.uploadFile(file)
    },
    async uploadFile(file) {
      this.importResult = null
      this.importError = ''
      try {
        const res = await hostsService.importAnsible(file)
        this.importResult = res.data
      } catch (e) {
        this.importError = e.response?.data?.error || "Erreur lors de l'import"
      }
    },
  },
}
</script>
