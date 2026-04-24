<template>
  <div class="flex items-center gap-2 min-w-0">
    <UserAvatar :user="user" :size="size" :text-class="textClass" />
    <span class="truncate" :class="nameClass">
      {{ user?.firstName }} {{ user?.lastName }}
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
  }
}
</script>
