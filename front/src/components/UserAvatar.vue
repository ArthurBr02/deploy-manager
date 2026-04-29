<template>
  <div 
    class="rounded-full flex-shrink-0 overflow-hidden flex items-center justify-center bg-gradient-to-br from-purple-500 to-accent text-white font-semibold"
    :style="{ width: size, height: size }"
  >
    <img v-if="user?.avatar" :src="user.avatar" class="w-full h-full object-cover" alt="avatar" />
    <span v-else :class="textClass">{{ initials }}</span>
  </div>
</template>

<script>
export default {
  name: 'UserAvatar',
  props: {
    user: {
      type: Object,
      required: true
    },
    size: {
      type: String,
      default: '24px'
    },
    textClass: {
      type: String,
      default: 'text-[10px]'
    }
  },
  computed: {
    initials() {
      if (!this.user) return '?'
      const first = this.user.firstName?.[0] || ''
      const last = this.user.lastName?.[0] || ''
      return (first + last).toUpperCase() || '?'
    }
  }
}
</script>
