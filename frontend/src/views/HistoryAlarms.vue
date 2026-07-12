<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">告警闭环</el-tag><h1>历史告警</h1></div>
      <div class="module-actions"><el-select v-model="filters.productGroup" clearable placeholder="产品分组" style="width: 150px"><el-option v-for="item in productCategories" :key="item.id" :label="item.name" :value="item.name" /></el-select><el-select v-model="filters.deviceGroup" clearable placeholder="设备分组" style="width: 150px"><el-option v-for="item in deviceGroups" :key="item.id" :label="item.name" :value="item.name" /></el-select><el-select v-model="filters.status" clearable placeholder="告警状态" style="width: 140px"><el-option v-for="item in alarmStatusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select><el-input v-model="filters.keyword" placeholder="设备 / 告警标题 / 规则" clearable class="search-input" /><el-button type="primary" @click="search">查询</el-button></div>
    </section>
    <el-card class="table-card" shadow="never">
      <el-table :data="rows" stripe @sort-change="handleSortChange">
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">{{ (page.current - 1) * page.size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="title" column-key="name" label="告警标题" min-width="180" sortable="custom" />
        <el-table-column prop="deviceName" label="设备" min-width="190" sortable="custom" />
        <el-table-column prop="level" label="等级" width="90" sortable="custom"><template #default="{ row }"><el-tag :type="row.level === '高' ? 'danger' : 'warning'" round>{{ row.level }}</el-tag></template></el-table-column>
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="value" label="触发值" width="550" />
        <el-table-column prop="status" label="状态" width="80" sortable="custom"><template #default="{ row }"><el-tag :type="statusTag(row.status)" round>{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="durationText" label="持续时长" width="120" />
        <el-table-column prop="finishTime" label="恢复/关闭时间" width="190" />
        <el-table-column prop="time" column-key="createdAt" label="时间" width="190" sortable="custom" />
        <el-table-column label="操作" width="260" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">查看</el-button><el-button link type="primary" :disabled="row.status !== '未确认'" @click="setStatus(row, 'ACKNOWLEDGED')">确认</el-button><el-button link type="warning" :disabled="row.status === '已关闭'" @click="setStatus(row, 'PROCESSING')">处理</el-button><el-button link type="success" :disabled="row.status === '已关闭'" @click="setStatus(row, 'RECOVERED')">恢复</el-button><el-button link type="danger" :disabled="row.status === '已关闭'" @click="openHandle(row)">关闭</el-button></template></el-table-column>
      </el-table>
      <div class="table-pagination">
        <span class="pagination-total">共 {{ pageCount }} 页</span>
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" :total="total" />
      </div>
    </el-card>
    <el-dialog v-model="detailVisible" :title="detailTitle" width="560px">
      <el-table :data="currentMetrics" :row-class-name="metricRowClass" stripe>
        <el-table-column prop="metric" label="指标" width="150" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="label" label="说明" min-width="160" />
      </el-table>
      <el-divider v-if="currentDetail?.handlingRecord" />
      <el-descriptions v-if="currentDetail?.handlingRecord" :column="1" border>
        <el-descriptions-item label="处置记录"><pre class="alarm-record">{{ currentDetail.handlingRecord }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    <el-dialog v-model="visible" title="告警处置" width="520px"><el-form :model="form" label-width="90px"><el-form-item label="告警"><el-input v-model="form.title" disabled /></el-form-item><el-form-item label="处置意见"><el-input v-model="form.remark" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submitHandle">确认处置</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { alarmApi, resourceApi, telemetryMetrics } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'
const rows = ref([])
const rules = ref([])
const productCategories = ref([])
const deviceGroups = ref([])
const filters = reactive({ keyword: '', status: '', productGroup: '', deviceGroup: '' })
const total = ref(0)
const visible = ref(false)
const detailVisible = ref(false)
const currentId = ref(null)
const currentDetail = ref(null)
const page = reactive({ current: 1, size: 20 })
const sort = reactive({ sortBy: 'createdAt', direction: 'desc' })
const form = reactive({ title: '', remark: '已现场复核并恢复正常。' })
const alarmStatusOptions = [
  { label: '未确认', value: 'OPEN' },
  { label: '已确认', value: 'ACKNOWLEDGED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已恢复', value: 'RECOVERED' },
  { label: '已关闭', value: 'CLOSED' }
]
const timeValue = (row) => new Date(row.createdAt || row.time || 0).getTime() || 0
const detailTitle = computed(() => currentDetail.value ? `${currentDetail.value.deviceName || '设备'} - 指标详情` : '指标详情')
const pageCount = computed(() => Math.ceil(total.value / page.size) || 0)
const currentMetrics = computed(() => currentDetail.value?.metrics || [])
const fallbackMetrics = (row) => [{ metric: 'trigger', label: '触发值', value: row.value || '-', unit: '' }]
const levelText = (level) => ({ HIGH: '高', MEDIUM: '中', LOW: '低', INFO: '低', high: '高', medium: '中', low: '低' }[level] || level || '低')
const levelRank = (level) => ({ 高: 3, 中: 2, 低: 1, 正常: 0 }[level] || 0)
const compare = (value, operator, threshold) => {
  const left = Number(value)
  const right = Number(threshold)
  if (Number.isNaN(left) || Number.isNaN(right)) {
    const equal = String(value ?? '').toLowerCase() === String(threshold ?? '').toLowerCase()
    return operator === '!=' ? !equal : equal
  }
  if (operator === '>=') return left >= right
  if (operator === '<') return left < right
  if (operator === '<=') return left <= right
  if (operator === '==') return left === right
  if (operator === '!=') return left !== right
  return left > right
}
const metricStatus = (metric) => {
  if (metric.metric === 'trigger') return levelText(currentDetail.value?.level)
  const hits = rules.value
    .filter((rule) => rule.enabled !== false && rule.status !== 'DISABLED' && rule.metric === metric.metric)
    .filter((rule) => !currentDetail.value?.ruleId || String(rule.id) === String(currentDetail.value.ruleId))
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
const statusTag = (status) => ({ 未确认: 'danger', 已确认: 'warning', 处理中: 'warning', 已恢复: 'success', 已关闭: 'info' }[status] || 'info')
const openDetail = async (row) => {
  const alarmTime = timeValue(row)
  let telemetryRows = []
  try {
    const telemetry = await resourceApi.list('historicalData', { page: 1, size: 200, deviceName: row.deviceName })
    telemetryRows = normalizeList(telemetry)
  } catch {
    telemetryRows = clone(fallbackRows.historicalData).map((item) => ({ ...item, metrics: [{ metric: item.metric, label: telemetryMetrics.find((metric) => metric.value === item.metric)?.label || item.metric, value: item.value, unit: item.unit }] }))
  }
  const candidates = telemetryRows
    .filter((item) => String(item.deviceId || '') === String(row.deviceId || '') || item.deviceName === row.deviceName)
    .sort((a, b) => Math.abs(timeValue(a) - alarmTime) - Math.abs(timeValue(b) - alarmTime))
  const telemetry = candidates[0]
  currentDetail.value = { ...row, metrics: telemetry?.metrics?.length ? telemetry.metrics : fallbackMetrics(row) }
  detailVisible.value = true
}
const load = async () => { try { const [alarms, ruleList, categoryList, groupList] = await Promise.all([resourceApi.list('historicalAlarm', { ...filters, ...sort, page: page.current, size: page.size }), resourceApi.list('rule'), resourceApi.list('productCategory'), resourceApi.list('deviceGroup')]); rows.value = normalizeList(alarms); total.value = Number(alarms.total ?? rows.value.length); rules.value = normalizeList(ruleList); productCategories.value = normalizeList(categoryList); deviceGroups.value = normalizeList(groupList) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); rows.value = clone(fallbackRows.historicalAlarm); total.value = rows.value.length; rules.value = clone(fallbackRows.rule || []) } }
const search = async () => { page.current = 1; await load() }
const openHandle = (row) => { currentId.value = row.id; form.title = row.title; form.remark = '已现场复核并恢复正常。'; visible.value = true }
const setStatus = async (row, status) => { const user = JSON.parse(localStorage.getItem('iot_user') || '{}'); try { await alarmApi.status(row.id, { status, handler: user.realName || user.username || 'admin' }); ElMessage.success('告警状态已更新'); await load() } catch (error) { quietError(error, '告警状态更新失败') } }
const submitHandle = async () => { const user = JSON.parse(localStorage.getItem('iot_user') || '{}'); try { await alarmApi.handle(currentId.value, { remark: form.remark, handler: user.realName || user.username || 'admin' }); ElMessage.success('告警已关闭'); await load() } catch (error) { quietError(error, '告警关闭失败，后端数据未变更') } visible.value = false }
const handleRealtime = (event) => { if (event.detail?.type === 'telemetry') load() }
const handleSortChange = ({ prop, column, order }) => {
  sort.sortBy = column?.columnKey || prop || 'createdAt'
  sort.direction = order === 'ascending' ? 'asc' : 'desc'
  page.current = 1
  load()
}
watch(() => [filters.keyword, filters.status, filters.productGroup, filters.deviceGroup, page.size], () => { page.current = 1 })
watch(() => [page.current, page.size], load)
onMounted(() => {
  load()
  window.addEventListener('iot-realtime', handleRealtime)
})
onBeforeUnmount(() => window.removeEventListener('iot-realtime', handleRealtime))
</script>

<style scoped>
.alarm-record {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.7;
}
</style>
