<template>
  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600 border border-gray-200">
    {{ label }}
  </span>
</template>

<script setup>
const props = defineProps({ type: String })
const map = { DEPLOY: 'Déployer', GENERATE: 'Générer', DELIVER: 'Livrer' }
const label = map[props.type] || props.type
</script>
