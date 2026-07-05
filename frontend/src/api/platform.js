import http from './http'

const endpoints = {
  productCategory: '/product-categories',
  product: '/products',
  deviceGroup: '/device-groups',
  device: '/devices',
  networkService: '/network-services',
  script: '/scripts',
  rule: '/rules',
  ruleAudit: '/rule-audits',
  screenGroup: '/screens',
  screen: '/screens',
  gbDevice: '/video-devices',
  streamProxy: '/video-streams',
  mediaService: '/dashboard/monitor',
  videoRecord: '/video-streams',
  videoAlarm: '/video-alarm-tasks',
  historicalData: '/telemetry',
  historicalAlarm: '/alarms',
  serviceMonitor: '/dashboard/monitor',
  task: '/tasks',
  taskLog: '/tasks/logs',
  user: '/users',
  role: '/roles',
  firmware: '/firmwares',
  operationLog: '/operation-logs',
  loginLog: '/login-logs',
  aiAgent: '/ai-agents'
}

const endpointOf = (kind) => endpoints[kind] || `/${kind}`

const timeText = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : ''
const cnLevel = (value) => ({ HIGH: '高', MEDIUM: '中', LOW: '低', INFO: '低', high: '高', medium: '中', low: '低' }[value] || value || '中')
const alarmStatus = (value) => ['CLOSED', 'closed', '已处理'].includes(value) ? '已处理' : '未处理'
const shortDate = (value) => String(value || '').slice(5) || value

const normalizeSeries = (input, labels = []) => {
  if (Array.isArray(input)) {
    return input.map((item, index) => {
      if (item && typeof item === 'object') {
        const name = item.name ?? item.date ?? item.day ?? labels[index]
        return { name: shortDate(name), value: item.value ?? item.count ?? item.total ?? 0 }
      }
      return { name: shortDate(labels[index] ?? index + 1), value: item ?? 0 }
    })
  }
  if (input && typeof input === 'object') {
    return Object.entries(input).map(([name, value]) => ({ name: shortDate(name), value }))
  }
  return []
}

const adaptItem = (kind, item = {}) => {
  const status = item.status
  const base = { ...item, createdAt: timeText(item.createdAt) || item.createdAt, updatedAt: timeText(item.updatedAt) || item.updatedAt }
  switch (kind) {
    case 'device':
      return { ...base, product: item.product || `产品#${item.productId || '-'}`, group: item.group || `分组#${item.groupId || '-'}`, online: status === 'ONLINE' || status === 'online', lastSeen: timeText(item.lastOnlineAt), temperature: item.temperature ?? '-' }
    case 'product':
      return { ...base, category: item.category || `分类#${item.categoryId || '-'}`, deviceCount: item.deviceCount ?? 0 }
    case 'productCategory':
      return { ...base, description: item.description || item.remark || '工业物联网产品分类' }
    case 'deviceGroup':
      return { ...base, location: item.location || item.remark || '智慧工厂园区', owner: item.owner || '运维组', deviceCount: item.deviceCount ?? 0 }
    case 'networkService':
      return { ...base, port: item.channel || item.portName || `${item.type || '接入'}服务通道`, uplink: item.uplink || `${item.upMessages || 0} msg`, downlink: item.downlink || `${item.downMessages || 0} msg`, enabled: !['STOPPED', 'DISABLED'].includes(status) }
    case 'script':
      return { ...base, version: item.version || 'v1.0', description: item.description || item.remark || '报文解析脚本' }
    case 'rule':
      return { ...base, level: cnLevel(item.alarmLevel || item.level), action: item.action || '生成告警并写入规则审计', enabled: item.enabled ?? status !== 'DISABLED' }
    case 'ruleAudit':
      return { ...base, ruleName: item.ruleName || `规则#${item.ruleId || '-'}`, device: item.device || `设备#${item.deviceId || '-'}`, time: timeText(item.createdAt) }
    case 'screen':
    case 'screenGroup':
      return { ...base, group: item.group || item.groupName || '默认分组', resolution: item.resolution || '1920×1080', screenCount: item.screenCount || 1, owner: item.owner || '生产指挥中心' }
    case 'gbDevice':
      return { ...base, channel: item.channel || item.channelNo || item.code, enabled: status !== 'DISABLED' }
    case 'streamProxy':
      return { ...base, source: item.source || item.streamUrl || item.playUrl || '厂区摄像机视频源', target: item.target || item.protocol || '网页播放', enabled: status !== 'DISABLED' }
    case 'videoAlarm':
      return { ...base, device: item.device || `视频设备#${item.videoDeviceId || '-'}`, type: item.type || item.algorithm || '区域入侵', level: cnLevel(item.level), time: timeText(item.createdAt) }
    case 'task':
      return { ...base, status: item.running || status === 'RUNNING' ? 'running' : 'stopped', lastRun: timeText(item.updatedAt || item.createdAt), successRate: item.successRate || '100%' }
    case 'taskLog':
      return { ...base, taskName: item.taskName || item.name || `任务#${item.taskId || '-'}`, message: item.message || item.result || item.detail, time: timeText(item.executeTime || item.createdAt), status: item.status === 'SUCCESS' ? '成功' : (item.status || '成功') }
    case 'user':
      return { ...base, nickname: item.nickname || item.realName || item.username, role: item.role || item.roleName || '平台管理员' }
    case 'firmware':
      return { ...base, product: item.product || item.targetProduct || 'PandaX工业网关', size: item.size || '12MB', status: item.upgradeStatus || status }
    case 'operationLog':
      return { ...base, user: item.user || item.operator || 'admin', module: item.module || item.moduleName, result: item.result || '成功', time: timeText(item.createdAt) }
    case 'loginLog':
      return { ...base, browser: item.browser || 'Chrome', result: item.success === false ? '失败' : '成功', time: timeText(item.createdAt) }
    case 'aiAgent':
      return { ...base, title: item.name, scenario: item.scenario || item.remark || '平台智能辅助', description: item.description || item.remark || '面向工业互联网运行场景提供建议', model: item.modelName || '平台默认模型', enabled: item.enabled ?? status !== 'DISABLED' }
    default:
      return base
  }
}

const adaptList = (kind, payload) => {
  if (kind === 'screenGroup' && Array.isArray(payload)) {
    const groups = new Map()
    payload.forEach((item) => {
      const name = item.groupName || item.group || '默认分组'
      const group = groups.get(name) || { id: name, name, screenCount: 0, owner: '生产指挥中心', status: 'ENABLED' }
      group.screenCount += 1
      groups.set(name, group)
    })
    return Array.from(groups.values())
  }
  if (['serviceMonitor', 'mediaService'].includes(kind) && payload && !Array.isArray(payload)) {
    return [
      { id: 'api', name: '平台业务服务', monitorObject: '业务服务模块', latency: '18ms', qps: 42, status: payload.http === 'running' ? 'healthy' : 'stopped', enabled: payload.http === 'running', host: '应用服务节点', cpu: `${payload.cpu || 0}%`, memory: `${payload.memory || 0}%` },
      { id: 'mqtt', name: 'MQTT接入服务', monitorObject: '设备接入模块', latency: '26ms', qps: 18, status: payload.mqtt === 'running' ? 'healthy' : 'stopped', enabled: payload.mqtt === 'running', host: '接入服务节点', cpu: `${payload.cpu || 0}%`, memory: `${payload.memory || 0}%` },
      { id: 'jvm', name: '运行资源', monitorObject: '运行线程监控', latency: `${payload.jvmThreads || 0} 线程`, qps: payload.disk || 0, status: 'healthy', enabled: true, host: '后台运行节点', cpu: `${payload.cpu || 0}%`, memory: `${payload.memory || 0}%` }
    ]
  }
  if (kind === 'historicalData' && Array.isArray(payload)) {
    return payload.flatMap((item) => [
      item.temperature !== null && item.temperature !== undefined ? { id: `${item.id}-temperature`, deviceName: item.deviceName, metric: 'temperature', value: item.temperature, unit: '℃', time: timeText(item.reportTime) } : null,
      item.humidity !== null && item.humidity !== undefined ? { id: `${item.id}-humidity`, deviceName: item.deviceName, metric: 'humidity', value: item.humidity, unit: '%', time: timeText(item.reportTime) } : null,
      item.pressure !== null && item.pressure !== undefined ? { id: `${item.id}-pressure`, deviceName: item.deviceName, metric: 'pressure', value: item.pressure, unit: 'kPa', time: timeText(item.reportTime) } : null
    ].filter(Boolean))
  }
  if (kind === 'historicalAlarm' && Array.isArray(payload)) {
    return payload.map((item) => ({ ...adaptItem(kind, item), title: item.title || item.name, value: item.value || item.content, status: alarmStatus(item.status), level: cnLevel(item.level), time: timeText(item.createdAt) }))
  }
  return Array.isArray(payload) ? payload.map((item) => adaptItem(kind, item)) : payload
}

export const authApi = {
  login: (payload) => http.post('/auth/login', payload)
}

export const dashboardApi = {
  get: async () => {
    const [summary, charts] = await Promise.all([
      http.get('/dashboard/summary'),
      http.get('/dashboard/charts')
    ])
    const deviceStatus = charts?.deviceStatus || {}
    const days = charts?.days || []
    const alarmTrend = normalizeSeries(charts?.alarmTrend, days)
    return {
      stats: {
        devices: summary?.devices,
        onlineDevices: summary?.onlineDevices,
        products: summary?.products,
        alarms: summary?.openAlarms ?? summary?.alarms,
        messages: summary?.telemetryToday,
        tasks: summary?.tasks
      },
      statusRatio: Object.entries(deviceStatus).map(([name, value]) => ({ name, value })),
      messageTrend: normalizeSeries(charts?.messageTrend, days),
      deviceGrowth: normalizeSeries(charts?.deviceGrowth, days),
      alarmTrend
    }
  }
}

export const resourceApi = {
  list: async (kind, params) => adaptList(kind, await http.get(endpointOf(kind), { params })),
  create: (kind, data) => http.post(endpointOf(kind), data),
  update: (kind, id, data) => http.put(`${endpointOf(kind)}/${id}`, data),
  remove: (kind, id) => http.delete(`${endpointOf(kind)}/${id}`),
  toggle: (kind, id) => http.post(`${endpointOf(kind)}/${id}/toggle`),
  custom: (kind, path, data) => http.post(`${endpointOf(kind)}${path}`, data)
}

export const telemetryApi = {
  simulate: (payload) => http.post('/telemetry/simulate', payload)
}

export const alarmApi = {
  handle: (id, payload) => http.post(`/alarms/${id}/handle`, payload)
}

export const taskApi = {
  logs: (params) => http.get('/tasks/logs', { params }),
  start: (id) => http.post(`/tasks/${id}/start`),
  stop: (id) => http.post(`/tasks/${id}/stop`)
}

export const aiAgentApi = {
  list: async () => adaptList('aiAgent', await http.get('/ai-agents')),
  chat: (payload) => http.post('/ai-agents/chat', payload, { timeout: 32000 }),
  models: (payload) => http.post('/ai-agents/models', payload, { timeout: 18000 })
}
