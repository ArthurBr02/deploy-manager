<template>
  <div class="flex h-full bg-warm-bg relative overflow-hidden">
    <!-- Backdrop for mobile -->
    <div
      v-if="isSidebarOpen"
      @click="isSidebarOpen = false"
      class="fixed inset-0 bg-black/20 backdrop-blur-sm z-40 lg:hidden transition-opacity duration-300"
      :class="{ 'opacity-100': isSidebarOpen, 'opacity-0': !isSidebarOpen }"
    ></div>

    <!-- Sidebar -->
    <aside
      class="fixed inset-y-0 left-0 z-50 w-64 bg-warm-sidebar border-r border-warm-border flex flex-col flex-shrink-0 transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:w-56"
      :class="isSidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <!-- Brand -->
      <div class="flex items-center justify-between px-4 py-4 border-b border-warm-border">
        <div class="flex items-center gap-2.5">
          <div class="w-7 h-7 rounded-md bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center text-white text-xs font-bold">D</div>
          <span class="font-semibold text-sm text-gray-900">Deploy Manager</span>
        </div>
        <button @click="isSidebarOpen = false" class="lg:hidden p-1 text-gray-400 hover:text-gray-600">
          <XIcon class="w-5 h-5" />
        </button>
      </div>

      <!-- Nav -->
      <nav class="flex-1 p-3 space-y-0.5 overflow-y-auto">
        <div class="text-[10px] font-semibold uppercase tracking-widest text-gray-400 px-2 py-3">Navigation</div>
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          @click="isSidebarOpen = false"
          class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-md text-sm text-gray-600 hover:bg-warm-muted hover:text-gray-900 transition-colors"
          :class="{ 'bg-warm-border/60 text-gray-900 font-medium': isActive(item.to) }"
        >
          <component :is="item.icon" class="w-4 h-4 flex-shrink-0" />
          {{ item.label }}
        </RouterLink>

        <template v-if="authStore.isAdmin">
          <div class="text-[10px] font-semibold uppercase tracking-widest text-gray-400 px-2 py-3 mt-2">Administration</div>
          <RouterLink
            v-for="item in adminItems"
            :key="item.to"
            :to="item.to"
            @click="isSidebarOpen = false"
            class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-md text-sm text-gray-600 hover:bg-warm-muted hover:text-gray-900 transition-colors"
            :class="{ 'bg-warm-border/60 text-gray-900 font-medium': isActive(item.to) }"
          >
            <component :is="item.icon" class="w-4 h-4 flex-shrink-0" />
            {{ item.label }}
          </RouterLink>
        </template>
      </nav>

      <!-- User footer -->
      <div class="border-t border-warm-border p-3 bg-warm-sidebar">
        <RouterLink to="/profile" @click="isSidebarOpen = false" class="flex items-center gap-2.5 px-2 py-2 rounded-md hover:bg-warm-muted transition-colors">
          <UserAvatar :user="authStore.user" size="28px" />
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-900 truncate">{{ authStore.user?.firstName }} {{ authStore.user?.lastName }}</div>
            <div class="text-xs text-gray-400">{{ authStore.user?.role === 'ADMIN' ? 'Administrateur' : 'Utilisateur' }}</div>
          </div>
        </RouterLink>
        <button @click="logout" class="flex items-center gap-2 px-2 py-1.5 mt-1 w-full rounded-md text-sm text-gray-500 hover:text-red-600 hover:bg-red-50 transition-colors">
          <LogOutIcon class="w-4 h-4" />
          Déconnexion
        </button>
      </div>
    </aside>

    <!-- Main -->
    <div class="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
      <!-- Mobile Header -->
      <header class="lg:hidden flex items-center justify-between px-4 py-3 bg-white border-b border-warm-border shrink-0">
        <div class="flex items-center gap-3">
          <button @click="isSidebarOpen = true" class="p-1 -ml-1 text-gray-500 hover:text-gray-900">
            <MenuIcon class="w-6 h-6" />
          </button>
          <div class="font-semibold text-sm text-gray-900">Deploy Manager</div>
        </div>
        <RouterLink to="/profile">
          <UserAvatar :user="authStore.user" size="32px" />
        </RouterLink>
      </header>

      <main class="flex-1 overflow-y-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script>
import { mapStores } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { ServerIcon, ClockIcon, UsersIcon, SettingsIcon, LogOutIcon, TerminalIcon, MenuIcon, XIcon } from '@/components/icons'
import UserAvatar from '@/components/UserAvatar.vue'

export default {
  components: { LogOutIcon, UserAvatar, MenuIcon, XIcon },
  data() {
    return {
      isSidebarOpen: false
    }
  },
  computed: {
    ...mapStores(useAuthStore),
    navItems() {
      return [
        { to: '/hosts', label: 'Hôtes', icon: ServerIcon },
        { to: '/deployments', label: 'Déploiements', icon: ClockIcon },
      ]
    },
    adminItems() {
      return [
        { to: '/admin/hosts', label: 'Hôtes', icon: ServerIcon },
        { to: '/admin/users', label: 'Utilisateurs', icon: UsersIcon },
        { to: '/admin/settings', label: 'Paramètres', icon: SettingsIcon },
        { to: '/admin/audit', label: 'Audit Logs', icon: TerminalIcon },
      ]
    },
  },
  methods: {
    isActive(path) {
      return this.$route.path === path || this.$route.path.startsWith(path + '/')
    },
    logout() {
      this.isSidebarOpen = false
      this.authStore.logout()
    }
  },
  watch: {
    '$route'() {
      this.isSidebarOpen = false
    }
  }
}
</script>
