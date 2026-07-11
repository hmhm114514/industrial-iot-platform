export const menus = [
  { title: '控制台', path: '/dashboard', icon: 'DataBoard' },
  {
    title: '设备管理',
    icon: 'Box',
    children: [
      { title: '产品分类', path: '/device/product-categories', kind: 'productCategory' },
      { title: '产品管理', path: '/device/products', kind: 'product' },
      { title: '设备分组', path: '/device/groups', kind: 'deviceGroup' },
      { title: '设备管理', path: '/device/devices', kind: 'device' },
      { title: '设备地图', path: '/device/map', special: 'DeviceMap' }
    ]
  },
  {
    title: '服务管理',
    icon: 'Connection',
    children: [
      { title: '网络服务', path: '/service/network', kind: 'networkService' },
      { title: '解析脚本', path: '/service/scripts', kind: 'script' }
    ]
  },
  {
    title: '规则引擎',
    icon: 'Share',
    children: [
      { title: '规则设计', path: '/rules/design', special: 'Rules' },
      { title: '规则审计', path: '/rules/audit', kind: 'ruleAudit' }
    ]
  },
  {
    title: '组态大屏',
    icon: 'Monitor',
    children: [
      { title: '大屏分组', path: '/screen/groups', kind: 'screenGroup' },
      { title: '组态大屏', path: '/screen/designer', kind: 'screen', special: 'ScreenDesigner' },
      { title: '可视化大屏', path: '/screen/visual', special: 'VisualScreens' }
    ]
  },
  {
    title: '视频中心',
    icon: 'VideoCamera',
    children: [
      { title: '监控设备', path: '/video/devices', special: 'LocalCameraDevices' },
      { title: '监控查看', path: '/video/monitor', special: 'VideoSquare' }
    ]
  },
  {
    title: '数据中心',
    icon: 'DataAnalysis',
    children: [
      { title: '历史数据', path: '/data/history', special: 'HistoryData' },
      { title: '历史告警', path: '/data/alarms', special: 'HistoryAlarms' },
      { title: '服务监控', path: '/data/service-monitor', kind: 'serviceMonitor' }
    ]
  },
  {
    title: '任务中心',
    icon: 'Timer',
    children: [
      { title: '任务中心', path: '/tasks', special: 'Tasks' },
      { title: '任务日志', path: '/tasks/logs', kind: 'taskLog' }
    ]
  },
  {
    title: '系统设置',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/users', kind: 'user' },
      { title: '角色管理', path: '/system/roles', kind: 'role' },
      { title: '固件管理', path: '/system/firmwares', kind: 'firmware' },
      { title: '操作日志', path: '/system/operation-logs', kind: 'operationLog' },
      { title: '登录日志', path: '/system/login-logs', kind: 'loginLog' }
    ]
  },
  {
    title: '扩展能力',
    icon: 'MagicStick',
    children: [
      { title: 'AI智能体', path: '/extension/ai-agent', kind: 'aiAgent', special: 'AiAgent' },
      { title: '开发工具', path: '/extension/dev-tools', kind: 'devTool', special: 'DevTools' }
    ]
  }
]

export const isSuperAdmin = (user = {}) => {
  const role = user.roleName || user.role || ''
  return role === '超级管理员' || role === 'SUPER_ADMIN' || role === 'admin'
}

export const filterMenusByRole = (items = menus, user = {}) => items
  .filter((item) => item.title !== '系统设置' || isSuperAdmin(user))
  .map((item) => item.children ? { ...item, children: filterMenusByRole(item.children, user) } : { ...item })

export const flattenMenus = (items = menus, parentTitle = '') =>
  items.flatMap((item) => {
    const crumb = parentTitle ? `${parentTitle} / ${item.title}` : item.title
    if (item.children?.length) return flattenMenus(item.children, item.title)
    return [{ ...item, crumb }]
  })

export const findMenuTrail = (path, items = menus, parents = []) => {
  for (const item of items) {
    const next = [...parents, item.title]
    if (item.path === path) return next
    if (item.children) {
      const found = findMenuTrail(path, item.children, next)
      if (found) return found
    }
  }
  return ['控制台']
}
