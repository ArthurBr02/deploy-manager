import api from '@/api/axios'

export default {
  getAll: () => api.get('/admin/users'),
  getById: (id) => api.get(`/admin/users/${id}`),
  getPublicById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/admin/users', data),
  update: (id, data) => api.put(`/admin/users/${id}`, data),
  delete: (id) => api.delete(`/admin/users/${id}`),
  getPermissions: (id) => api.get(`/admin/users/${id}/permissions`),
  setPermission: (id, data) => api.put(`/admin/users/${id}/permissions`, data),
}
