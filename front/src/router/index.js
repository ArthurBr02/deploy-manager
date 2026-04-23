import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/forgot-password', name: 'forgot-password', component: () => import('@/views/ForgotPasswordView.vue'), meta: { public: true } },
    { path: '/reset-password', name: 'reset-password', component: () => import('@/views/ResetPasswordView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/hosts' },
        { path: 'hosts', name: 'hosts', component: () => import('@/views/HostsView.vue') },
        { path: 'hosts/:id', name: 'host-detail', component: () => import('@/views/HostDetailView.vue') },
        { path: 'deployments', name: 'deployments', component: () => import('@/views/DeploymentsView.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue') },
        { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/UsersView.vue'), meta: { admin: true } },
        { path: 'admin/users/:id', name: 'admin-user-detail', component: () => import('@/views/admin/UserDetailView.vue'), meta: { admin: true } },
        { path: 'admin/settings', name: 'admin-settings', component: () => import('@/views/admin/SettingsView.vue'), meta: { admin: true } },
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ]
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }
  if (to.meta.admin && auth.user?.role !== 'ADMIN') {
    return next({ name: 'hosts' })
  }
  if (to.meta.public && auth.isAuthenticated) {
    return next({ name: 'hosts' })
  }
  next()
})

export default router
