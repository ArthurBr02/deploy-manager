<template>
  <RouterLink v-if="user?.id" :to="`/users/${user.id}`" class="flex items-center gap-2 min-w-0 hover:opacity-80 transition-opacity">
    <UserAvatar :user="user" :size="size" :text-class="textClass" />
    <span class="truncate" :class="nameClass">
      {{ displayName }}
    </span>
  </RouterLink>
  <div v-else class="flex items-center gap-2 min-w-0">
    <UserAvatar :user="user" :size="size" :text-class="textClass" />
    <span class="truncate" :class="nameClass">
      {{ displayName }}
    </span>
  </div>
</template>

<script>
import UserAvatar from './UserAvatar.vue'

export default {
  name: 'UserBadge',
  components: { UserAvatar },
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
    },
    nameClass: {
      type: String,
      default: 'text-sm'
    }
  },
  computed: {
    displayName() {
      if (!this.user) return 'Système'
      const name = `${this.user.firstName || ''} ${this.user.lastName || ''}`.trim()
      if (name) return name
      return this.user.email || this.user.id || 'Système'
    }
  }
}
</script>
