<template>
  <div class="bg-white border border-warm-border rounded-xl overflow-hidden">
    <div v-if="loading" class="text-center py-10 text-gray-400">Chargement...</div>
    <div v-else-if="!deployments.length" class="text-center py-10 text-gray-400">Aucun déploiement</div>
    <table v-else class="w-full text-sm">
      <thead class="bg-warm-muted border-b border-warm-border">
        <tr>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Date</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Type</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Statut</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Hôte</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Utilisateur</th>
          <th class="text-left py-3 px-4 font-medium text-xs text-gray-500 uppercase tracking-wide">Durée</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in deployments" :key="d.id" class="border-b border-warm-border/50 hover:bg-warm-muted/40">
          <td class="py-3 px-4 text-xs text-gray-500 font-mono">{{ formatDate(d.createdAt) }}</td>
          <td class="py-3 px-4"><TypeBadge :type="d.type" /></td>
          <td class="py-3 px-4"><StatusBadge :status="d.status" /></td>
          <td class="py-3 px-4 font-medium">{{ d.hostName || '—' }}</td>
          <td class="py-3 px-4 text-gray-500">{{ d.userFirstName }} {{ d.userLastName }}</td>
          <td class="py-3 px-4 text-xs text-gray-400">{{ d.timeout > 0 ? d.timeout + ' min' : '&#8734;' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import StatusBadge from '@/components/StatusBadge.vue'
import TypeBadge from '@/components/TypeBadge.vue'

defineProps({ deployments: Array, loading: Boolean })

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}
</script>
