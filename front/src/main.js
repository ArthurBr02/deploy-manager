import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

;(async () => {
  const auth = useAuthStore()
  await auth.tryRestoreSession()
  app.use(router)
  app.mount('#app')
})()
