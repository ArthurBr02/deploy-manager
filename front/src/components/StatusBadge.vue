<template>
  <span :class="['inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium border', cls]">
    <span v-if="status === 'IN_PROGRESS'" class="dm-spin" style="width:8px;height:8px;border-width:1.5px" />
    <span v-else class="w-1.5 h-1.5 rounded-full bg-current" />
    {{ label }}
  </span>
</template>

<script setup>
const props = defineProps({ status: String })

const map = {
  SUCCESS:     { cls: 'text-status-success bg-status-success-bg border-green-200',   label: 'Succès' },
  FAILURE:     { cls: 'text-status-failure bg-status-failure-bg border-red-200',     label: 'Échec' },
  IN_PROGRESS: { cls: 'text-status-progress bg-status-progress-bg border-yellow-200', label: 'En cours' },
  PENDING:     { cls: 'text-status-pending bg-status-pending-bg border-gray-200',    label: 'En attente' },
  CANCELLED:   { cls: 'text-status-cancelled bg-status-cancelled-bg border-slate-200', label: 'Annulé' },
  NEVER:       { cls: 'text-gray-500 bg-gray-50 border-gray-200',                    label: 'Jamais' },
}
const { cls, label } = map[props.status] || map.NEVER
</script>
