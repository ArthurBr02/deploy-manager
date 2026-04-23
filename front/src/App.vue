<template>
  <RouterView />
  <ToastContainer />
</template>

<script>
import ToastContainer from '@/components/ToastContainer.vue'

export default {
  components: { ToastContainer },
}
</script>
