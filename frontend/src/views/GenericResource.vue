<template>
  <div class="page-stack">
    <section class="module-head">
      <div>
        <el-tag effect="dark" class="hero-tag">{{ cfg.accent || '模块' }}</el-tag>
        <h1>{{ cfg.title }}</h1>
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
            <el-button v-if="kind === 'device'" text type="primary" @click="openView(row)">查看</el-button>
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
            <el-button v-if="kind === 'device'" link type="primary" @click="openView(row)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="kind === 'device'" link type="success" @click="openSimulate(row)">模拟上报</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-form-item v-for="field in cfg.fields" :key="field.prop" :label="field.label" :prop="field.prop">
          <el-select v-if="field.type === 'select'" v-model="form[field.prop]" class="full" placeholder="请选择" :multiple="field.multiple" collapse-tags collapse-tags-tooltip>
            <el-option v-for="item in fieldOptions(field)" :key="optionValue(item, field)" :label="optionLabel(item, field)" :value="optionValue(item, field)" />
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

    <el-dialog v-model="detailVisible" :title="detailTitle" width="760px" destroy-on-close>
      <div class="device-detail">
        <section v-for="section in detailSections" :key="section.title" class="device-detail-section">
          <h3>{{ section.title }}</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item v-for="item in section.items" :key="item.label" :label="item.label">
              <StatusTag v-if="item.type === 'status'" :value="item.value" />
              <span v-else>{{ item.value || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </section>
      </div>
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
const optionSources = ref({})
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const simulateVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const currentDetail = ref(null)
const formRef = ref(null)
const form = reactive({})
const simulateForm = reactive({ deviceCode: '', deviceName: '', temperature: 36, humidity: 55 })

const dialogTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${cfg.value.accent || cfg.value.title}`)
const detailTitle = computed(() => currentDetail.value ? `设备详情 - ${currentDetail.value.name || currentDetail.value.code}` : '设备详情')
const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return rows.value
  return rows.value.filter((row) => JSON.stringify(row).toLowerCase().includes(key))
})
const formRules = computed(() => Object.fromEntries(cfg.value.fields.filter((field) => field.required).map((field) => [field.prop, [{ required: true, message: `请选择或输入${field.label}`, trigger: field.type === 'select' ? 'change' : 'blur' }]])))

const fallback = () => clone(fallbackRows[kind.value] || fallbackExtra[kind.value] || [])
const optionLabel = (item, field) => item && typeof item === 'object' ? item[field.optionLabel || 'label'] ?? item.name ?? item.label : item
const optionValue = (item, field) => item && typeof item === 'object' ? item[field.optionValue || 'value'] ?? item.id ?? item.value : item
const fieldOptions = (field) => field.source ? optionSources.value[field.source] || [] : field.options || []
const valueText = (value) => value === null || value === undefined || value === '' ? '-' : String(value)
const linkStatus = (row = {}) => {
  const raw = String(row.linkStatus || row.status || '').toLowerCase()
  if (row.online === true || raw === 'online') return '在线'
  if (row.online === false || raw === 'offline') return '离线'
  if (raw === 'inactive') return '未激活'
  return row.linkStatus || (row.online === undefined ? '-' : '离线')
}
const coordinateText = (row = {}) => {
  if (row.latitude !== undefined && row.longitude !== undefined) return `${row.latitude}, ${row.longitude}`
  return row.location || '-'
}
const detailSections = computed(() => {
  const row = currentDetail.value || {}
  return [
    {
      title: '基础信息',
      items: [
        { label: '设备名称', value: row.name },
        { label: '设备编码', value: row.code },
        { label: '设备别名', value: row.alias },
        { label: '设备类型', value: row.deviceType || '直连设备' },
        { label: '启用状态', value: row.status, type: 'status' }
      ]
    },
    {
      title: '所属信息',
      items: [
        { label: '所属产品', value: row.product },
        { label: '所属分组', value: row.group },
        { label: '接入协议', value: row.protocol || row.productProtocol },
        { label: '安装位置', value: row.location }
      ]
    },
    {
      title: '连接信息',
      items: [
        { label: '连接状态', value: linkStatus(row), type: 'status' },
        { label: '最后通信', value: row.lastSeen || row.lastTime || row.lastOnlineAt },
        { label: '当前温度', value: row.temperature === undefined ? '-' : `${row.temperature}℃` },
        { label: '经纬度', value: coordinateText(row) }
      ]
    },
    {
      title: '认证与扩展',
      items: [
        { label: '设备 Key', value: row.deviceKey || row.token },
        { label: '父设备', value: row.parentId },
        { label: '固件版本', value: row.otaVersion },
        { label: '说明', value: row.description || row.remark },
        { label: '创建时间', value: row.createdAt },
        { label: '更新时间', value: row.updatedAt }
      ]
    }
  ].map((section) => ({ ...section, items: section.items.map((item) => ({ ...item, value: valueText(item.value) })) }))
})

const loadOptions = async () => {
  const sources = [...new Set(cfg.value.fields.map((field) => field.source).filter(Boolean))]
  if (!sources.length) return
  const entries = await Promise.all(sources.map(async (source) => {
    try {
      return [source, await resourceApi.list(source)]
    } catch (error) {
      return [source, []]
    }
  }))
  optionSources.value = { ...optionSources.value, ...Object.fromEntries(entries) }
}

const withOptionLabels = (items) => {
  const categories = optionSources.value.productCategory || []
  const rules = optionSources.value.rule || []
  const products = optionSources.value.product || []
  const groups = optionSources.value.deviceGroup || []
  if (kind.value === 'device') {
    return items.map((item) => ({
      ...item,
      product: products.find((product) => String(product.id) === String(item.productId))?.name || item.product,
      productProtocol: products.find((product) => String(product.id) === String(item.productId))?.protocol,
      group: groups.find((group) => String(group.id) === String(item.groupId))?.name || item.group
    }))
  }
  if (kind.value !== 'product') return items
  return items.map((item) => ({
    ...item,
    category: categories.find((category) => category.id === item.categoryId)?.name || item.category,
    ruleChain: parseMultiValue(item.ruleIds || item.ruleId).map((id) => rules.find((rule) => String(rule.id) === String(id))?.name).filter(Boolean).join('，') || item.ruleChain || '-'
  }))
}

const parseMultiValue = (value) => Array.isArray(value) ? value : String(value || '').split(',').map((item) => item.trim()).filter(Boolean).map((item) => Number.isNaN(Number(item)) ? item : Number(item))
const resetForm = (row = {}) => {
  Object.keys(form).forEach((key) => delete form[key])
  cfg.value.fields.forEach((field) => { form[field.prop] = field.multiple ? parseMultiValue(row[field.prop] || row.ruleId) : field.prop === 'password' ? '' : row[field.prop] ?? (field.type === 'number' ? 0 : '') })
  form.status = row.status || 'enabled'
  form.enabled = row.enabled ?? true
}

const load = async () => {
  loading.value = true
  try {
    await loadOptions()
    const payload = await resourceApi.list(kind.value, { keyword: keyword.value })
    rows.value = withOptionLabels(normalizeList(payload))
  } catch (error) {
    quietError(error, '数据服务暂不可用，当前为演示数据')
    rows.value = withOptionLabels(fallback())
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

const openView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const saveRow = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = { ...form }
  cfg.value.fields.filter((field) => field.multiple).forEach((field) => { payload[field.prop] = (payload[field.prop] || []).join(',') })
  if (kind.value === 'product' && payload.ruleIds) payload.ruleId = Number(String(payload.ruleIds).split(',')[0])
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
