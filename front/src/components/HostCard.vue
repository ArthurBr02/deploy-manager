<template>
  <div class="bg-white border border-warm-border rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow">
    <div class="flex items-start justify-between mb-3">
      <div class="flex-1 min-w-0">
        <h3 class="font-semibold text-sm text-gray-900 truncate">{{ host.name }}</h3>
        <div class="text-xs text-gray-400 font-mono mt-0.5">{{ host.ip }}</div>
        <div v-if="host.domain" class="text-xs text-gray-400 truncate">{{ host.domain }}</div>
      </div>
      <RouterLink :to="`/hosts/${host.id}`" class="ml-2 text-gray-400 hover:text-gray-600">
        <EyeIcon class="w-4 h-4" />
      </RouterLink>
    </div>

    <div class="mb-3">
      <StatusBadge :status="host.lastDeploymentStatus || 'NEVER'" />
    </div>

    <div class="flex gap-1.5 flex-wrap">
      <button v-if="host.canDeploy" @click="$emit('deploy')"
        :title="resolvedCommand('deploymentCommand')"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-accent/10 text-accent hover:bg-accent/20 transition-colors">
        <RocketIcon class="w-3 h-3" /> Déployer
      </button>
      <button v-if="host.canDeploy && host.generateCommand" @click="$emit('generate')"
        :title="resolvedCommand('generateCommand')"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors">
        <PackageIcon class="w-3 h-3" /> Générer
      </button>
      <button v-if="host.canDeploy && host.deliverCommand" @click="$emit('deliver')"
        :title="resolvedCommand('deliverCommand')"
        class="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors">
        <TruckIcon class="w-3 h-3" /> Livrer
      </button>
    </div>
  </div>
</template>

<script setup>
import StatusBadge from '@/components/StatusBadge.vue'
import { EyeIcon, RocketIcon, PackageIcon, TruckIcon } from '@/components/icons'

const props = defineProps({ host: Object })
defineEmits(['deploy', 'generate', 'deliver'])

function resolvedCommand(field) {
  const cmd = props.host[field]
  if (!cmd) return ''
  return cmd
    .replace('{host}', props.host.name || '')
    .replace('{ip}', props.host.ip || '')
    .replace('{domain}', props.host.domain || '')
}
</script>
