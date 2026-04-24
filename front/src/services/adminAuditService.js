import axios from '@/api/axios'

export default {
  list(page = 0, size = 20) {
    return axios.get('/admin/audit', { params: { page, size } })
  },
}
