import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

api.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let refreshing = null

api.interceptors.response.use(
  res => res,
  err => {
    const original = err.config
    if (err.response?.status === 401 && !original._retry) {
      original._retry = true
      if (!refreshing) {
        refreshing = api.post('/auth/refresh').then(r => {
          const auth = useAuthStore()
          auth.accessToken = r.data.accessToken
          refreshing = null
        }).catch(() => {
          const auth = useAuthStore()
          auth.logout()
          refreshing = null
        })
      }
      return refreshing.then(() => {
        const auth = useAuthStore()
        if (auth.accessToken) {
          original.headers.Authorization = `Bearer ${auth.accessToken}`
          return api(original)
        }
        return Promise.reject(err)
      })
    }
    return Promise.reject(err)
  }
)

export default api
