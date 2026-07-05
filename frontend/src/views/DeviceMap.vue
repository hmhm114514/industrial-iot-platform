<template>
  <div class="device-map-page page-stack">
    <section class="module-head map-head">
      <div>
        <el-tag effect="dark" class="hero-tag">真实地图</el-tag>
        <h1>设备地图</h1>
        <p>基于真实世界底图展示设备位置，仅渲染具备合法经纬度的设备。</p>
      </div>
      <div class="module-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索设备或位置"
          clearable
          class="search-input"
          @keyup.enter="locateFirstMatched"
        />
        <el-button type="primary" @click="locateFirstMatched">定位设备</el-button>
        <el-button :loading="loading" @click="loadDevices">刷新</el-button>
      </div>
    </section>

    <el-alert
      v-if="demoMode"
      title="数据服务暂不可用，当前为演示点位"
      type="warning"
      show-icon
      :closable="false"
      class="map-alert"
    />
    <el-alert
      v-else-if="missingCount > 0"
      :title="`共 ${totalCount} 台设备，${missingCount} 台缺少经纬度未显示`"
      type="info"
      show-icon
      :closable="false"
      class="map-alert"
    />

    <section class="map-layout">
      <div class="map-shell-real">
        <div ref="mapRef" class="leaflet-map" />
        <div class="map-overlay-card">
          <span>{{ demoMode ? '演示模式' : '平台数据' }}</span>
          <strong>{{ markerDevices.length }}</strong>
          <em>{{ demoMode ? '演示点位' : '已显示设备' }}</em>
        </div>
        <div v-if="loading" class="map-loading">正在加载设备坐标...</div>
      </div>

      <aside class="map-side-card">
        <div class="side-head">
          <div>
            <span>设备列表</span>
            <strong>{{ filteredDevices.length }}/{{ totalCount }}</strong>
          </div>
          <el-tag size="small" effect="plain">OSM</el-tag>
        </div>
        <div class="map-legend-real">
          <span><i class="online" />在线</span>
          <span><i class="offline" />离线</span>
          <span><i class="alarm" />告警</span>
        </div>
        <div class="device-list">
          <button
            v-for="device in filteredDevices"
            :key="device.id"
            type="button"
            class="device-item"
            :class="device.tone"
            @click="focusDevice(device)"
          >
            <span>{{ device.location || '位置未填写' }}</span>
            <strong>{{ device.name }}</strong>
            <em>{{ device.statusLabel }} · {{ device.lastOnlineText }}</em>
          </button>
          <div v-if="!filteredDevices.length && !loading" class="empty-device">
            暂无可显示的设备坐标，底图仍可拖拽缩放查看真实地图。
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { resourceApi } from '../api/platform'
import { quietError } from '../api/http'

const keyword = ref('')
const mapRef = ref(null)
const loading = ref(false)
const demoMode = ref(false)
const totalCount = ref(0)
const missingCount = ref(0)
const markerDevices = ref([])

let mapInstance = null
let markerLayer = null
const markerMap = new Map()

const fallbackDevices = [
  { id: 'demo-shanghai', name: '演示点位-上海临港网关', status: 'ONLINE', location: '上海临港', latitude: 30.9006, longitude: 121.9289, lastOnlineAt: '2026-07-04 09:30:00' },
  { id: 'demo-nanjing', name: '演示点位-南京江宁传感器', status: 'OFFLINE', location: '南京江宁', latitude: 31.9539, longitude: 118.8402, lastOnlineAt: '2026-07-03 20:18:20' },
  { id: 'demo-suzhou', name: '演示点位-苏州仓储监测', status: 'ALARM', location: '苏州昆山', latitude: 31.3846, longitude: 120.9807, lastOnlineAt: '2026-07-04 08:42:11' }
]

const filteredDevices = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return markerDevices.value
  return markerDevices.value.filter((device) => [device.name, device.location, device.statusLabel]
    .filter(Boolean)
    .some((text) => String(text).toLowerCase().includes(word)))
})

const toNumber = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

const isValidCoordinate = (latitude, longitude) => (
  latitude !== null && longitude !== null &&
  latitude >= -90 && latitude <= 90 &&
  longitude >= -180 && longitude <= 180
)

const formatTime = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '暂无记录'

const statusLabelOf = (device) => {
  const raw = String(device.status || '').toUpperCase()
  if (device.online === true || raw === 'ONLINE' || raw === 'ENABLED') return '在线'
  if (raw === 'ALARM' || raw === 'WARNING') return '告警'
  if (raw === 'DISABLED' || raw === 'STOPPED') return '停用'
  if (device.online === false || raw === 'OFFLINE') return '离线'
  return device.status || '未知'
}

const toneOf = (label) => {
  if (label === '在线') return 'online'
  if (label === '告警') return 'alarm'
  if (label === '停用') return 'disabled'
  return 'offline'
}

const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, (char) => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  "'": '&#39;',
  '"': '&quot;'
}[char]))

const normalizeDevice = (device) => {
  const latitude = toNumber(device.latitude)
  const longitude = toNumber(device.longitude)
  if (!isValidCoordinate(latitude, longitude)) return null

  const statusLabel = statusLabelOf(device)
  return {
    ...device,
    id: device.id ?? `${device.name || 'device'}-${latitude}-${longitude}`,
    name: device.name || device.code || '未命名设备',
    latitude,
    longitude,
    statusLabel,
    tone: toneOf(statusLabel),
    lastOnlineText: formatTime(device.lastOnlineAt || device.lastSeen)
  }
}

const popupHtml = (device) => `
  <div class="device-map-popup">
    <strong>${escapeHtml(device.name)}</strong>
    <span class="popup-status ${escapeHtml(device.tone)}">${escapeHtml(device.statusLabel)}</span>
    <p>位置：${escapeHtml(device.location || '未填写')}</p>
    <p>最后在线：${escapeHtml(device.lastOnlineText)}</p>
  </div>
`

const markerIcon = (device) => L.divIcon({
  className: `iot-map-marker ${device.tone}`,
  html: '<span>●</span>',
  iconSize: [34, 34],
  iconAnchor: [17, 17],
  popupAnchor: [0, -18]
})

const initMap = () => {
  if (mapInstance || !mapRef.value) return

  mapInstance = L.map(mapRef.value, { zoomControl: false }).setView([31.2304, 121.4737], 6)
  L.control.zoom({ position: 'bottomright' }).addTo(mapInstance)
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(mapInstance)
  markerLayer = L.layerGroup().addTo(mapInstance)
}

const fitMapToDevices = () => {
  if (!mapInstance) return
  if (!markerDevices.value.length) {
    mapInstance.setView([31.2304, 121.4737], 6)
    return
  }
  const bounds = L.latLngBounds(markerDevices.value.map((device) => [device.latitude, device.longitude]))
  mapInstance.fitBounds(bounds, { padding: [48, 48], maxZoom: 13 })
}

const renderMarkers = () => {
  if (!markerLayer) return
  markerLayer.clearLayers()
  markerMap.clear()

  markerDevices.value.forEach((device) => {
    const marker = L.marker([device.latitude, device.longitude], { icon: markerIcon(device) }).bindPopup(popupHtml(device))
    marker.addTo(markerLayer)
    markerMap.set(device.id, marker)
  })
  fitMapToDevices()
  nextTick(() => mapInstance?.invalidateSize())
}

const applyDevices = (devices, isFallback = false) => {
  const rows = Array.isArray(devices) ? devices : []
  const normalized = rows.map(normalizeDevice).filter(Boolean)

  demoMode.value = isFallback
  totalCount.value = rows.length
  missingCount.value = isFallback ? 0 : rows.length - normalized.length
  markerDevices.value = normalized
  renderMarkers()
}

const loadDevices = async () => {
  loading.value = true
  try {
    const devices = await resourceApi.list('device')
    applyDevices(devices, false)
  } catch (error) {
    quietError(error, '设备数据服务暂不可用，当前为演示点位')
    applyDevices(fallbackDevices, true)
  } finally {
    loading.value = false
  }
}

const focusDevice = (device) => {
  const marker = markerMap.get(device.id)
  if (!mapInstance || !marker) return
  mapInstance.flyTo([device.latitude, device.longitude], Math.max(mapInstance.getZoom(), 14), { duration: 0.8 })
  marker.openPopup()
}

const locateFirstMatched = () => {
  if (!keyword.value.trim()) {
    fitMapToDevices()
    return
  }

  const target = filteredDevices.value[0]
  if (!target) {
    ElMessage.info('未找到带有效坐标的匹配设备')
    return
  }
  focusDevice(target)
}

const invalidateMapSize = () => mapInstance?.invalidateSize()

onMounted(async () => {
  await nextTick()
  initMap()
  await loadDevices()
  invalidateMapSize()
  setTimeout(invalidateMapSize, 160)
  window.addEventListener('resize', invalidateMapSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', invalidateMapSize)
  markerMap.clear()
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
    markerLayer = null
  }
})
</script>

<style scoped>
.device-map-page {
  --geo-deep: #10251f;
  --geo-green: #1f6b54;
  --geo-mint: #dff0df;
  --geo-gold: #d99a32;
}

.map-head {
  background:
    radial-gradient(circle at 82% 14%, rgba(217, 154, 50, .24), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, .96), rgba(223, 240, 223, .9));
  border-color: rgba(31, 107, 84, .16);
}

.map-head h1 {
  color: var(--geo-deep);
}

.map-head .hero-tag {
  background: linear-gradient(90deg, var(--geo-green), var(--geo-gold));
}

.map-head :deep(.el-button--primary) {
  --el-button-bg-color: var(--geo-green);
  --el-button-border-color: var(--geo-green);
  --el-button-hover-bg-color: #258263;
  --el-button-hover-border-color: #258263;
}

.map-alert {
  border-radius: 16px;
}

:global(.iot-map-marker) {
  display: grid;
  place-items: center;
  width: 34px !important;
  height: 34px !important;
  margin-left: -17px !important;
  margin-top: -17px !important;
  border-radius: 50% 50% 50% 8px;
  transform: rotate(-45deg);
  background: #94a3b8;
  border: 3px solid #ffffff;
  box-shadow: 0 12px 26px rgba(16, 37, 31, .28);
}

:global(.iot-map-marker span) {
  display: grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: transparent;
  background: #ffffff;
  transform: rotate(45deg);
}

:global(.iot-map-marker.online) { background: #10b981; }
:global(.iot-map-marker.offline) { background: #64748b; }
:global(.iot-map-marker.alarm) { background: #fb7185; animation: mapMarkerPulse 1.6s infinite; }
:global(.iot-map-marker.disabled) { background: #f59e0b; }

@keyframes mapMarkerPulse {
  50% { box-shadow: 0 0 0 12px rgba(251, 113, 133, .16), 0 12px 26px rgba(16, 37, 31, .28); }
}

.map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: stretch;
}

.map-shell-real {
  position: relative;
  height: 650px;
  min-height: calc(100vh - 270px);
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(31, 107, 84, .18);
  background: var(--geo-mint);
  box-shadow: 0 28px 70px rgba(16, 37, 31, .18);
}

.leaflet-map {
  width: 100%;
  height: 100%;
  min-height: 520px;
  z-index: 1;
}

.map-shell-real::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.36), inset 0 -90px 120px rgba(16,37,31,.12);
}

.map-overlay-card,
.map-loading {
  position: absolute;
  z-index: 3;
  border: 1px solid rgba(255,255,255,.58);
  background: rgba(255, 252, 241, .84);
  backdrop-filter: blur(14px);
  color: var(--geo-deep);
  box-shadow: 0 16px 36px rgba(16,37,31,.16);
}

.map-overlay-card {
  left: 22px;
  top: 22px;
  min-width: 150px;
  padding: 16px 18px;
  border-radius: 22px;
}

.map-overlay-card span,
.map-overlay-card em {
  display: block;
  color: rgba(16,37,31,.62);
  font-size: 12px;
  font-style: normal;
}

.map-overlay-card strong {
  display: block;
  margin: 5px 0;
  color: var(--geo-green);
  font-size: 38px;
  line-height: 1;
}

.map-loading {
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  padding: 12px 18px;
  border-radius: 999px;
  font-weight: 800;
}

.map-side-card {
  min-height: 650px;
  padding: 18px;
  border-radius: 28px;
  color: #f7f1df;
  background:
    radial-gradient(circle at 92% 8%, rgba(217,154,50,.24), transparent 30%),
    linear-gradient(180deg, rgba(16,37,31,.98), rgba(22,50,42,.95));
  border: 1px solid rgba(255,255,255,.12);
  box-shadow: 0 28px 70px rgba(16,37,31,.2);
}

.side-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.side-head span,
.side-head strong {
  display: block;
}

.side-head span {
  color: rgba(247,241,223,.68);
}

.side-head strong {
  margin-top: 4px;
  color: #f2bf72;
  font-size: 30px;
}

.map-legend-real {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  color: rgba(247,241,223,.74);
  font-size: 12px;
}

.map-legend-real i {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 6px;
  border-radius: 50%;
}

.map-legend-real .online { background: #34d399; }
.map-legend-real .offline { background: #94a3b8; }
.map-legend-real .alarm { background: #fb7185; }

.device-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 545px;
  overflow: auto;
  padding-right: 4px;
}

.device-item,
.empty-device {
  width: 100%;
  border: 1px solid rgba(255,255,255,.12);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  color: #f8f3e7;
  background: rgba(255,255,255,.07);
}

.device-item {
  cursor: pointer;
  transition: transform .18s ease, background .18s ease, border-color .18s ease;
}

.device-item:hover {
  transform: translateX(-4px);
  background: rgba(255,255,255,.12);
  border-color: rgba(242,191,114,.48);
}

.device-item span,
.device-item strong,
.device-item em {
  display: block;
}

.device-item span {
  margin-bottom: 7px;
  color: rgba(247,241,223,.58);
  font-size: 12px;
}

.device-item strong {
  font-size: 16px;
}

.device-item em {
  margin-top: 8px;
  color: rgba(247,241,223,.68);
  font-size: 12px;
  font-style: normal;
}

.device-item.online { box-shadow: inset 4px 0 0 #34d399; }
.device-item.offline { box-shadow: inset 4px 0 0 #94a3b8; }
.device-item.alarm { box-shadow: inset 4px 0 0 #fb7185; }
.device-item.disabled { box-shadow: inset 4px 0 0 #f59e0b; }

.empty-device {
  color: rgba(247,241,223,.68);
  line-height: 1.7;
}

:deep(.device-map-popup) {
  min-width: 190px;
  color: #13251f;
}

:deep(.device-map-popup strong) {
  display: block;
  margin-bottom: 8px;
  font-size: 15px;
}

:deep(.device-map-popup p) {
  margin: 6px 0 0;
  color: #53635d;
}

:deep(.popup-status) {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
}

:deep(.popup-status.online) { background: #10b981; }
:deep(.popup-status.offline) { background: #64748b; }
:deep(.popup-status.alarm) { background: #f43f5e; }
:deep(.popup-status.disabled) { background: #f59e0b; }

:deep(.leaflet-container) {
  font-family: "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei UI", sans-serif;
}

@media (max-width: 1180px) {
  .map-layout {
    grid-template-columns: 1fr;
  }

  .map-side-card {
    min-height: auto;
  }

  .device-list {
    max-height: 360px;
  }
}

@media (max-width: 760px) {
  .map-shell-real {
    height: 560px;
    min-height: 560px;
  }

  .leaflet-map {
    min-height: 560px;
  }
}
</style>
