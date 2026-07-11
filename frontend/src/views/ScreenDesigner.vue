<template>
  <div class="screen-designer-page">
    <header class="designer-toolbar">
      <div>
        <span class="designer-kicker">组态大屏</span>
        <strong>{{ currentScreen?.name || '设备数据可视化大屏' }}</strong>
      </div>
      <div class="toolbar-actions">
        <el-select v-model="selectedScreenId" placeholder="选择大屏" style="width: 190px" @change="selectScreen">
          <el-option v-for="screen in screens" :key="screen.id" :label="screen.name" :value="screen.id" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="saveLayout">保存</el-button>
      </div>
    </header>

    <div class="designer-workspace">
      <aside class="component-rail">
        <span class="rail-title">组件库</span>
        <button class="component-button" type="button" @click="addWidget">
          <DataLine />
          <strong>实时图表</strong>
          <small>设备遥测</small>
        </button>
      </aside>

      <main class="designer-stage">
        <div class="stage-head">
          <div><i />实时数据已连接</div>
          <span>{{ widgets.length }} 个组件</span>
        </div>
        <div v-if="widgets.length" class="widget-grid">
          <ScreenTelemetryWidget
            v-for="widget in widgets"
            :key="widget.id"
            :widget="widget"
            :rows="telemetryRows"
            :device-name="deviceName(widget.deviceId)"
            :metric-label="metricMeta(widget.metric).label"
            :metric-unit="metricMeta(widget.metric).unit"
            :selected="selectedWidgetId === widget.id"
            @select="selectedWidgetId = widget.id"
          />
        </div>
        <button v-else class="empty-stage" type="button" @click="addWidget">
          <CirclePlus />
          <strong>添加第一个设备可视化</strong>
          <span>从设备数据开始搭建大屏</span>
        </button>
      </main>

      <aside class="property-panel">
        <template v-if="selectedWidget">
          <div class="property-title">
            <div><span>属性配置</span><strong>设备数据可视化</strong></div>
            <el-button circle text type="danger" :icon="Delete" title="删除组件" @click="removeWidget" />
          </div>

          <label>组件标题</label>
          <el-input v-model="selectedWidget.title" placeholder="例如：设备温度趋势" />

          <label>选择设备</label>
          <el-select v-model="selectedWidget.deviceId" filterable placeholder="请选择设备" @change="handleDeviceChange">
            <el-option v-for="device in devices" :key="device.id" :label="`${device.name} · ${device.code || device.id}`" :value="device.id" />
          </el-select>

          <label>数据指标</label>
          <el-select v-model="selectedWidget.metric" placeholder="请选择指标">
            <el-option v-for="metric in telemetryMetrics" :key="metric.value" :label="metric.label" :value="metric.value" />
          </el-select>

          <label>可视化方式</label>
          <div class="chart-type-grid">
            <button v-for="type in chartTypes" :key="type.value" type="button" :class="{ active: selectedWidget.chartType === type.value }" @click="selectedWidget.chartType = type.value">
              <component :is="type.icon" />
              <span>{{ type.label }}</span>
            </button>
          </div>

          <template v-if="selectedWidget.chartType === 'gauge'">
            <div class="number-range">
              <div><label>最小值</label><el-input-number v-model="selectedWidget.min" :controls="false" /></div>
              <div><label>最大值</label><el-input-number v-model="selectedWidget.max" :controls="false" /></div>
            </div>
          </template>

          <label>显示数据点</label>
          <el-slider v-model="selectedWidget.limit" :min="5" :max="50" :step="5" show-input />

          <label>主题颜色</label>
          <div class="color-row">
            <button v-for="color in colors" :key="color" type="button" :class="{ active: selectedWidget.color === color }" :style="{ backgroundColor: color }" :title="color" @click="selectedWidget.color = color" />
          </div>
        </template>
        <div v-else class="empty-property">
          <Setting />
          <strong>选择一个组件</strong>
          <span>可配置设备、指标和可视化方式</span>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Check, CirclePlus, DataLine, Delete, Histogram, Odometer, Refresh, Setting, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ScreenTelemetryWidget from '../components/ScreenTelemetryWidget.vue'
import { resourceApi, telemetryMetrics } from '../api/platform'
import { quietError } from '../api/http'

const screens = ref([])
const devices = ref([])
const telemetryRows = ref([])
const widgets = ref([])
const legacyWidgets = ref([])
const selectedScreenId = ref(null)
const selectedWidgetId = ref(null)
const loading = ref(false)
const saving = ref(false)

const colors = ['#35d6ed', '#4f8cff', '#34d399', '#f7b955', '#ff6b81']
const chartTypes = [
  { label: '折线图', value: 'line', icon: TrendCharts },
  { label: '面积图', value: 'area', icon: DataLine },
  { label: '柱状图', value: 'bar', icon: Histogram },
  { label: '仪表盘', value: 'gauge', icon: Odometer },
  { label: '数字卡', value: 'number', icon: Setting }
]

const currentScreen = computed(() => screens.value.find((screen) => String(screen.id) === String(selectedScreenId.value)))
const selectedWidget = computed(() => widgets.value.find((widget) => widget.id === selectedWidgetId.value))

const parseScreen = (screen) => {
  let config = {}
  try { config = JSON.parse(screen?.configJson || '{}') } catch { config = {} }
  const all = Array.isArray(config.widgets) ? config.widgets : []
  widgets.value = all.filter((widget) => widget?.type === 'device-telemetry').map(normalizeWidget)
  legacyWidgets.value = all.filter((widget) => widget?.type !== 'device-telemetry')
  selectedWidgetId.value = widgets.value[0]?.id || null
}

const normalizeWidget = (widget) => ({
  id: widget.id || `telemetry-${Date.now()}`,
  type: 'device-telemetry', title: widget.title || '设备实时数据', deviceId: widget.deviceId || '',
  metric: widget.metric || 'temperature', chartType: widget.chartType || 'line', limit: Number(widget.limit || 20),
  color: widget.color || colors[0], min: Number(widget.min || 0), max: Number(widget.max || 100)
})

const selectScreen = (id) => parseScreen(screens.value.find((screen) => String(screen.id) === String(id)))
const deviceName = (id) => devices.value.find((device) => String(device.id) === String(id))?.name || '未选择设备'
const metricMeta = (value) => telemetryMetrics.find((metric) => metric.value === value) || { label: value || '设备指标', unit: '' }

const loadData = async () => {
  loading.value = true
  try {
    const [screenRows, deviceRows, telemetry] = await Promise.all([
      resourceApi.list('screen'), resourceApi.list('device'), resourceApi.list('historicalData')
    ])
    screens.value = Array.isArray(screenRows) ? screenRows : []
    devices.value = Array.isArray(deviceRows) ? deviceRows : []
    telemetryRows.value = Array.isArray(telemetry) ? telemetry : []
    const nextId = screens.value.some((item) => String(item.id) === String(selectedScreenId.value))
      ? selectedScreenId.value : screens.value[0]?.id
    selectedScreenId.value = nextId || null
    if (nextId) selectScreen(nextId)
  } catch (error) {
    quietError(error, '组态大屏数据加载失败')
  } finally {
    loading.value = false
  }
}

const realtimeRow = (payload = {}) => {
  const telemetry = payload.telemetry || {}
  let body = {}
  try { body = telemetry.payload ? JSON.parse(telemetry.payload) : {} } catch { body = {} }
  const metricValues = Object.fromEntries(telemetryMetrics.map((metric) => [
    metric.value, telemetry[metric.value] ?? body[metric.value]
  ]).filter(([, value]) => value !== null && value !== undefined))
  return {
    ...telemetry,
    deviceId: telemetry.deviceId ?? payload.deviceId,
    deviceName: telemetry.deviceName ?? payload.deviceName,
    metricValues,
    time: String(telemetry.reportTime || new Date().toISOString()).replace('T', ' ').slice(0, 19)
  }
}

const addWidget = () => {
  const firstDevice = devices.value[0]
  const widget = normalizeWidget({
    id: `telemetry-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    deviceId: firstDevice?.id || '', title: firstDevice ? `${firstDevice.name}温度` : '设备实时数据'
  })
  widgets.value.push(widget)
  selectedWidgetId.value = widget.id
}

const handleDeviceChange = (id) => {
  const device = devices.value.find((item) => String(item.id) === String(id))
  if (device && selectedWidget.value) selectedWidget.value.title = `${device.name}${metricMeta(selectedWidget.value.metric).label.split(' ')[0]}`
}

const removeWidget = () => {
  const index = widgets.value.findIndex((widget) => widget.id === selectedWidgetId.value)
  if (index < 0) return
  widgets.value.splice(index, 1)
  selectedWidgetId.value = widgets.value[Math.min(index, widgets.value.length - 1)]?.id || null
}

const saveLayout = async () => {
  saving.value = true
  try {
    const configJson = JSON.stringify({ widgets: [...legacyWidgets.value, ...widgets.value] }, null, 2)
    if (currentScreen.value?.id) {
      await resourceApi.update('screen', currentScreen.value.id, {
        name: currentScreen.value.name,
        code: currentScreen.value.code,
        status: currentScreen.value.status || 'DRAFT',
        remark: currentScreen.value.remark,
        groupName: currentScreen.value.groupName || currentScreen.value.group || '默认分组',
        published: Boolean(currentScreen.value.published),
        configJson
      })
    } else {
      const created = await resourceApi.create('screen', { name: '设备数据可视化大屏', groupName: '默认分组', status: 'DRAFT', published: false, configJson })
      selectedScreenId.value = created?.id
    }
    ElMessage.success('大屏配置已保存')
    await loadData()
  } catch (error) {
    quietError(error, '大屏配置保存失败')
  } finally {
    saving.value = false
  }
}

const handleRealtime = (event) => {
  if (event.detail?.type !== 'telemetry') return
  const payload = event.detail?.payload || {}
  if (!widgets.value.some((widget) => String(widget.deviceId) === String(payload.deviceId))) return
  const row = realtimeRow(payload)
  telemetryRows.value = [row, ...telemetryRows.value.filter((item) => String(item.id) !== String(row.id))]
}

onMounted(() => {
  loadData()
  window.addEventListener('iot-realtime', handleRealtime)
})
onBeforeUnmount(() => window.removeEventListener('iot-realtime', handleRealtime))
</script>

<style scoped>
.screen-designer-page { min-height: calc(100vh - 118px); display: flex; flex-direction: column; overflow: hidden; border: 1px solid #dce7f2; border-radius: 8px; background: #071423; }
.designer-toolbar { height: 64px; flex: 0 0 auto; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 0 18px; border-bottom: 1px solid #dfe8f1; background: #fff; }
.designer-toolbar > div:first-child { display: flex; flex-direction: column; gap: 3px; }
.designer-toolbar strong { color: #172b42; font-size: 16px; }
.designer-kicker { color: #6b8298; font-size: 11px; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.designer-workspace { min-height: 0; flex: 1; display: grid; grid-template-columns: 116px minmax(0, 1fr) 300px; }
.component-rail { padding: 18px 12px; border-right: 1px solid #203852; background: #10243e; }
.rail-title { display: block; margin: 0 0 12px 4px; color: #7fa1bd; font-size: 11px; }
.component-button { width: 100%; aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; border: 1px solid #285478; border-radius: 6px; color: #d9f7ff; background: #173b62; cursor: pointer; }
.component-button:hover { border-color: #35d6ed; background: #19466f; }
.component-button svg { width: 25px; color: #47d8ed; }
.component-button strong { font-size: 12px; }
.component-button small { color: #789ab7; font-size: 10px; }
.designer-stage { min-width: 0; padding: 18px; overflow: auto; background-color: #081525; background-image: linear-gradient(rgba(55, 100, 137, .12) 1px, transparent 1px), linear-gradient(90deg, rgba(55, 100, 137, .12) 1px, transparent 1px); background-size: 24px 24px; }
.stage-head { display: flex; justify-content: space-between; margin-bottom: 14px; color: #688ca9; font-size: 11px; }
.stage-head div { display: flex; align-items: center; gap: 7px; color: #61dca8; }
.stage-head i { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 8px #34d399; }
.widget-grid { display: grid; grid-template-columns: repeat(2, minmax(300px, 1fr)); gap: 14px; }
.empty-stage { width: 100%; min-height: 430px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px; border: 1px dashed #315879; border-radius: 8px; color: #7399b8; background: rgba(10, 33, 56, .56); cursor: pointer; }
.empty-stage:hover { border-color: #35d6ed; color: #9adfec; }
.empty-stage svg { width: 40px; }
.empty-stage strong { color: #d7eff8; font-size: 16px; }
.empty-stage span { font-size: 11px; }
.property-panel { padding: 19px 16px; overflow: auto; border-left: 1px solid #203852; background: #10243e; }
.property-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; }
.property-title div { display: flex; flex-direction: column; gap: 4px; }
.property-title span, .property-panel > label, .number-range label { color: #7194b1; font-size: 11px; }
.property-title strong { color: #e8f8ff; font-size: 15px; }
.property-panel > label { display: block; margin: 17px 0 7px; }
.chart-type-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; }
.chart-type-grid button { height: 66px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; border: 1px solid #294c69; border-radius: 6px; color: #789ab6; background: #132e4b; cursor: pointer; }
.chart-type-grid button:hover, .chart-type-grid button.active { border-color: #35d6ed; color: #4edbef; background: #153b5b; }
.chart-type-grid svg { width: 21px; }
.number-range { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; margin-top: 15px; }
.number-range label { display: block; margin-bottom: 6px; }
.number-range :deep(.el-input-number) { width: 100%; }
.color-row { display: flex; gap: 10px; }
.color-row button { width: 25px; height: 25px; border: 2px solid transparent; border-radius: 50%; cursor: pointer; }
.color-row button.active { border-color: #fff; box-shadow: 0 0 0 2px #35d6ed; }
.empty-property { min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; text-align: center; color: #668aa7; }
.empty-property svg { width: 34px; }
.empty-property strong { color: #d6edf6; }
.empty-property span { font-size: 11px; }
.property-panel :deep(.el-input__wrapper), .property-panel :deep(.el-select__wrapper), .property-panel :deep(.el-input-number .el-input__wrapper) { box-shadow: 0 0 0 1px #2b4b67 inset; background: #0b1d31; }
.property-panel :deep(.el-input__inner), .property-panel :deep(.el-select__placeholder), .property-panel :deep(.el-select__selected-item) { color: #dceef7; }
@media (max-width: 1250px) { .designer-workspace { grid-template-columns: 96px minmax(0, 1fr) 270px; } .widget-grid { grid-template-columns: 1fr; } }
</style>
