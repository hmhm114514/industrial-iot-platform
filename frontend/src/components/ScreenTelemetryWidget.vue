<template>
  <article :class="['telemetry-widget', { selected }]" @click="$emit('select')">
    <header>
      <div>
        <span>{{ deviceName }}</span>
        <h3>{{ widget.title || metricLabel }}</h3>
      </div>
      <div class="widget-live"><i />实时</div>
    </header>
    <div v-if="!points.length" class="widget-empty">
      <DataLine />
      <span>等待设备上报数据</span>
    </div>
    <div v-else-if="widget.chartType === 'number'" class="number-view">
      <strong>{{ latestValue }}</strong>
      <span>{{ metricUnit }}</span>
      <small>更新于 {{ latestTime }}</small>
    </div>
    <div v-else ref="chartRef" class="widget-chart" />
    <footer>
      <span>{{ metricLabel }}</span>
      <b>{{ latestValue }} {{ metricUnit }}</b>
    </footer>
  </article>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { DataLine } from '@element-plus/icons-vue'

const props = defineProps({
  widget: { type: Object, required: true },
  rows: { type: Array, default: () => [] },
  deviceName: { type: String, default: '未选择设备' },
  metricLabel: { type: String, default: '设备指标' },
  metricUnit: { type: String, default: '' },
  selected: Boolean
})

defineEmits(['select'])

const chartRef = ref(null)
let chart
let observer

const points = computed(() => props.rows
  .filter((row) => String(row.deviceId) === String(props.widget.deviceId))
  .map((row) => ({
    time: row.time || row.reportTime || '',
    value: Number(row.metricValues?.[props.widget.metric] ?? row[props.widget.metric])
  }))
  .filter((point) => Number.isFinite(point.value))
  .slice(0, Number(props.widget.limit || 20))
  .reverse())

const latest = computed(() => points.value.at(-1))
const latestValue = computed(() => latest.value?.value ?? '--')
const latestTime = computed(() => String(latest.value?.time || '--').slice(11, 19))

const chartOption = computed(() => {
  const values = points.value.map((item) => item.value)
  const labels = points.value.map((item) => String(item.time).slice(11, 19))
  const color = props.widget.color || '#35d6ed'
  if (props.widget.chartType === 'gauge') {
    const max = Math.max(Number(props.widget.max || 100), latest.value?.value || 0)
    return {
      series: [{
        type: 'gauge', min: Number(props.widget.min || 0), max,
        progress: { show: true, width: 12 }, axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(111,151,190,.22)']] } },
        axisTick: { show: false }, splitLine: { length: 8, lineStyle: { color: '#6185a8' } },
        axisLabel: { color: '#7899b8', distance: 18, fontSize: 10 }, pointer: { itemStyle: { color } },
        detail: { valueAnimation: true, color: '#f4fbff', fontSize: 30, offsetCenter: [0, '68%'], formatter: `{value} ${props.metricUnit}` },
        data: [{ value: latest.value?.value || 0 }]
      }]
    }
  }
  const isBar = props.widget.chartType === 'bar'
  const isArea = props.widget.chartType === 'area'
  return {
    animationDuration: 350,
    grid: { left: 44, right: 16, top: 20, bottom: 32 },
    tooltip: { trigger: 'axis', backgroundColor: '#102844', borderColor: '#2b6384', textStyle: { color: '#eaf8ff' } },
    xAxis: { type: 'category', data: labels, boundaryGap: isBar, axisLine: { lineStyle: { color: '#315370' } }, axisLabel: { color: '#7395b5', hideOverlap: true } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: 'rgba(92,139,177,.16)' } }, axisLabel: { color: '#7395b5' } },
    series: [{
      data: values, type: isBar ? 'bar' : 'line', smooth: !isBar, symbol: 'circle', symbolSize: 6,
      barMaxWidth: 26, lineStyle: { width: 3, color }, itemStyle: { color },
      areaStyle: isArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: `${color}88` }, { offset: 1, color: `${color}08` }]) } : undefined
    }]
  }
})

const render = async () => {
  if (props.widget.chartType === 'number' || !points.value.length) {
    chart?.dispose()
    chart = null
    return
  }
  await nextTick()
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(chartOption.value, true)
}

watch([points, () => props.widget.chartType, () => props.widget.color, () => props.widget.min, () => props.widget.max], render, { deep: true })

onMounted(() => {
  observer = new ResizeObserver(() => chart?.resize())
  if (chartRef.value) observer.observe(chartRef.value)
  render()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.telemetry-widget { min-width: 0; height: 330px; display: grid; grid-template-rows: auto 1fr auto; padding: 18px; border: 1px solid rgba(61, 139, 184, .28); border-radius: 8px; background: rgba(9, 29, 52, .92); box-shadow: inset 0 0 36px rgba(36, 170, 220, .04); cursor: pointer; transition: border-color .18s, box-shadow .18s; }
.telemetry-widget:hover, .telemetry-widget.selected { border-color: #35d6ed; box-shadow: inset 0 0 36px rgba(36, 170, 220, .07), 0 0 0 2px rgba(53, 214, 237, .12); }
header, footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
header span, footer span { color: #6f9aba; font-size: 11px; }
h3 { margin: 4px 0 0; color: #eaf8ff; font-size: 16px; letter-spacing: 0; }
.widget-live { display: inline-flex; align-items: center; gap: 6px; color: #68e4ad; font-size: 11px; }
.widget-live i { width: 7px; height: 7px; border-radius: 50%; background: #34d399; box-shadow: 0 0 9px #34d399; }
.widget-chart { width: 100%; min-height: 0; }
.widget-empty, .number-view { display: flex; flex-direction: column; align-items: center; justify-content: center; color: #658aa9; }
.widget-empty svg { width: 38px; margin-bottom: 10px; }
.number-view strong { color: #f4fbff; font-size: 64px; line-height: 1; font-family: "DIN Alternate", Arial, sans-serif; }
.number-view span { margin-top: 8px; color: #35d6ed; }
.number-view small { margin-top: 14px; color: #6384a1; }
footer { padding-top: 10px; border-top: 1px solid rgba(91, 139, 177, .14); }
footer b { color: #d9f7ff; font-size: 13px; }
</style>
