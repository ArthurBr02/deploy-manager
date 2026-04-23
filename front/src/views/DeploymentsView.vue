<template>
  <div class="flex flex-col h-full">
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Historique des déploiements</h1>
      <div class="flex-1" />
      <!-- Filters -->
      <select v-model="filters.status" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
        <option value="">Tous les statuts</option>
        <option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <select v-model="filters.type" class="border border-warm-border rounded-md px-2 py-1.5 text-sm outline-none focus:border-accent">
        <option value="">Tous les types</option>
        <option value="DEPLOY">Déployer</option>
        <option value="GENERATE">Générer</option>
        <option value="DELIVER">Livrer</option>
      </select>
    </header>
    <div class="flex-1 overflow-auto p-6">
      <DeploymentTable :deployments="deployments" :loading="loading" />
      <div v-if="total > 1" class="flex justify-center gap-2 mt-4">
        <button @click="page--" :disabled="page === 0" class="px-3 py-1.5 text-sm border border-warm-border rounded-md disabled:opacity-40">Précédent</button>
        <span class="px-3 py-1.5 text-sm text-gray-500">{{ page + 1 }} / {{ total }}</span>
        <button @click="page++" :disabled="page >= total - 1" class="px-3 py-1.5 text-sm border border-warm-border rounded-md disabled:opacity-40">Suivant</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import api from '@/api/axios'
import DeploymentTable from '@/components/DeploymentTable.vue'

const deployments = ref([])
const loading = ref(false)
const page = ref(0)
const total = ref(1)
const filters = reactive({ status: '', type: '' })

const statuses = [
  { value: 'SUCCESS', label: 'Succès' },
  { value: 'FAILURE', label: 'Échec' },
  { value: 'IN_PROGRESS', label: 'En cours' },
  { value: 'CANCELLED', label: 'Annulé' },
]

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, size: 20 })
    if (filters.status) params.append('status', filters.status)
    if (filters.type) params.append('type', filters.type)
    const res = await api.get('/deployments?' + params)
    deployments.value = res.data.content
    total.value = res.data.totalPages
  } finally {
    loading.value = false
  }
}

watch([page, filters], () => load(), { deep: true })
onMounted(load)
</script>
