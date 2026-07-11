import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getOrCreateMonitorClientId,
  MONITOR_CLIENT_ID_PREFIX,
  MONITOR_CLIENT_ID_STORAGE_KEY
} from '../src/utils/monitorClient.js'

test('复用存储中的工作站 ID', () => {
  const storage = { getItem: () => 'monitor-client-existing', setItem() { throw new Error('不应写入') } }
  assert.equal(getOrCreateMonitorClientId({ storage }), 'monitor-client-existing')
})

test('使用 randomUUID 创建并保存工作站 ID', () => {
  const values = new Map()
  const storage = {
    getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value)
  }
  const id = getOrCreateMonitorClientId({
    storage,
    crypto: { randomUUID: () => 'uuid-123' }
  })
  assert.equal(id, 'monitor-client-uuid-123')
  assert.equal(values.get(MONITOR_CLIENT_ID_STORAGE_KEY), id)
  assert.equal(getOrCreateMonitorClientId({ storage }), id)
})

test('存储异常时模块生命周期内复用同一 fallback ID', () => {
  const storage = { getItem() { throw new Error('blocked') } }
  const first = getOrCreateMonitorClientId({ storage, crypto: { randomUUID: () => 'fallback' } })
  const second = getOrCreateMonitorClientId({ storage, crypto: { randomUUID: () => 'different' } })
  assert.equal(first, second)
  assert.equal(first, 'monitor-client-fallback')
})

test('无 crypto 时生成带稳定前缀的 fallback 格式', () => {
  const values = new Map()
  const storage = {
    getItem: (key) => values.get(key) || null,
    setItem: (key, value) => values.set(key, value)
  }
  const id = getOrCreateMonitorClientId({ storage, crypto: null, now: () => 1000, random: () => 0.5 })
  assert.ok(id.startsWith(MONITOR_CLIENT_ID_PREFIX))
  assert.equal(values.get(MONITOR_CLIENT_ID_STORAGE_KEY), id)
})

test('无可用存储时当前模块生命周期内保持同一 ID', () => {
  const first = getOrCreateMonitorClientId({ storage: null, crypto: null, now: () => 2000, random: () => 0.1 })
  const second = getOrCreateMonitorClientId({ storage: null, crypto: null, now: () => 3000, random: () => 0.9 })
  assert.equal(first, second)
  assert.ok(first.startsWith(MONITOR_CLIENT_ID_PREFIX))
})
