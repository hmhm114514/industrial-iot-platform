<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">告警闭环</el-tag><h1>历史告警</h1><p>查询规则触发的告警，并完成处置闭环。</p></div>
      <div class="module-actions"><el-input v-model="keyword" placeholder="设备 / 告警标题" clearable class="search-input" /><el-button type="primary" @click="load">查询</el-button></div>
    </section>
    <el-card class="table-card" shadow="never">
      <el-table :data="filteredRows" stripe>
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="title" label="告警标题" min-width="180" />
        <el-table-column prop="deviceName" label="设备" min-width="190" />
        <el-table-column prop="level" label="等级" width="90"><template #default="{ row }"><el-tag :type="row.level === '高' ? 'danger' : 'warning'" round>{{ row.level }}</el-tag></template></el-table-column>
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="value" label="触发值" width="520" />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === '已处理' ? 'success' : 'danger'" round>{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="handler" label="处理人" width="120" />
        <el-table-column prop="time" label="时间" width="190" />
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">查看</el-button><el-button link type="primary" :disabled="row.status === '已处理'" @click="openHandle(row)">处置</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="detailVisible" :title="detailTitle" width="560px">
      <el-table :data="currentMetrics" :row-class-name="metricRowClass" stripe>
        <el-table-column prop="metric" label="指标" width="150" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="label" label="说明" min-width="160" />
      </el-table>
    </el-dialog>
    <el-dialog v-model="visible" title="告警处置" width="520px"><el-form :model="form" label-width="90px"><el-form-item label="告警"><el-input v-model="form.title" disabled /></el-form-item><el-form-item label="处置意见"><el-input v-model="form.remark" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submitHandle">确认处置</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { alarmApi, resourceApi, telemetryMetrics } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'
const rows = ref([])
const telemetryRows = ref([])
const rules = ref([])
const keyword = ref('')
const visible = ref(false)
const detailVisible = ref(false)
const currentId = ref(null)
const currentDetail = ref(null)
const form = reactive({ title: '', remark: '已现场复核并恢复正常。' })
const timeValue = (row) => new Date(row.createdAt || row.time || 0).getTime() || 0
const filteredRows = computed(() => rows.value
  .filter((row) => !keyword.value || JSON.stringify(row).includes(keyword.value))
  .slice()
  .sort((a, b) => timeValue(b) - timeValue(a)))
const detailTitle = computed(() => currentDetail.value ? `${currentDetail.value.deviceName || '设备'} - 指标详情` : '指标详情')
const currentMetrics = computed(() => currentDetail.value?.metrics || [])
const fallbackMetrics = (row) => [{ metric: 'trigger', label: '触发值', value: row.value || '-', unit: '' }]
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
const metricStatus = (metric) => {
  if (metric.metric === 'trigger') return levelText(currentDetail.value?.level)
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
const openDetail = (row) => {
  const alarmTime = timeValue(row)
  const candidates = telemetryRows.value
    .filter((item) => String(item.deviceId || '') === String(row.deviceId || '') || item.deviceName === row.deviceName)
    .sort((a, b) => Math.abs(timeValue(a) - alarmTime) - Math.abs(timeValue(b) - alarmTime))
  const telemetry = candidates[0]
  currentDetail.value = { ...row, metrics: telemetry?.metrics?.length ? telemetry.metrics : fallbackMetrics(row) }
  detailVisible.value = true
}
const load = async () => { try { const [alarms, telemetry, ruleList] = await Promise.all([resourceApi.list('historicalAlarm', { keyword: keyword.value }), resourceApi.list('historicalData'), resourceApi.list('rule')]); rows.value = normalizeList(alarms); telemetryRows.value = normalizeList(telemetry); rules.value = normalizeList(ruleList) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); rows.value = clone(fallbackRows.historicalAlarm); rules.value = clone(fallbackRows.rule || []); telemetryRows.value = clone(fallbackRows.historicalData).map((row) => ({ ...row, metrics: [{ metric: row.metric, label: telemetryMetrics.find((item) => item.value === row.metric)?.label || row.metric, value: row.value, unit: row.unit }] })) } }
const openHandle = (row) => { currentId.value = row.id; form.title = row.title; form.remark = '已现场复核并恢复正常。'; visible.value = true }
const submitHandle = async () => { const user = JSON.parse(localStorage.getItem('iot_user') || '{}'); try { await alarmApi.handle(currentId.value, { remark: form.remark, handler: user.realName || user.username || 'admin' }); ElMessage.success('告警已处置'); await load() } catch (error) { quietError(error, '告警处置失败，后端数据未变更') } visible.value = false }
const handleRealtime = (event) => { if (event.detail?.type === 'telemetry') load() }
onMounted(() => {
  load()
  window.addEventListener('iot-realtime', handleRealtime)
})
onBeforeUnmount(() => window.removeEventListener('iot-realtime', handleRealtime))
</script>
