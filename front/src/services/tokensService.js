import axios from '../api/axios';

export default {
  list() {
    return axios.get('/profile/tokens');
  },
  create(data) {
    return axios.post('/profile/tokens', data);
  },
  delete(id) {
    return axios.delete(`/profile/tokens/${id}`);
  }
};
