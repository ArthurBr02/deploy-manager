<template>
  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600 border border-gray-200">
    {{ label }}
  </span>
</template>

<script>
const TYPE_MAP = { DEPLOY: 'Déployer', GENERATE: 'Générer', DELIVER: 'Livrer', ROLLBACK: 'Rollback' }

export default {
  props: { type: String },
  computed: {
    label() { return TYPE_MAP[this.type] || this.type },
  },
}
</script>
