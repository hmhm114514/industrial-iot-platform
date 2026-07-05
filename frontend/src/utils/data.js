export const normalizeList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (!payload) return []
  if (Array.isArray(payload.records)) return payload.records
  if (Array.isArray(payload.list)) return payload.list
  if (Array.isArray(payload.rows)) return payload.rows
  if (Array.isArray(payload.content)) return payload.content
  if (Array.isArray(payload.data)) return payload.data
  return []
}

export const pickToken = (payload) =>
  payload?.token || payload?.accessToken || payload?.jwt || payload?.data?.token || payload?.data?.accessToken

export const nowText = () => new Date().toLocaleString('zh-CN', { hour12: false })

export const uid = (prefix = 'ID') => `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

export const clone = (value) => JSON.parse(JSON.stringify(value))
