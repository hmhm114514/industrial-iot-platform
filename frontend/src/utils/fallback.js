import { uid } from './data'

const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export const fallbackDashboard = {
  stats: {
    devices: 128,
    onlineDevices: 96,
    products: 18,
    alarms: 7,
    messages: 36820,
    tasks: 12
  },
  messageTrend: days.map((name, index) => ({ name, value: [2800, 3600, 4200, 5100, 6800, 5900, 7200][index] })),
  deviceGrowth: days.map((name, index) => ({ name, value: [8, 12, 9, 18, 15, 22, 19][index] })),
  statusRatio: [
    { name: '在线', value: 96 },
    { name: '离线', value: 21 },
    { name: '告警', value: 11 }
  ],
  alarmTrend: days.map((name, index) => ({ name, value: [2, 5, 3, 9, 6, 4, 7][index] }))
}

const commonTime = '2026-07-03 09:30:00'

export const fallbackRows = {
  deviceAttribute: [
    { id: 1, name: '是否启用告警', valueType: 'BOOL', valueTypeText: 'bool类型', minEnabled: false, maxEnabled: false, range: '未启用', status: 'enabled', createdAt: commonTime },
    { id: 2, name: '采样间隔', valueType: 'INT', valueTypeText: '整型', minEnabled: true, maxEnabled: true, minValue: 1, maxValue: 60, range: '1 ~ 60', status: 'enabled', createdAt: commonTime },
    { id: 3, name: '温度', valueType: 'FLOAT', valueTypeText: '浮点型', minEnabled: true, maxEnabled: true, minValue: -20, maxValue: 120, range: '-20 ~ 120', status: 'enabled', createdAt: commonTime }
  ],
  productCategory: [
    { id: 1, name: '环境监测', code: 'ENV', description: '温湿度、气体、水质等传感产品', status: 'enabled', createdAt: commonTime },
    { id: 2, name: '工业控制', code: 'CTRL', description: 'PLC、网关、执行机构', status: 'enabled', createdAt: commonTime }
  ],
  product: [
    { id: 1, name: '温湿度采集器 TH-200', code: 'TH-200', category: '环境监测', protocol: 'MQTT', deviceCount: 36, status: 'enabled', createdAt: commonTime },
    { id: 2, name: '边缘网关 EG-4G', code: 'EG-4G', category: '工业控制', protocol: 'HTTP', deviceCount: 12, status: 'enabled', createdAt: commonTime }
  ],
  deviceGroup: [
    { id: 1, name: '一号厂房', code: 'PLANT-A', location: '南京 / 江宁', owner: '运维一组', attributeIds: '1,2', attributeNames: '是否启用告警，采样间隔', deviceCount: 45, status: 'enabled' },
    { id: 2, name: '仓储冷链', code: 'COLD', location: '苏州 / 昆山', owner: '运维二组', attributeIds: '1,3', attributeNames: '是否启用告警，温度', deviceCount: 18, status: 'enabled' }
  ],
  device: [
    { id: 1, name: 'A区温度传感器-01', code: 'DEV-TH-001', product: '温湿度采集器 TH-200', group: '一号厂房', online: true, status: 'enabled', temperature: 31.6, location: '118.84,31.95', lastSeen: commonTime },
    { id: 2, name: '北门边缘网关', code: 'DEV-EG-009', product: '边缘网关 EG-4G', group: '一号厂房', online: false, status: 'disabled', temperature: 26.2, location: '118.83,31.94', lastSeen: '2026-07-02 20:18:20' }
  ],
  networkService: [
    { id: 1, name: 'MQTT 接入服务', type: 'MQTT', port: '设备接入通道', status: 'running', uplink: '12.8MB/min', downlink: '3.2MB/min', enabled: true },
    { id: 2, name: 'HTTP 数据服务', type: 'HTTP', port: '数据上报通道', status: 'running', uplink: '8.1MB/min', downlink: '1.5MB/min', enabled: true }
  ],
  script: [
    { id: 1, name: '温湿度 JSON 解析', language: 'JavaScript', version: 'v1.2', status: 'enabled', description: '解析 temperature/humidity 字段' },
    { id: 2, name: '网关透传脚本', language: 'Groovy', version: 'v1.0', status: 'draft', description: '提取子设备数据点' }
  ],
  rule: [
    { id: 1, name: '温度超 38℃ 告警', metric: '温度', operator: '>', threshold: 38, level: '高', action: '生成告警并通知运维', enabled: true, status: 'enabled' }
  ],
  ruleAudit: [
    { id: 1, ruleName: '温度超 38℃ 告警', device: 'A区温度传感器-01', result: '通过', detail: '31.6℃ 未触发', time: commonTime },
    { id: 2, ruleName: '温度超 38℃ 告警', device: 'B区温度传感器-03', result: '触发', detail: '42.1℃ 生成高温告警', time: commonTime }
  ],
  screenGroup: [
    { id: 1, name: '园区总览', screenCount: 3, owner: '展示中心', status: 'enabled' },
    { id: 2, name: '产线监控', screenCount: 5, owner: '制造部', status: 'enabled' }
  ],
  screen: [
    { id: 1, name: '智慧工厂驾驶舱', group: '园区总览', resolution: '1920×1080', status: 'published', updatedAt: commonTime },
    { id: 2, name: '设备运行态势屏', group: '产线监控', resolution: '3840×2160', status: 'draft', updatedAt: commonTime }
  ],
  gbDevice: [
    { id: 1, name: '厂区门岗球机', channel: '34020000001320000001', status: 'online', location: '北门', enabled: true },
    { id: 2, name: '仓库枪机', channel: '34020000001320000002', status: 'offline', location: '冷链仓库', enabled: false }
  ],
  streamProxy: [
    { id: 1, name: '北门视频转发', source: '北门球机视频源', target: '网页播放', status: 'running', enabled: true },
    { id: 2, name: '厂房直播代理', source: '厂房巡检视频源', target: '低延迟播放', status: 'stopped', enabled: false }
  ],
  mediaService: [
    { id: 1, name: '流媒体主节点', host: '主节点', cpu: '38%', memory: '54%', status: 'healthy', enabled: true },
    { id: 2, name: '录像存储节点', host: '存储节点', cpu: '41%', memory: '63%', status: 'healthy', enabled: true }
  ],
  videoRecord: [
    { id: 1, device: '厂区门岗球机', period: '2026-07-03 08:00 ~ 09:00', size: '1.2GB', status: 'ready' },
    { id: 2, device: '仓库枪机', period: '2026-07-02 20:00 ~ 21:00', size: '980MB', status: 'archived' }
  ],
  videoAlarm: [
    { id: 1, device: '厂区门岗球机', type: '越界检测', level: '中', status: '未处理', time: commonTime },
    { id: 2, device: '仓库枪机', type: '遮挡告警', level: '低', status: '已处理', time: commonTime }
  ],
  historicalData: [
    { id: 1, deviceName: 'A区温度传感器-01', metric: 'temperature', value: 31.6, unit: '℃', time: commonTime },
    { id: 2, deviceName: 'A区温度传感器-01', metric: 'humidity', value: 56, unit: '%RH', time: commonTime },
    { id: 3, deviceName: 'B区温度传感器-03', metric: 'temperature', value: 42.1, unit: '℃', time: '2026-07-03 10:02:11' }
  ],
  historicalAlarm: [
    { id: 1, title: 'B区温度过高', deviceName: 'B区温度传感器-03', level: '高', ruleName: '温度超 38℃ 告警', status: '未处理', value: '42.1℃', time: '2026-07-03 10:02:11', handler: '-' },
    { id: 2, title: '网关离线', deviceName: '北门边缘网关', level: '中', ruleName: '网关离线告警', status: '已处理', value: '离线 20 分钟', time: commonTime, handler: 'admin' }
  ],
  serviceMonitor: [
    { id: 1, name: '认证服务', monitorObject: '用户认证服务', latency: '23ms', qps: 32, status: 'healthy', enabled: true },
    { id: 2, name: '遥测服务', monitorObject: '设备数据服务', latency: '41ms', qps: 118, status: 'healthy', enabled: true }
  ],
  user: [
    { id: 1, username: 'admin', nickname: '平台管理员', role: '超级管理员', phone: '13800000000', status: 'enabled', createdAt: commonTime },
    { id: 2, username: 'operator', nickname: '运维人员', role: '运维角色', phone: '13900000000', status: 'enabled', createdAt: commonTime }
  ],
  role: [
    { id: 1, name: '超级管理员', code: 'ADMIN', permissions: '全部菜单与数据权限', status: 'enabled' },
    { id: 2, name: '运维角色', code: 'OPS', permissions: '设备、告警、任务', status: 'enabled' }
  ],
  firmware: [
    { id: 1, name: 'TH-200 固件', version: '2.1.0', product: '温湿度采集器 TH-200', size: '6.4MB', status: 'released', enabled: true },
    { id: 2, name: 'EG-4G 网关固件', version: '1.9.3', product: '边缘网关 EG-4G', size: '18.2MB', status: 'testing', enabled: false }
  ],
  operationLog: [
    { id: 1, user: 'admin', module: '设备管理', action: '新增设备', ip: '本地演示终端', time: commonTime, result: '成功' },
    { id: 2, user: 'admin', module: '规则引擎', action: '修改阈值', ip: '本地演示终端', time: commonTime, result: '成功' }
  ]
}

export const withNewRow = (rows, row) => [{ id: uid('row'), ...row }, ...rows]
