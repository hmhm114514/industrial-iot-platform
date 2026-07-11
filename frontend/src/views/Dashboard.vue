<template>
  <div class="dashboard page-stack">
    <section class="hero-panel">
      <div>
        <el-tag effect="dark" class="hero-tag">实时态势</el-tag>
        <h1>工业物联网智能平台控制台</h1>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="load">刷新数据</el-button>
        <el-button size="large" @click="$router.push('/device/devices')">设备接入</el-button>
      </div>
    </section>

    <el-row :gutter="18" class="stat-row">
      <el-col v-for="card in statCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card">
          <div :class="['stat-icon', card.tone]"><el-icon><component :is="card.icon" /></el-icon></div>
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <em>{{ card.tip }}</em>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="15">
        <div class="chart-card tall"><div ref="lineRef" class="chart" /></div>
      </el-col>
      <el-col :xs="24" :lg="9">
        <div class="chart-card tall"><div ref="pieRef" class="chart" /></div>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="12">
        <div class="chart-card"><div ref="barRef" class="chart" /></div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="chart-card"><div ref="alarmRef" class="chart" /></div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Cpu, DataLine, Bell, Box, Connection, Timer } from '@element-plus/icons-vue'
import { dashboardApi } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackDashboard } from '../utils/fallback'

const emptyDashboard = { stats: {}, messageTrend: [], deviceGrowth: [], statusRatio: [], alarmTrend: [] }
const data = ref(emptyDashboard)
const lineRef = ref(null)
const pieRef = ref(null)
const barRef = ref(null)
const alarmRef = ref(null)
const charts = []

const numberText = (value) => Number(value || 0).toLocaleString()
const emptyStats = { devices: 0, onlineDevices: 0, products: 0, alarms: 0, messages: 0, tasks: 0 }
const stats = computed(() => data.value.stats || emptyStats)
const statCards = computed(() => [
  { label: '设备总数', value: numberText(stats.value.devices), tip: `${stats.value.onlineDevices || 0} 台在线`, icon: Cpu, tone: 'blue' },
  { label: '在线设备', value: numberText(stats.value.onlineDevices), tip: '实时通信中', icon: Connection, tone: 'green' },
  { label: '产品数量', value: numberText(stats.value.products), tip: '产品模型', icon: Box, tone: 'purple' },
  { label: '当前告警', value: numberText(stats.value.alarms), tip: '需关注', icon: Bell, tone: 'red' },
  { label: '今日消息', value: numberText(stats.value.messages), tip: '遥测上报', icon: DataLine, tone: 'cyan' },
  { label: '任务数量', value: numberText(stats.value.tasks), tip: '调度运行', icon: Timer, tone: 'orange' }
])

const list = (key) => data.value[key] ?? []
const names = (key) => list(key).map((item) => item.name || item.date)
const values = (key) => list(key).map((item) => item.value ?? item.count ?? 0)

const makeChart = (refEl, option) => {
  if (!refEl.value) return
  const chart = echarts.init(refEl.value)
  chart.setOption(option)
  charts.push(chart)
}

const renderCharts = async () => {
  charts.splice(0).forEach((chart) => chart.dispose())
  await nextTick()
  makeChart(lineRef, {
    title: { text: '消息上报趋势', textStyle: { color: '#21304d', fontWeight: 800 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 24, top: 64, bottom: 34 },
    xAxis: { type: 'category', data: names('messageTrend'), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: values('messageTrend'), smooth: true, symbolSize: 8, areaStyle: { color: 'rgba(26,115,232,.12)' }, lineStyle: { width: 4, color: '#1a73e8' } }]
  })
  makeChart(pieRef, {
    title: { text: '设备状态占比', textStyle: { color: '#21304d', fontWeight: 800 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 4 },
    series: [{ type: 'pie', radius: ['48%', '72%'], center: ['50%', '48%'], data: data.value.statusRatio || [], label: { formatter: '{b}\n{d}%' } }]
  })
  makeChart(barRef, {
    title: { text: '近 7 日新增设备', textStyle: { color: '#21304d', fontWeight: 800 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 18, top: 60, bottom: 32 },
    xAxis: { type: 'category', data: names('deviceGrowth') },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values('deviceGrowth'), barWidth: 22, itemStyle: { borderRadius: [8, 8, 0, 0], color: '#00b8d9' } }]
  })
  makeChart(alarmRef, {
    title: { text: '告警趋势', textStyle: { color: '#21304d', fontWeight: 800 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 18, top: 60, bottom: 32 },
    xAxis: { type: 'category', data: names('alarmTrend') },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: values('alarmTrend'), smooth: true, symbol: 'circle', lineStyle: { width: 4, color: '#ff5a5f' }, itemStyle: { color: '#ff5a5f' } }]
  })
}

const load = async () => {
  try {
    const payload = await dashboardApi.get()
    data.value = {
      stats: { ...emptyStats, ...(payload?.stats || payload?.summary || {}) },
      messageTrend: payload?.messageTrend ?? [],
      deviceGrowth: payload?.deviceGrowth ?? [],
      statusRatio: payload?.statusRatio ?? [],
      alarmTrend: payload?.alarmTrend ?? []
    }
  } catch (error) {
    quietError(error, '数据服务暂不可用，当前为演示数据')
    data.value = fallbackDashboard
  }
  renderCharts()
}

const resize = () => charts.forEach((chart) => chart.resize())
const handleRealtime = (event) => { if (event.detail?.type === 'telemetry') load() }
onMounted(() => {
  load()
  window.addEventListener('resize', resize)
  window.addEventListener('iot-realtime', handleRealtime)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('iot-realtime', handleRealtime)
  charts.forEach((chart) => chart.dispose())
})
</script>
