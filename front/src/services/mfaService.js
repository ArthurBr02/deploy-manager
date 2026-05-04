import api from '@/api/axios'

export default {
  verifyMfa: (challengeId, code, trustDevice) =>
    api.post('/auth/verify-mfa', { challengeId, code, trustDevice }),

  getTrustedDevices: () => api.get('/auth/trusted-devices'),

  revokeTrustedDevice: (id) => api.delete(`/auth/trusted-devices/${id}`),

  adminGetTrustedDevices: (userId) =>
    api.get(`/admin/users/${userId}/trusted-devices`),

  adminRevokeTrustedDevice: (userId, deviceId) =>
    api.delete(`/admin/users/${userId}/trusted-devices/${deviceId}`),

  adminRevokeAllTrustedDevices: (userId) =>
    api.delete(`/admin/users/${userId}/trusted-devices`),
}
