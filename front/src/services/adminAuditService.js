import axios from '@/api/axios'

export default {
  list(params = {}) {
    return axios.get('/admin/audit', { params })
  },
  getByUserId(userId, page = 0, size = 20) {
    return axios.get(`/admin/audit/user/${userId}`, { params: { page, size } })
  },
  getById(id) {
    return axios.get(`/admin/audit/${id}`)
  },
}
