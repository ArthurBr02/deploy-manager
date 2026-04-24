import axios from '@/api/axios'

export default {
  list(page = 0, size = 20) {
    return axios.get('/admin/audit', { params: { page, size } })
  },
  getByUserId(userId, page = 0, size = 20) {
    return axios.get(`/admin/audit/user/${userId}`, { params: { page, size } })
  },
}
