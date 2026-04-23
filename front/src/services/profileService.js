import api from '@/api/axios'

export default {
  update: (data) => api.put('/profile', data),
  changePassword: (data) => api.post('/profile/change-password', data),
  uploadAvatar: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/profile/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  deleteAvatar: () => api.delete('/profile/avatar'),
}
