export const MONITOR_CLIENT_ID_STORAGE_KEY = 'industrial-iot-monitor-client-id'
export const MONITOR_CLIENT_ID_PREFIX = 'monitor-client-'

let fallbackClientId = null

const createFallbackSuffix = (options) => {
  const now = options.now || Date.now
  const random = options.random || Math.random
  return `${now().toString(36)}-${Math.floor(random() * Number.MAX_SAFE_INTEGER).toString(36)}`
}

const createClientId = (cryptoApi, options) => {
  try {
    if (cryptoApi && typeof cryptoApi.randomUUID === 'function') {
      return `${MONITOR_CLIENT_ID_PREFIX}${cryptoApi.randomUUID()}`
    }
  } catch { /* use deterministic-shape fallback below */ }
  return `${MONITOR_CLIENT_ID_PREFIX}${createFallbackSuffix(options)}`
}

export function getOrCreateMonitorClientId(options = {}) {
  let storage
  let cryptoApi
  try {
    storage = Object.prototype.hasOwnProperty.call(options, 'storage') ? options.storage : globalThis.localStorage
  } catch { storage = null }
  try {
    cryptoApi = Object.prototype.hasOwnProperty.call(options, 'crypto') ? options.crypto : globalThis.crypto
  } catch { cryptoApi = null }

  if (!storage || typeof storage.getItem !== 'function' || typeof storage.setItem !== 'function') {
    if (!fallbackClientId) fallbackClientId = createClientId(cryptoApi, options)
    return fallbackClientId
  }

  try {
    const existing = storage.getItem(MONITOR_CLIENT_ID_STORAGE_KEY)
    if (existing) return existing
  } catch {
    if (!fallbackClientId) fallbackClientId = createClientId(cryptoApi, options)
    return fallbackClientId
  }

  const clientId = fallbackClientId || createClientId(cryptoApi, options)
  try {
    storage.setItem(MONITOR_CLIENT_ID_STORAGE_KEY, clientId)
  } catch {
    fallbackClientId = clientId
    return fallbackClientId
  }
  return clientId
}
