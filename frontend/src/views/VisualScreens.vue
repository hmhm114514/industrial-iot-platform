<template>
  <div class="page-stack visual-page">
    <section class="module-head visual-head">
      <div>
        <el-tag effect="dark" class="hero-tag">组态大屏</el-tag>
        <h1>可视化大屏</h1>
        <p>读取已配置的大屏方案，结合设备、告警和遥测数据生成运行驾驶舱。</p>
      </div>
      <div class="module-actions">
        <el-select v-model="selectedId" placeholder="选择大屏" style="width: 220px" @change="selectScreen">
          <el-option v-for="screen in screens" :key="screen.id" :label="screen.name" :value="screen.id" />
        </el-select>
        <el-button @click="openEditor">编辑布局</el-button>
        <el-button type="primary" :disabled="!currentScreen" @click="publishScreen">发布大屏</el-button>
      </div>
    </section>

    <el-alert
      v-if="fallbackMode"
      title="大屏服务暂不可用，当前展示本地驾驶舱模板"
      type="warning"
      show-icon
      :closable="false"
    />

    <div class="screen-preview dynamic-screen">
      <div class="screen-toolbar">
        <div>
          <span>{{ currentScreen?.groupName || currentScreen?.group || '默认分组' }}</span>
          <strong>{{ currentScreen?.name || '智慧工厂运行驾驶舱' }}</strong>
        </div>
        <el-tag :type="currentScreen?.published ? 'success' : 'info'" effect="dark">
          {{ currentScreen?.published ? '已发布' : '编辑中' }}
        </el-tag>
      </div>

      <div class="screen-grid dynamic-grid">
        <div v-for="widget in widgets" :key="widget.id" :class="['screen-panel', widget.span === 2 ? 'wide' : '', widget.tone || '']">
          <h3>{{ widget.title }}</h3>
          <strong v-if="widget.type === 'stat'">{{ widget.value }}</strong>
          <span v-if="widget.subtext">{{ widget.subtext }}</span>
          <div v-if="widget.type === 'trend'" class="trend-bars">
            <i v-for="item in widget.items" :key="item.name || item.label || item.date" :style="{ height: `${Math.max(8, Number(item.value || 0) * 10)}px` }"><em>{{ item.name || item.label || item.date }}</em></i>
          </div>
          <ul v-if="widget.type === 'list'">
            <li v-for="item in widget.items" :key="item.label || item.name">
              <span>{{ item.label || item.name }}</span><b>{{ item.value }}</b>
            </li>
          </ul>
          <div v-if="widget.type === 'topology'" class="node-map live-node-map">
            <i v-for="item in widget.items" :key="item.id || item.name" :class="item.tone" :style="item.style" />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="editorVisible" title="编辑大屏布局" width="760px">
      <el-alert title="可调整大屏标题、分组和组件 JSON；保存后预览页会按配置渲染。" type="info" show-icon :closable="false" />
      <el-form :model="editForm" label-width="90px" class="dialog-form">
        <el-form-item label="大屏名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="editForm.groupName" /></el-form-item>
        <el-form-item label="组件配置"><el-input v-model="editForm.configJson" type="textarea" :rows="12" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveScreen">保存布局</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi, resourceApi } from '../api/platform'
import { quietError } from '../api/http'

const screens = ref([])
const selectedId = ref(null)
const currentScreen = ref(null)
const dashboard = ref({ stats: {}, messageTrend: [], statusRatio: [], alarmTrend: [] })
const devices = ref([])
const alarms = ref([])
const fallbackMode = ref(false)
const editorVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ id: null, name: '', groupName: '', configJson: '' })

const defaultConfig = {
  widgets: [
    { id: 'device-stat', type: 'stat', title: '设备总数', metric: 'devices', span: 1 },
    { id: 'online-stat', type: 'stat', title: '在线设备', metric: 'onlineDevices', span: 1 },
    { id: 'alarm-stat', type: 'stat', title: '当前告警', metric: 'alarms', tone: 'warn', span: 1 },
    { id: 'message-trend', type: 'trend', title: '消息趋势', metric: 'messageTrend', span: 2 },
    { id: 'device-list', type: 'list', title: '设备运行状态', metric: 'devices', span: 1 },
    { id: 'topology', type: 'topology', title: '设备拓扑', metric: 'topology', span: 2 }
  ]
}

const config = computed(() => {
  try {
    const parsed = JSON.parse(currentScreen.value?.configJson || '')
    return parsed?.widgets?.length && typeof parsed.widgets[0] === 'object' ? parsed : defaultConfig
  } catch {
    return defaultConfig
  }
})

const widgets = computed(() => config.value.widgets.map((widget) => buildWidget(widget)))

const buildWidget = (widget) => {
  if (!widget || typeof widget !== 'object') return { id: String(widget || 'widget'), type: 'stat', title: '设备总数', metric: 'devices', value: 0, subtext: '平台统计' }
  const stats = dashboard.value.stats || {}
  if (widget.type === 'stat') {
    const value = Number(stats[widget.metric] || 0).toLocaleString()
    const subtext = widget.metric === 'onlineDevices' ? '实时通信中' : widget.metric === 'alarms' ? '待关注' : '平台统计'
    return { ...widget, value, subtext }
  }
  if (widget.type === 'trend') return { ...widget, items: dashboard.value[widget.metric] || [] }
  if (widget.type === 'list') return { ...widget, items: devices.value.slice(0, 5).map((item) => ({ label: item.name, value: item.online ? '在线' : '离线' })) }
  if (widget.type === 'topology') {
    return { ...widget, items: devices.value.slice(0, 8).map((item, index) => ({
      ...item,
      tone: item.online ? 'online' : alarms.value.some((alarm) => alarm.deviceName === item.name && alarm.status !== '已处理') ? 'alarm' : '',
      style: { left: `${12 + (index * 17) % 76}%`, top: `${24 + (index * 29) % 58}%` }
    })) }
  }
  return widget
}

const load = async () => {
  try {
    const [screenRows, dash, deviceRows, alarmRows] = await Promise.all([
      resourceApi.list('screen'),
      dashboardApi.get(),
      resourceApi.list('device'),
      resourceApi.list('historicalAlarm')
    ])
    screens.value = Array.isArray(screenRows) ? screenRows : []
    dashboard.value = dash || dashboard.value
    devices.value = Array.isArray(deviceRows) ? deviceRows : []
    alarms.value = Array.isArray(alarmRows) ? alarmRows : []
    fallbackMode.value = false
  } catch (error) {
    quietError(error, '大屏数据服务暂不可用，当前展示本地驾驶舱模板')
    fallbackMode.value = true
    screens.value = [{ id: 'local-screen', name: '智慧工厂运行驾驶舱', groupName: '默认分组', published: false, configJson: JSON.stringify(defaultConfig, null, 2) }]
    dashboard.value = { stats: { devices: 0, onlineDevices: 0, alarms: 0 }, messageTrend: [] }
    devices.value = []
    alarms.value = []
  }
  const next = screens.value.find((item) => item.published) || screens.value[0]
  if (next) selectScreen(next.id)
}

const selectScreen = (id) => {
  currentScreen.value = screens.value.find((item) => item.id === id) || screens.value[0]
  selectedId.value = currentScreen.value?.id
}

const openEditor = () => {
  const screen = currentScreen.value || { name: '智慧工厂运行驾驶舱', groupName: '默认分组', configJson: JSON.stringify(defaultConfig, null, 2) }
  Object.assign(editForm, { id: screen.id, name: screen.name, groupName: screen.groupName || screen.group || '默认分组', configJson: screen.configJson || JSON.stringify(defaultConfig, null, 2) })
  editorVisible.value = true
}

const saveScreen = async () => {
  saving.value = true
  try {
    JSON.parse(editForm.configJson || '{}')
    const payload = { name: editForm.name, groupName: editForm.groupName, configJson: editForm.configJson, status: currentScreen.value?.status || 'DRAFT', published: currentScreen.value?.published || false }
    if (editForm.id && editForm.id !== 'local-screen') await resourceApi.update('screen', editForm.id, payload)
    else await resourceApi.create('screen', payload)
    ElMessage.success('大屏布局已保存')
    editorVisible.value = false
    await load()
  } catch (error) {
    quietError(error, '大屏布局保存失败，请检查组件配置')
  } finally {
    saving.value = false
  }
}

const publishScreen = async () => {
  if (!currentScreen.value?.id || currentScreen.value.id === 'local-screen') return ElMessage.warning('请先保存大屏布局')
  try {
    await resourceApi.custom('screen', `/${currentScreen.value.id}/publish`)
    ElMessage.success('大屏已发布')
    await load()
  } catch (error) {
    quietError(error, '大屏发布失败，请稍后重试')
  }
}

onMounted(load)
</script>

<style scoped>
.screen-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 18px; }
.screen-toolbar div { display: flex; flex-direction: column; gap: 4px; }
.screen-toolbar span { color: #7db9ff; font-size: 13px; }
.screen-toolbar strong { color: #fff; font-size: 22px; }
.trend-bars { display: flex; align-items: end; gap: 10px; height: 150px; padding-top: 14px; }
.trend-bars i { flex: 1; min-width: 18px; border-radius: 10px 10px 4px 4px; background: linear-gradient(180deg, #38e1ff, #1277ff); position: relative; box-shadow: 0 0 16px rgba(56, 225, 255, .28); }
.trend-bars em { position: absolute; left: 50%; bottom: -26px; transform: translateX(-50%); color: #8fb4db; font-size: 12px; font-style: normal; white-space: nowrap; }
.screen-panel ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.screen-panel li { display: flex; justify-content: space-between; gap: 12px; padding: 10px 12px; border-radius: 12px; background: rgba(255, 255, 255, .05); }
.screen-panel li b { color: #fff; }
.live-node-map i.online { background: #19d27c; }
.live-node-map i.alarm { background: #ff4d5e; box-shadow: 0 0 0 10px rgba(255,77,94,.14); }
</style>
