<template>
  <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-80">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="flex items-start gap-3 rounded-lg p-4 shadow-lg text-sm font-medium border"
        :class="toastClass(toast.type)"
      >
        <span class="flex-1">{{ toast.message }}</span>
        <button @click="toastStore.remove(toast.id)" class="text-current opacity-60 hover:opacity-100">&#x2715;</button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useToastStore } from '@/stores/toast'

export default {
  computed: {
    ...mapStores(useToastStore),
  },
  methods: {
    toastClass(type) {
      return {
        success: 'bg-status-success-bg border-green-200 text-status-success',
        error: 'bg-status-failure-bg border-red-200 text-status-failure',
        info: 'bg-blue-50 border-blue-200 text-blue-700',
      }[type] || 'bg-warm-panel border-warm-border text-gray-700'
    },
  },
}
</script>

<style scoped>
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(100%); }
.toast-leave-to { opacity: 0; transform: translateX(100%); }
</style>
