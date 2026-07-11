<template>
  <div class="page-stack">
    <section class="module-head">
      <div>
        <el-tag effect="dark" class="hero-tag">数据中心</el-tag>
        <h1>历史数据</h1>
      </div>
      <div class="module-actions">
        <el-select v-model="query.productGroup" clearable placeholder="产品分组" style="width: 160px">
          <el-option v-for="item in productCategories" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
        <el-select v-model="query.deviceGroup" clearable placeholder="设备分组" style="width: 160px">
          <el-option v-for="item in deviceGroups" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
        <el-input v-model="query.deviceName" placeholder="设备名称" clearable class="search-input" />
        <el-select v-model="query.metric" clearable placeholder="指标" style="width: 180px">
          <el-option v-for="item in telemetryMetrics" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </section>

    <el-card class="table-card" shadow="never">
      <div class="history-content">
        <aside class="alarm-donut-panel">
          <div class="alarm-donut large">
            <svg viewBox="0 0 180 180" class="alarm-donut-svg" @mouseleave="hoverSegment = null">
              <path
                v-for="segment in alarmSegments"
                :key="segment.status"
                :d="segment.path"
                :stroke="segment.color"
                :class="{ active: hoverSegment === segment.status }"
                class="alarm-donut-segment"
                fill="none"
                stroke-linecap="butt"
                @mouseenter="hoverSegment = segment.status"
              />
            </svg>
            <div class="alarm-donut-center">
              <strong>{{ filteredRows.length }}</strong>
              <span>总条数</span>
            </div>
            <div v-if="hoveredAlarm" class="alarm-donut-tooltip">
              <strong>{{ hoveredAlarm.status }}</strong>
              <span>{{ hoveredAlarm.count }} 条 / {{ hoveredAlarm.percent }}%</span>
            </div>
          </div>
          <div class="alarm-donut-legend">
            <span><i class="normal" />正常</span>
            <span><i class="low" />低</span>
            <span><i class="middle" />中</span>
            <span><i class="high" />高</span>
          </div>
        </aside>
        <section class="history-main">
          <div class="history-summary">
            <span>数据摘要</span>
            <el-tag type="info" round>共 {{ filteredRows.length }} 条</el-tag>
            <el-tag v-for="item in alarmSummary" :key="item.status" :type="alarmTag(item.status)" round>{{ item.status }} {{ item.count }}项</el-tag>
          </div>
          <el-table :data="pagedRows" stripe>
            <el-table-column label="序号" width="80">
              <template #default="{ $index }">{{ (page.current - 1) * page.size + $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="productGroup" label="产品分组" width="140" />
            <el-table-column prop="deviceGroup" label="设备分组" width="140" />
            <el-table-column prop="deviceCode" label="设备编码" width="145" />
            <el-table-column label="告警状态" width="115" align="center">
              <template #default="{ row }"><el-tag :type="alarmTag(row.alarmStatus)" round>{{ row.alarmStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="time" label="上报时间" width="180" />
            <el-table-column prop="deviceName" label="设备名称" min-width="190" />
            <el-table-column prop="ruleName" label="规则名称" min-width="180" />
            <el-table-column label="详情" width="100" align="center" fixed="right">
              <template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">查看</el-button></template>
            </el-table-column>
          </el-table>
          <div class="table-pagination">
            <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="filteredRows.length" />
          </div>
        </section>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="560px">
      <el-table :data="currentDetail?.metrics || []" :row-class-name="metricRowClass" stripe>
        <el-table-column prop="metric" label="指标" width="150" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="label" label="说明" min-width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { resourceApi, telemetryMetrics } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'

const rows = ref([])
const devices = ref([])
const products = ref([])
const productCategories = ref([])
const deviceGroups = ref([])
const rules = ref([])
const query = reactive({ productGroup: '', deviceGroup: '', deviceName: '', metric: '' })
const page = reactive({ current: 1, size: 20 })
const detailVisible = ref(false)
const currentDetail = ref(null)
const hoverSegment = ref(null)

const filteredRows = computed(() => {
  const deviceName = query.deviceName.trim().toLowerCase()
  return rows.value.filter((row) => {
    const matchDevice = !deviceName || String(row.deviceName || '').toLowerCase().includes(deviceName)
    const matchProductGroup = !query.productGroup || row.productGroup === query.productGroup
    const matchDeviceGroup = !query.deviceGroup || row.deviceGroup === query.deviceGroup
    const matchMetric = !query.metric || row.metrics.some((metric) => metric.metric === query.metric)
    return matchProductGroup && matchDeviceGroup && matchDevice && matchMetric
  })
})
const pagedRows = computed(() => filteredRows.value.slice((page.current - 1) * page.size, page.current * page.size))
const detailTitle = computed(() => currentDetail.value ? `${currentDetail.value.deviceName} - ${currentDetail.value.time}` : '指标详情')
const alarmSummary = computed(() => ['正常', '低', '中', '高'].map((status) => ({ status, count: filteredRows.value.filter((row) => row.alarmStatus === status).length })))
const alarmCount = (status) => alarmSummary.value.find((item) => item.status === status)?.count || 0
const alarmColors = { 正常: '#67c23a', 低: '#f3c96b', 中: '#e6a23c', 高: '#f56c6c' }
const polar = (angle, radius = 70) => {
  const radians = (angle - 90) * Math.PI / 180
  return { x: 90 + radius * Math.cos(radians), y: 90 + radius * Math.sin(radians) }
}
const arcPath = (start, end) => {
  const from = polar(start)
  const to = polar(end)
  const large = end - start > 180 ? 1 : 0
  return `M ${from.x} ${from.y} A 70 70 0 ${large} 1 ${to.x} ${to.y}`
}
const alarmSegments = computed(() => {
  const total = filteredRows.value.length
  if (!total) return [{ status: '无数据', count: 0, percent: 100, color: 'rgba(148,163,184,.32)', path: arcPath(0, 359.99) }]
  let cursor = 0
  return alarmSummary.value.filter((item) => item.count > 0).map((item) => {
    const degrees = item.count / total * 360
    const segment = {
      ...item,
      percent: Math.round(item.count / total * 1000) / 10,
      color: alarmColors[item.status],
      path: arcPath(cursor, Math.min(cursor + degrees, 359.99))
    }
    cursor += degrees
    return segment
  })
})
const hoveredAlarm = computed(() => alarmSegments.value.find((item) => item.status === hoverSegment.value))

const alarmTag = (status) => ({ 高: 'danger', 中: 'warning', 低: 'warning', 正常: 'success' }[status] || 'info')
const levelText = (level) => ({ HIGH: '高', MEDIUM: '中', LOW: '低', INFO: '低', high: '高', medium: '中', low: '低' }[level] || level || '低')
const levelRank = (level) => ({ 高: 3, 中: 2, 低: 1, 正常: 0 }[level] || 0)
const compare = (value, operator, threshold) => {
  const left = Number(value)
  const right = Number(threshold)
  if (Number.isNaN(left) || Number.isNaN(right)) return false
  if (operator === '>=') return left >= right
  if (operator === '<') return left < right
  if (operator === '<=') return left <= right
  if (operator === '==') return left === right
  return left > right
}
const alarmStatus = (row) => {
  const hits = rules.value
    .filter((rule) => rule.enabled !== false && rule.status !== 'DISABLED')
    .filter((rule) => compare(row.metricValues?.[rule.metric], rule.operator, rule.threshold))
    .map((rule) => levelText(rule.alarmLevel || rule.level))
  return hits.sort((a, b) => levelRank(b) - levelRank(a))[0] || '正常'
}
const metricStatus = (metric) => {
  const hits = rules.value
    .filter((rule) => rule.enabled !== false && rule.status !== 'DISABLED' && rule.metric === metric.metric)
    .filter((rule) => compare(metric.value, rule.operator, rule.threshold))
    .map((rule) => levelText(rule.alarmLevel || rule.level))
  return hits.sort((a, b) => levelRank(b) - levelRank(a))[0] || '正常'
}
const metricRowClass = ({ row }) => ({
  正常: 'metric-row-normal',
  低: 'metric-row-low',
  中: 'metric-row-middle',
  高: 'metric-row-high'
}[metricStatus(row)] || 'metric-row-normal')
const enrichRows = (items) => items.map((row) => {
  const device = devices.value.find((item) => String(item.id) === String(row.deviceId) || item.name === row.deviceName)
  const product = products.value.find((item) => String(item.id) === String(device?.productId))
  const category = productCategories.value.find((item) => String(item.id) === String(product?.categoryId))
  const group = deviceGroups.value.find((item) => String(item.id) === String(device?.groupId))
  const ruleIds = String(device?.ruleIds || row.ruleIds || product?.ruleIds || device?.ruleId || row.ruleId || product?.ruleId || '').split(',').map((item) => item.trim()).filter(Boolean)
  const ruleName = ruleIds.map((id) => rules.value.find((item) => String(item.id) === String(id))?.name).filter(Boolean).join('，')
  return { ...row, productGroup: category?.name || product?.category || '-', deviceGroup: group?.name || device?.group || '-', deviceCode: device?.code || row.deviceCode || '-', ruleName: ruleName || product?.ruleChain || '-', alarmStatus: alarmStatus(row) }
})

const search = async () => { page.current = 1; await load() }
const load = async () => {
  try {
    const [telemetry, deviceList, productList, categoryList, groupList, ruleList] = await Promise.all([
      resourceApi.list('historicalData', query),
      resourceApi.list('device'),
      resourceApi.list('product'),
      resourceApi.list('productCategory'),
      resourceApi.list('deviceGroup'),
      resourceApi.list('rule')
    ])
    devices.value = normalizeList(deviceList)
    products.value = normalizeList(productList)
    productCategories.value = normalizeList(categoryList)
    deviceGroups.value = normalizeList(groupList)
    rules.value = normalizeList(ruleList)
    rows.value = enrichRows(normalizeList(telemetry))
  } catch (error) {
    quietError(error, '数据服务暂不可用，当前为演示数据')
    rows.value = clone(fallbackRows.historicalData).map((row) => ({ ...row, metrics: [{ metric: row.metric, label: row.metric, value: row.value, unit: row.unit }], metricValues: { [row.metric]: row.value }, productGroup: '-', deviceGroup: '-', deviceCode: '-', ruleName: '-', alarmStatus: '正常' }))
  }
}
const openDetail = (row) => { currentDetail.value = row; detailVisible.value = true }
const handleRealtime = (event) => { if (event.detail?.type === 'telemetry') load() }

watch(() => [query.productGroup, query.deviceGroup, query.deviceName, query.metric, page.size], () => { page.current = 1 })
onMounted(() => {
  load()
  window.addEventListener('iot-realtime', handleRealtime)
})
onBeforeUnmount(() => window.removeEventListener('iot-realtime', handleRealtime))
</script>
