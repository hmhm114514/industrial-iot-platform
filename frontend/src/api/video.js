import http from './http'

const base = '/video-devices/local-cameras'

export const monitorDeviceApi = {
  list: (params) => http.get(base, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, businessData) => http.put(`${base}/${id}`, businessData),
  rebind: (id, bindingData) => http.post(`${base}/${id}/rebind`, bindingData),
  toggle: (id) => http.post(`${base}/${id}/toggle`),
  remove: (id) => http.delete(`${base}/${id}`)
}

export default monitorDeviceApi
