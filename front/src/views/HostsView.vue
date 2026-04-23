<template>
  <div class="flex flex-col h-full">
    <!-- Topbar -->
    <header class="h-14 border-b border-warm-border bg-white flex items-center px-6 gap-4 flex-shrink-0">
      <h1 class="text-base font-semibold text-gray-900">Hôtes</h1>
      <div class="flex-1" />
      <div class="relative">
        <SearchIcon class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" />
        <input v-model="search" placeholder="Rechercher..." class="pl-8 pr-3 py-1.5 text-sm border border-warm-border rounded-md outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 w-52" />
      </div>
      <RouterLink v-if="authStore.isAdmin" to="/admin/hosts/new">
        <button class="flex items-center gap-1.5 bg-accent hover:bg-accent-hover text-white px-3 py-1.5 rounded-md text-sm font-medium transition-colors">
          <PlusIcon class="w-3.5 h-3.5" /> Nouvel hôte
        </button>
      </RouterLink>
    </header>

    <!-- Content -->
    <div class="flex-1 overflow-auto p-6">
      <div v-if="loading" class="text-center py-20 text-gray-400">Chargement...</div>
      <div v-else-if="!filtered.length" class="text-center py-20 border-2 border-dashed border-warm-border rounded-xl text-gray-400">
        <ServerIcon class="w-10 h-10 mx-auto mb-3 opacity-30" />
        <p>Aucun hôte accessible</p>
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <HostCard
          v-for="host in filtered"
          :key="host.id"
          :host="host"
          @deploy="openDeployModal(host, 'DEPLOY')"
          @generate="openDeployModal(host, 'GENERATE')"
          @deliver="openDeployModal(host, 'DELIVER')"
        />
      </div>
    </div>
  </div>

  <!-- Deploy Modal -->
  <DeployModal
    v-if="modal.show"
    :host="modal.host"
    :type="modal.type"
    :default-timeout="modal.defaultTimeout"
    @close="modal.show = false"
    @deployed="onDeployed"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import HostCard from '@/components/HostCard.vue'
import DeployModal from '@/components/DeployModal.vue'
import { SearchIcon, PlusIcon, ServerIcon } from '@/components/icons'

const authStore = useAuthStore()
const toastStore = useToastStore()
const hosts = ref([])
const loading = ref(true)
const search = ref('')
const modal = ref({ show: false, host: null, type: 'DEPLOY', defaultTimeout: 10 })

const filtered = computed(() =>
  hosts.value.filter(h => h.name.toLowerCase().includes(search.value.toLowerCase()) || h.ip.includes(search.value))
)

async function load() {
  try {
    const res = await api.get('/hosts')
    hosts.value = res.data
  } finally {
    loading.value = false
  }
}

function openDeployModal(host, type) {
  modal.value = { show: true, host, type, defaultTimeout: host.defaultTimeout ?? 10 }
}

function onDeployed() {
  modal.value.show = false
  load()
  toastStore.success('Déploiement lancé')
}

onMounted(load)
</script>
