<template>
  <div class="page-stack">
    <section class="module-head">
      <div>
        <el-tag effect="dark" class="hero-tag">规则引擎</el-tag>
        <h1>温度阈值规则设计</h1>
        <p>配置设备温度阈值，模拟遥测上报时可由平台触发告警并写入审计。</p>
      </div>
      <div class="module-actions">
        <el-input v-model="keyword" placeholder="规则名称 / 指标" clearable class="search-input" />
        <el-button @click="load">查询</el-button>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
      </div>
    </section>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="8">
        <el-card class="blueprint-card" shadow="never">
          <div ref="metricChartRef" class="chart rule-metric-chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="16">
        <el-card class="table-card" shadow="never">
          <el-table :data="filteredRows" stripe>
            <el-table-column prop="name" label="规则名称" min-width="190" />
            <el-table-column prop="metric" label="指标" width="120" />
            <el-table-column label="条件" width="420"><template #default="{ row }">{{ row.metric }} {{ row.operator }} {{ row.threshold }}</template></el-table-column>
            <el-table-column prop="level" label="等级" width="90"><template #default="{ row }"><el-tag :type="row.level === '高' ? 'danger' : 'warning'" round>{{ row.level }}</el-tag></template></el-table-column>
            <el-table-column prop="action" label="动作" min-width="180" />
            <el-table-column label="启用" width="90"><template #default="{ row }"><el-switch v-model="row.enabled" @change="toggle(row)" /></template></el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="visible" :title="editingId ? '编辑规则' : '新增规则'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="规则名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="数据指标" prop="metric"><el-select v-model="form.metric" class="full"><el-option v-for="item in metrics" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="判断符" prop="operator"><el-select v-model="form.operator" class="full"><el-option label=">" value=">" /><el-option label=">=" value=">=" /><el-option label="<" value="<" /><el-option label="<=" value="<=" /><el-option label="==" value="==" /></el-select></el-form-item>
        <el-form-item label="阈值" prop="threshold"><el-input-number v-model="form.threshold" class="full" :min="-50" :max="150" /></el-form-item>
        <el-form-item label="告警等级" prop="level"><el-select v-model="form.level" class="full"><el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" /></el-select></el-form-item>
        <el-form-item label="触发动作" prop="action">
          <el-select v-model="form.action" class="full" placeholder="请选择触发动作">
            <el-option v-for="item in actionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resourceApi } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'

const rows = ref([])
const keyword = ref('')
const visible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const metricChartRef = ref(null)
let metricChart
const form = reactive({ name: '', metric: 'temperature', operator: '>', threshold: 38, level: '高', action: '生成告警并通知运维', enabled: true })
const actionOptions = ['生成告警', '生成告警并通知运维', '仅记录规则审计']
const metrics = [
  { label: '温度 temperature', value: 'temperature' },
  { label: '湿度 humidity', value: 'humidity' },
  { label: '压力 pressure', value: 'pressure' },
  { label: '振动 vibration', value: 'vibration' },
  { label: '电流 current', value: 'current' },
  { label: '电压 voltage', value: 'voltage' },
  { label: '功率 power', value: 'power' },
  { label: '转速 speed', value: 'speed' }
]
const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metric: [{ required: true, message: '请选择数据指标', trigger: 'change' }],
  operator: [{ required: true, message: '请选择判断符', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'change' }],
  level: [{ required: true, message: '请选择告警等级', trigger: 'change' }],
  action: [{ required: true, message: '请选择触发动作', trigger: 'change' }]
}
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || JSON.stringify(row).includes(keyword.value)))
const metricName = (value) => metrics.find((item) => item.value === value)?.label.split(' ')[0] || value || '未知'
const metricStats = computed(() => metrics.map((metric) => ({
  name: metricName(metric.value),
  count: rows.value.filter((row) => row.metric === metric.value).length
})))
const renderMetricChart = async () => {
  await nextTick()
  if (!metricChartRef.value) return
  if (!metricChart) metricChart = echarts.init(metricChartRef.value)
  metricChart.setOption({
    title: { text: '指标规则数量', textStyle: { color: '#21304d', fontWeight: 800, fontSize: 16 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 54, right: 22, top: 58, bottom: 24 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: metricStats.value.map((item) => item.name), inverse: true },
    series: [{
      type: 'bar',
      data: metricStats.value.map((item) => item.count),
      barWidth: 16,
      itemStyle: { borderRadius: [0, 8, 8, 0], color: '#1769ff' },
      label: { show: true, position: 'right', formatter: '{c}' }
    }]
  })
}
const resizeMetricChart = () => metricChart?.resize()

const load = async () => {
  try {
    const payload = await resourceApi.list('rule', { keyword: keyword.value })
    rows.value = normalizeList(payload)
  } catch (error) {
    quietError(error, '数据服务暂不可用，当前为演示数据')
    rows.value = clone(fallbackRows.rule)
  }
  renderMetricChart()
}
const fill = (row = {}) => Object.assign(form, { name: row.name || '', metric: row.metric || 'temperature', operator: row.operator || '>', threshold: row.threshold ?? 38, level: row.level || '高', action: row.action || '生成告警并通知运维', enabled: row.enabled ?? true })
const openCreate = () => { editingId.value = null; fill(); visible.value = true }
const openEdit = (row) => { editingId.value = row.id; fill(row); visible.value = true }
const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload = { ...form, alarmLevel: ({ 高: 'HIGH', 中: 'MEDIUM', 低: 'LOW' }[form.level] || form.level), status: form.enabled ? 'ENABLED' : 'DISABLED' }
  try { editingId.value ? await resourceApi.update('rule', editingId.value, payload) : await resourceApi.create('rule', payload); ElMessage.success('规则已保存'); await load() } catch (error) { quietError(error, '规则保存失败，后端数据未变更') }
  visible.value = false
}
const remove = async (row) => { await ElMessageBox.confirm(`确认删除规则“${row.name}”？`, '提示', { type: 'warning' }); try { await resourceApi.remove('rule', row.id); await load() } catch (error) { quietError(error, '规则删除失败，后端数据未变更') } }
const toggle = async (row) => { try { await resourceApi.toggle('rule', row.id, row.enabled); ElMessage.success('启用状态已更新'); await load() } catch (error) { row.enabled = !row.enabled; quietError(error, '规则启停失败，后端数据未变更') } }
onMounted(() => {
  load()
  window.addEventListener('resize', resizeMetricChart)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeMetricChart)
  metricChart?.dispose()
})
</script>
