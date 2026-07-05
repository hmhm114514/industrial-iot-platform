<template>
  <div class="page-stack">
    <section class="module-head">
      <div>
        <el-tag effect="dark" class="hero-tag">{{ cfg.accent || '模块' }}</el-tag>
        <h1>{{ cfg.title }}</h1>
        <p>{{ cfg.subtitle }}</p>
      </div>
      <div class="module-actions">
        <el-input v-model="keyword" :placeholder="cfg.searchable || '输入关键字搜索'" clearable class="search-input" @keyup.enter="load" />
        <el-button @click="load">查询</el-button>
        <el-button v-if="!cfg.readonly" type="primary" @click="openCreate">新增{{ cfg.accent || '' }}</el-button>
      </div>
    </section>

    <div v-if="cfg.card" class="card-grid">
      <div v-for="row in filteredRows" :key="row.id" class="resource-card">
        <div class="resource-preview">
          <span>{{ cfg.preview || row.product || row.group || row.name }}</span>
        </div>
        <div class="resource-card-body">
          <div class="resource-title">
            <strong>{{ row.name }}</strong>
            <StatusTag :value="row.status" />
          </div>
          <p>{{ row.code || row.resolution || row.location || row.product || '可视化资源' }}</p>
          <div class="resource-meta">
            <span v-if="row.online !== undefined"><i :class="row.online ? 'dot online' : 'dot'" />{{ row.online ? '在线' : '离线' }}</span>
            <span v-if="row.temperature !== undefined">{{ row.temperature }}℃</span>
            <span v-if="row.lastSeen">{{ row.lastSeen }}</span>
          </div>
          <div class="resource-actions" v-if="!cfg.readonly">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="kind === 'device'" text type="success" @click="openSimulate(row)">模拟上报</el-button>
            <el-button text type="danger" @click="removeRow(row)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-card v-else class="table-card" shadow="never">
      <el-table v-loading="loading" :data="filteredRows" stripe>
        <el-table-column type="index" width="56" label="#" />
        <el-table-column v-for="col in cfg.columns" :key="col.prop" v-bind="col">
          <template #default="{ row }">
            <StatusTag v-if="['status', 'tag', 'health', 'online', 'alarm'].includes(col.type)" :value="row[col.prop]" :mode="col.type" />
            <el-switch v-else-if="col.type === 'switch'" v-model="row[col.prop]" @change="toggleRow(row, col.prop)" />
            <span v-else>{{ row[col.prop] ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!cfg.readonly" label="操作" fixed="right" width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="kind === 'device'" link type="success" @click="openSimulate(row)">模拟上报</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item v-for="field in cfg.fields" :key="field.prop" :label="field.label">
          <el-select v-if="field.type === 'select'" v-model="form[field.prop]" class="full" placeholder="请选择">
            <el-option v-for="item in field.options" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" class="full" :min="field.min ?? 0" :max="field.max" />
          <el-input v-else v-model="form[field.prop]" :type="field.type === 'password' ? 'password' : 'text'" :show-password="field.type === 'password'" :placeholder="field.placeholder || `请输入${field.label}`" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="simulateVisible" title="模拟设备遥测上报" width="520px" destroy-on-close>
      <el-alert title="提交后会模拟设备遥测上报，平台可据此写入历史数据并触发阈值告警。" type="info" show-icon :closable="false" />
      <el-form :model="simulateForm" label-width="100px" class="dialog-form">
        <el-form-item label="设备编码"><el-input v-model="simulateForm.deviceCode" disabled /></el-form-item>
        <el-form-item label="温度"><el-input-number v-model="simulateForm.temperature" :min="-20" :max="120" class="full" /></el-form-item>
        <el-form-item label="湿度"><el-input-number v-model="simulateForm.humidity" :min="0" :max="100" class="full" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="simulateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSimulate">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import { resourceApi, telemetryApi } from '../api/platform'
import { quietError } from '../api/http'
import { resourceConfigs, fallbackExtra } from '../config/resources'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'

const StatusTag = defineComponent({
  props: { value: [String, Boolean, Number], mode: String },
  setup(props) {
    return () => {
      const raw = props.value
      const text = raw === true ? '启用' : raw === false ? '停用' : String(raw ?? '-')
      const normalized = text.toLowerCase()
      const okWords = ['enabled', 'running', 'online', 'healthy', 'published', 'ready', 'success', 'hit', '成功', '通过', '启用']
      const warnWords = ['draft', 'testing', 'demo', 'medium', 'pass', '未处理', '中']
      const badWords = ['disabled', 'stopped', 'offline', 'failed', 'high', 'open', '高', '触发', '失败']
      const type = okWords.includes(normalized) || okWords.includes(text) ? 'success' : badWords.includes(normalized) || badWords.includes(text) ? 'danger' : warnWords.includes(normalized) || warnWords.includes(text) ? 'warning' : 'info'
      const labelMap = { enabled: '启用', disabled: '停用', running: '运行中', stopped: '已停止', online: '在线', offline: '离线', healthy: '健康', published: '已发布', ready: '可回放', archived: '已归档', released: '已发布', testing: '测试中', demo: '演示', success: '成功', failed: '失败', hit: '触发', pass: '通过', open: '未处理', closed: '已处理' }
      return h(ElTag, { type, effect: 'light', round: true }, () => labelMap[normalized] || text)
    }
  }
})

const route = useRoute()
const kind = computed(() => route.meta.kind)
const cfg = computed(() => resourceConfigs[kind.value] || resourceConfigs.devTool)
const rows = ref([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const simulateVisible = ref(false)
const editingId = ref(null)
const form = reactive({})
const simulateForm = reactive({ deviceCode: '', deviceName: '', temperature: 36, humidity: 55 })

const dialogTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${cfg.value.accent || cfg.value.title}`)
const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return rows.value
  return rows.value.filter((row) => JSON.stringify(row).toLowerCase().includes(key))
})

const fallback = () => clone(fallbackRows[kind.value] || fallbackExtra[kind.value] || [])

const resetForm = (row = {}) => {
  Object.keys(form).forEach((key) => delete form[key])
  cfg.value.fields.forEach((field) => { form[field.prop] = field.prop === 'password' ? '' : row[field.prop] ?? (field.type === 'number' ? 0 : '') })
  form.status = row.status || 'enabled'
  form.enabled = row.enabled ?? true
}

const load = async () => {
  loading.value = true
  try {
    const payload = await resourceApi.list(kind.value, { keyword: keyword.value })
    rows.value = normalizeList(payload)
  } catch (error) {
    quietError(error, '数据服务暂不可用，当前为演示数据')
    rows.value = fallback()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  resetForm(row)
  dialogVisible.value = true
}

const saveRow = async () => {
  saving.value = true
  const payload = { ...form }
  if (kind.value === 'user' && editingId.value && !payload.password) delete payload.password
  try {
    if (editingId.value) await resourceApi.update(kind.value, editingId.value, payload)
    else await resourceApi.create(kind.value, payload)
    await load()
    ElMessage.success('保存成功')
  } catch (error) {
    quietError(error, '保存失败，后端数据未变更')
  }
  saving.value = false
  dialogVisible.value = false
}

const removeRow = async (row) => {
  await ElMessageBox.confirm(`确认删除“${row.name || row.title || row.id}”？`, '删除确认', { type: 'warning' })
  try {
    await resourceApi.remove(kind.value, row.id)
    await load()
    ElMessage.success('删除成功')
  } catch (error) {
    quietError(error, '删除失败，后端数据未变更')
  }
}

const toggleRow = async (row, prop) => {
  try {
    await resourceApi.toggle(kind.value, row.id, row[prop])
    await load()
    ElMessage.success('状态已更新')
  } catch (error) {
    row[prop] = typeof row[prop] === 'boolean' ? !row[prop] : row[prop]
    quietError(error, '状态更新失败，后端数据未变更')
  }
}

const openSimulate = (row) => {
  simulateForm.deviceCode = row.code
  simulateForm.deviceName = row.name
  simulateForm.temperature = row.temperature || 36
  simulateForm.humidity = 55
  simulateVisible.value = true
}

const submitSimulate = async () => {
  saving.value = true
  try {
    const row = rows.value.find((item) => item.code === simulateForm.deviceCode || item.name === simulateForm.deviceName)
    await telemetryApi.simulate({ deviceId: row?.id, ...simulateForm, timestamp: new Date().toISOString() })
    await load()
    ElMessage.success('模拟上报成功，历史数据与规则告警可查看')
  } catch (error) {
    quietError(error, '模拟上报失败，后端数据未变更')
  }
  saving.value = false
  simulateVisible.value = false
}

watch(() => route.fullPath, load)
onMounted(load)
</script>
