const statusColumn = { prop: 'status', label: '状态', type: 'status', width: 110 }
const enabledColumn = { prop: 'enabled', label: '启停', type: 'switch', width: 100 }

const textField = (prop, label, extra = {}) => ({ prop, label, type: 'text', ...extra })
const selectField = (prop, label, options, extra = {}) => ({ prop, label, type: 'select', options, ...extra })
const numberField = (prop, label, extra = {}) => ({ prop, label, type: 'number', ...extra })

export const resourceConfigs = {
  productCategory: {
    title: '产品分类',
    subtitle: '按业务场景组织产品模型，便于统一权限和统计。',
    accent: '分类',
    searchable: '分类名称 / 编码',
    columns: [
      { prop: 'name', label: '分类名称', minWidth: 160 },
      { prop: 'code', label: '分类编码', width: 130 },
      { prop: 'description', label: '说明', minWidth: 220 },
      statusColumn,
      { prop: 'createdAt', label: '创建时间', width: 180 }
    ],
    fields: [textField('name', '分类名称'), textField('code', '分类编码'), textField('description', '说明')]
  },
  product: {
    title: '产品管理',
    subtitle: '定义物模型、协议类型和接入方式，是设备接入的基础。',
    accent: '产品',
    searchable: '产品名称 / 编码',
    columns: [
      { prop: 'name', label: '产品名称', minWidth: 190 },
      { prop: 'code', label: '产品编码', width: 140 },
      { prop: 'category', label: '所属分类', width: 130 },
      { prop: 'protocol', label: '接入协议', width: 110, type: 'tag' },
      { prop: 'deviceCount', label: '设备数', width: 90 },
      statusColumn,
      { prop: 'createdAt', label: '创建时间', width: 180 }
    ],
    fields: [
      textField('name', '产品名称'),
      textField('code', '产品编码'),
      textField('category', '所属分类'),
      selectField('protocol', '接入协议', ['MQTT', 'HTTP', 'TCP', 'CoAP', 'GB28181']),
      numberField('deviceCount', '设备数')
    ]
  },
  deviceGroup: {
    title: '设备分组',
    subtitle: '用厂区、产线或项目维度管理设备资产。',
    accent: '分组',
    searchable: '分组名称 / 位置',
    columns: [
      { prop: 'name', label: '分组名称', minWidth: 160 },
      { prop: 'code', label: '编码', width: 130 },
      { prop: 'location', label: '位置', minWidth: 160 },
      { prop: 'owner', label: '负责人', width: 120 },
      { prop: 'deviceCount', label: '设备数', width: 90 },
      statusColumn
    ],
    fields: [textField('name', '分组名称'), textField('code', '编码'), textField('location', '位置'), textField('owner', '负责人'), numberField('deviceCount', '设备数')]
  },
  device: {
    title: '设备管理',
    subtitle: '维护设备档案、在线状态和最新遥测数据，可模拟上报温度。',
    accent: '设备',
    searchable: '设备名称 / 编码 / 分组',
    card: true,
    columns: [
      { prop: 'name', label: '设备名称', minWidth: 190 },
      { prop: 'code', label: '设备编码', width: 150 },
      { prop: 'product', label: '产品', minWidth: 170 },
      { prop: 'group', label: '分组', width: 120 },
      { prop: 'online', label: '在线', type: 'online', width: 90 },
      { prop: 'temperature', label: '温度℃', width: 100 },
      { prop: 'latitude', label: '纬度', width: 110 },
      { prop: 'longitude', label: '经度', width: 110 },
      statusColumn,
      { prop: 'lastSeen', label: '最后通信', width: 180 }
    ],
    fields: [
      textField('name', '设备名称'),
      textField('code', '设备编码'),
      textField('product', '所属产品'),
      textField('group', '所属分组'),
      numberField('temperature', '当前温度'),
      numberField('latitude', '纬度', { min: -90, max: 90 }),
      numberField('longitude', '经度', { min: -180, max: 180 }),
      textField('location', '位置')
    ]
  },
  networkService: {
    title: '网络服务',
    subtitle: '展示 MQTT、HTTP 等接入服务运行态势和吞吐状态。',
    accent: '服务',
    columns: [
      { prop: 'name', label: '服务名称', minWidth: 180 },
      { prop: 'type', label: '类型', type: 'tag', width: 100 },
      { prop: 'port', label: '服务通道', width: 110 },
      { prop: 'uplink', label: '上行速率', width: 130 },
      { prop: 'downlink', label: '下行速率', width: 130 },
      { prop: 'status', label: '运行状态', type: 'health', width: 110 },
      enabledColumn
    ],
    fields: [textField('name', '服务名称'), selectField('type', '类型', ['MQTT', 'HTTP', 'TCP', 'UDP']), numberField('port', '服务通道')]
  },
  script: {
    title: '解析脚本',
    subtitle: '用脚本把原始报文转换为平台统一数据点。',
    accent: '脚本',
    columns: [
      { prop: 'name', label: '脚本名称', minWidth: 180 },
      { prop: 'language', label: '语言', type: 'tag', width: 120 },
      { prop: 'version', label: '版本', width: 100 },
      { prop: 'description', label: '说明', minWidth: 220 },
      statusColumn
    ],
    fields: [textField('name', '脚本名称'), selectField('language', '语言', ['JavaScript', 'Groovy', 'Python']), textField('version', '版本'), textField('description', '说明')],
    preview: 'function decode(payload) { return JSON.parse(payload); }'
  },
  ruleAudit: {
    title: '规则审计',
    subtitle: '记录规则执行结果，便于追踪告警产生过程。',
    readonly: true,
    columns: [
      { prop: 'ruleName', label: '规则名称', minWidth: 180 },
      { prop: 'device', label: '设备', minWidth: 160 },
      { prop: 'result', label: '执行结果', type: 'alarm', width: 110 },
      { prop: 'detail', label: '详情', minWidth: 220 },
      { prop: 'time', label: '时间', width: 180 }
    ],
    fields: []
  },
  screenGroup: {
    title: '大屏分组',
    subtitle: '从已配置大屏中聚合分组，避免分组与大屏数据混用。',
    accent: '分组',
    readonly: true,
    columns: [
      { prop: 'name', label: '分组名称', minWidth: 180 },
      { prop: 'screenCount', label: '大屏数量', width: 110 },
      { prop: 'owner', label: '归属', width: 130 },
      statusColumn
    ],
    fields: []
  },
  screen: {
    title: '组态大屏',
    subtitle: '以卡片预览方式管理可视化大屏，支持发布状态演示。',
    accent: '大屏',
    card: true,
    columns: [
      { prop: 'name', label: '大屏名称', minWidth: 180 },
      { prop: 'group', label: '分组', width: 130 },
      { prop: 'resolution', label: '分辨率', width: 130 },
      { prop: 'status', label: '发布状态', type: 'status', width: 110 },
      { prop: 'updatedAt', label: '更新时间', width: 180 }
    ],
    fields: [textField('name', '大屏名称'), textField('groupName', '分组'), textField('configJson', '组件配置 JSON')],
    preview: '数据中台 / 设备拓扑 / 告警态势 / 能耗排行'
  },
  gbDevice: {
    title: '国标设备',
    subtitle: '管理 GB28181 视频设备和通道在线状态。',
    accent: '国标设备',
    columns: [
      { prop: 'name', label: '设备名称', minWidth: 180 },
      { prop: 'channel', label: '通道编码', minWidth: 190 },
      { prop: 'location', label: '位置', width: 130 },
      { prop: 'status', label: '在线状态', type: 'health', width: 110 },
      enabledColumn
    ],
    fields: [textField('name', '设备名称'), textField('channel', '通道编码'), textField('location', '位置')]
  },
  streamProxy: {
    title: '拉流代理',
    subtitle: '配置视频来源与播放方式转换代理。',
    accent: '代理',
    columns: [
      { prop: 'name', label: '代理名称', minWidth: 180 },
      { prop: 'source', label: '视频来源', minWidth: 190 },
      { prop: 'target', label: '播放方式', width: 120 },
      { prop: 'status', label: '状态', type: 'health', width: 110 },
      enabledColumn
    ],
    fields: [textField('name', '代理名称'), textField('source', '视频来源'), textField('target', '播放方式')]
  },
  mediaService: {
    title: '流媒体服务',
    subtitle: '观察流媒体节点健康度、CPU 和内存负载。',
    accent: '节点',
    columns: [
      { prop: 'name', label: '节点名称', minWidth: 180 },
      { prop: 'host', label: '服务节点', width: 130 },
      { prop: 'cpu', label: 'CPU', width: 90 },
      { prop: 'memory', label: '内存', width: 90 },
      { prop: 'status', label: '健康度', type: 'health', width: 110 },
      enabledColumn
    ],
    fields: [textField('name', '节点名称'), textField('host', '服务节点')]
  },
  videoRecord: {
    title: '录像回放',
    subtitle: '按设备和时间段查看录像文件。',
    readonly: true,
    columns: [
      { prop: 'device', label: '设备', minWidth: 180 },
      { prop: 'period', label: '录像时间段', minWidth: 240 },
      { prop: 'size', label: '大小', width: 100 },
      { prop: 'status', label: '状态', type: 'status', width: 110 }
    ],
    fields: []
  },
  videoAlarm: {
    title: '视频告警',
    subtitle: '展示越界、遮挡等智能视频告警。',
    readonly: true,
    columns: [
      { prop: 'device', label: '设备', minWidth: 180 },
      { prop: 'type', label: '告警类型', width: 130 },
      { prop: 'level', label: '等级', type: 'alarm', width: 90 },
      { prop: 'status', label: '处置状态', type: 'status', width: 110 },
      { prop: 'time', label: '时间', width: 180 }
    ],
    fields: []
  },
  serviceMonitor: {
    title: '服务监控',
    subtitle: '监控平台业务服务可用性、延迟和吞吐。',
    readonly: true,
    columns: [
      { prop: 'name', label: '服务', minWidth: 160 },
      { prop: 'monitorObject', label: '监控对象', minWidth: 180 },
      { prop: 'latency', label: '延迟', width: 90 },
      { prop: 'qps', label: 'QPS', width: 90 },
      { prop: 'status', label: '健康度', type: 'health', width: 110 },
      enabledColumn
    ],
    fields: []
  },
  taskLog: {
    title: '任务日志',
    subtitle: '记录任务执行状态和输出信息。',
    readonly: true,
    columns: [
      { prop: 'taskName', label: '任务名称', minWidth: 180 },
      { prop: 'status', label: '结果', type: 'alarm', width: 100 },
      { prop: 'message', label: '执行信息', minWidth: 260 },
      { prop: 'time', label: '时间', width: 180 }
    ],
    fields: []
  },
  user: {
    title: '用户管理',
    subtitle: '维护系统用户、角色和启用状态。',
    accent: '用户',
    columns: [
      { prop: 'username', label: '账号', width: 130 },
      { prop: 'realName', label: '真实姓名', width: 140 },
      { prop: 'roleName', label: '角色', width: 140 },
      statusColumn,
      { prop: 'createdAt', label: '创建时间', width: 180 }
    ],
    fields: [
      textField('username', '账号'),
      textField('password', '密码', { type: 'password', placeholder: '新增用户必填；编辑留空则保留原密码' }),
      textField('realName', '真实姓名'),
      selectField('roleName', '角色', ['超级管理员', '平台管理员']),
      selectField('status', '状态', ['enabled', 'disabled'])
    ]
  },
  role: {
    title: '角色管理',
    subtitle: '用角色聚合菜单权限和数据权限。',
    accent: '角色',
    columns: [
      { prop: 'name', label: '角色名称', width: 150 },
      { prop: 'code', label: '角色编码', width: 130 },
      { prop: 'permissions', label: '权限说明', minWidth: 240 },
      statusColumn
    ],
    fields: [textField('name', '角色名称'), textField('code', '角色编码'), textField('permissions', '权限说明')]
  },
  firmware: {
    title: '固件管理',
    subtitle: '管理设备固件包、版本状态和发布流程。',
    accent: '固件',
    columns: [
      { prop: 'name', label: '固件名称', minWidth: 160 },
      { prop: 'version', label: '版本', width: 100 },
      { prop: 'product', label: '适配产品', minWidth: 170 },
      { prop: 'size', label: '大小', width: 90 },
      { prop: 'status', label: '状态', type: 'status', width: 110 },
      enabledColumn
    ],
    fields: [textField('name', '固件名称'), textField('version', '版本'), textField('product', '适配产品'), textField('size', '大小')]
  },
  operationLog: {
    title: '操作日志',
    subtitle: '审计用户在平台中的关键操作。',
    readonly: true,
    columns: [
      { prop: 'user', label: '用户', width: 110 },
      { prop: 'module', label: '模块', width: 130 },
      { prop: 'action', label: '操作', width: 140 },
      { prop: 'ip', label: '来源终端', width: 130 },
      { prop: 'result', label: '结果', type: 'alarm', width: 90 },
      { prop: 'time', label: '时间', width: 180 }
    ],
    fields: []
  },
  loginLog: {
    title: '登录日志',
    subtitle: '记录登录来源、浏览器和认证结果。',
    readonly: true,
    columns: [
      { prop: 'username', label: '账号', width: 130 },
      { prop: 'ip', label: '来源终端', width: 130 },
      { prop: 'browser', label: '浏览器', width: 120 },
      { prop: 'result', label: '结果', type: 'alarm', width: 90 },
      { prop: 'time', label: '时间', width: 180 }
    ],
    fields: []
  },
  aiAgent: {
    title: 'AI智能体',
    subtitle: '扩展模块：用智能助手解释告警、生成巡检建议。',
    readonly: true,
    columns: [
      { prop: 'name', label: '能力', minWidth: 160 },
      { prop: 'description', label: '说明', minWidth: 260 },
      { prop: 'status', label: '状态', type: 'status', width: 110 }
    ],
    fields: []
  },
  devTool: {
    title: '开发工具',
    subtitle: '扩展模块：联调工作台、代码生成和报文模拟。',
    readonly: true,
    columns: [
      { prop: 'name', label: '工具', minWidth: 160 },
      { prop: 'description', label: '说明', minWidth: 260 },
      { prop: 'status', label: '状态', type: 'status', width: 110 }
    ],
    fields: []
  }
}

export const fallbackExtra = {
  aiAgent: [
    { id: 1, name: '告警解释助手', description: '根据设备、阈值和历史数据生成处置建议', status: 'demo' },
    { id: 2, name: '巡检计划助手', description: '根据离线率和告警趋势生成巡检清单', status: 'demo' }
  ],
  devTool: [
    { id: 1, name: '联调工作台', description: '保存常用业务联调模板', status: 'demo' },
    { id: 2, name: '报文模拟器', description: '生成 MQTT/HTTP 测试报文', status: 'demo' }
  ]
}
