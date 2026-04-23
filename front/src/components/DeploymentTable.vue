<template>
  <div class="bg-white border border-warm-border rounded-xl overflow-hidden">
    <div v-if="loading" class="text-center py-10 text-gray-400 text-sm">Chargement...</div>
    <div v-else-if="!deployments.length" class="text-center py-10 text-gray-400 text-sm">Aucun déploiement</div>
    <table v-else class="w-full text-sm">
      <thead class="bg-warm-muted border-b border-warm-border">
        <tr>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide w-24">ID</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Hôte</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Type</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Statut</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Lancé par</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Durée</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Date</th>
          <th class="py-3 px-4 w-10" />
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in deployments" :key="d.id" class="border-b border-warm-border/50 hover:bg-warm-muted/40">
          <td class="py-3 px-4 text-xs text-gray-500 font-mono">#{{ shortId(d.id) }}</td>
          <td class="py-3 px-4 font-medium">{{ d.hostName || '—' }}</td>
          <td class="py-3 px-4"><TypeBadge :type="d.type" /></td>
          <td class="py-3 px-4"><StatusBadge :status="d.status" /></td>
          <td class="py-3 px-4 text-gray-500">{{ d.userFirstName }} {{ d.userLastName }}</td>
          <td class="py-3 px-4 text-xs font-mono text-gray-400">{{ formatDuration(d.durationSeconds) }}</td>
          <td class="py-3 px-4 text-xs text-gray-400">{{ formatDate(d.createdAt) }}</td>
          <td class="py-3 px-4">
            <button @click="$emit('view', d)" class="p-1 rounded hover:bg-warm-muted text-gray-400 hover:text-gray-600">
              <EyeIcon class="w-3.5 h-3.5" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import StatusBadge from '@/components/StatusBadge.vue'
import TypeBadge from '@/components/TypeBadge.vue'
import { EyeIcon } from '@/components/icons'

export default {
  components: { StatusBadge, TypeBadge, EyeIcon },
  props: { deployments: Array, loading: Boolean },
  emits: ['view'],
  methods: {
    shortId(id) {
      if (!id) return '—'
      return String(id).slice(0, 6)
    },
    formatDate(d) {
      if (!d) return '—'
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    formatDuration(seconds) {
      if (seconds == null) return '—'
      const m = Math.floor(seconds / 60).toString().padStart(2, '0')
      const s = (seconds % 60).toString().padStart(2, '0')
      return `${m}:${s}`
    },
  },
}
</script>
