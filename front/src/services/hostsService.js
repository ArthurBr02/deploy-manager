import api from '@/api/axios'

export default {
  getAll: () => api.get('/hosts'),
  getById: (id) => api.get(`/hosts/${id}`),
  update: (id, data) => api.put(`/hosts/${id}`, data),
  create: (data) => api.post('/admin/hosts', data),
  delete: (id) => api.delete(`/admin/hosts/${id}`),
  importAnsible: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/hosts/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  downloadDump: (id) => api.get(`/hosts/${id}/dump`, { responseType: 'blob' }),
  requestDump: (id) => api.post(`/hosts/${id}/dump-request`),
  getTlogStreamUrl: (id, token) => `${import.meta.env.VITE_API_URL || '/api'}/hosts/${id}/tlog?token=${token}`,
}
