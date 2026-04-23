import api from '@/api/axios'

export default {
  launch: (hostId, data) => api.post(`/deployments/hosts/${hostId}/deploy`, data),
  cancel: (deploymentId) => api.post(`/deployments/${deploymentId}/cancel`),
  getHistory: (hostId, size = 20) => api.get(`/deployments?hostId=${hostId}&size=${size}`),
  list: (params) => api.get('/deployments?' + new URLSearchParams(params)),
  getStats: (params) => api.get('/deployments/stats?' + new URLSearchParams(params)),
  exportCsv: (params) => api.get('/deployments/export?' + new URLSearchParams(params), { responseType: 'blob' }),
}
