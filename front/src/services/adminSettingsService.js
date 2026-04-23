import api from '@/api/axios'

export default {
  get: () => api.get('/admin/settings'),
  update: (settings) => api.put('/admin/settings', { settings }),
}
