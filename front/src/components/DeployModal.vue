<template>
  <div class="fixed inset-0 bg-black/40 z-40 flex items-center justify-center p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
      <div class="p-5 border-b border-warm-border">
        <h2 class="font-semibold text-gray-900">Lancer — {{ typeLabel }}</h2>
        <p class="text-sm text-gray-500 mt-0.5">{{ host.name }} ({{ host.ip }})</p>
      </div>
      <div class="p-5 space-y-4">
        <div class="bg-warm-muted rounded-md p-3 font-mono text-xs text-gray-600 break-all">
          {{ resolvedCommand }}
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            Timeout (minutes) — <span class="text-gray-400 font-normal">0 = désactivé</span>
          </label>
          <input v-model.number="timeout" type="number" min="0"
            class="w-full border border-warm-border rounded-md px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent/20" />
        </div>
        <div v-if="error" class="text-sm text-status-failure bg-status-failure-bg rounded-md px-3 py-2">{{ error }}</div>
      </div>
      <div class="p-4 bg-warm-muted border-t border-warm-border flex justify-end gap-2">
        <button @click="$emit('close')" class="px-4 py-2 rounded-md text-sm border border-warm-border bg-white hover:bg-warm-muted">Annuler</button>
        <button @click="launch" :disabled="loading"
          class="px-4 py-2 rounded-md text-sm bg-accent text-white hover:bg-accent-hover disabled:opacity-50">
          {{ loading ? 'Lancement...' : 'Lancer' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import api from '@/api/axios'

const props = defineProps({ host: Object, type: String, defaultTimeout: { type: Number, default: 10 } })
const emit = defineEmits(['close', 'deployed'])

const timeout = ref(props.defaultTimeout)
const loading = ref(false)
const error = ref('')

const typeLabel = computed(() => ({ DEPLOY: 'Déploiement', GENERATE: 'Génération', DELIVER: 'Livraison' }[props.type] || props.type))

const resolvedCommand = computed(() => {
  const cmd = props.type === 'DEPLOY' ? props.host.deploymentCommand
    : props.type === 'GENERATE' ? props.host.generateCommand
    : props.host.deliverCommand
  if (!cmd) return 'sh /root/{host}/liv.sh'.replace('{host}', props.host.name)
  return cmd
    .replace('{host}', props.host.name || '')
    .replace('{ip}', props.host.ip || '')
    .replace('{domain}', props.host.domain || '')
})

async function launch() {
  loading.value = true
  error.value = ''
  try {
    await api.post(`/deployments/hosts/${props.host.id}/deploy`, { type: props.type, timeout: timeout.value })
    emit('deployed')
  } catch (e) {
    error.value = e.response?.data?.error || 'Erreur lors du lancement'
  } finally {
    loading.value = false
  }
}
</script>
