<template>
  <div class="page-stack monitor-device-page">
    <header class="monitor-device-hero">
      <div>
        <span class="monitor-device-kicker">视频中心 · 监控资产</span>
        <h1>监控设备</h1>
        <p>登记当前浏览器工作站可使用的本机摄像头，并维护生产用途、安装位置和启停状态。</p>
      </div>
      <div class="monitor-device-hero-actions">
        <span class="monitor-workstation-chip">{{ workstationLabel }}</span>
        <el-button :loading="detectionState === 'loading'" :disabled="!environmentReady" @click="detectLocalCameras">检测本机摄像头</el-button>
        <el-button type="primary" :disabled="!environmentReady" @click="openCreate">新增监控设备</el-button>
      </div>
    </header>

    <div v-if="!secureContext || !cameraSupported" class="monitor-environment-alert" role="alert">
      <strong>{{ !secureContext ? '当前地址不能检测摄像头' : '当前浏览器不支持摄像头访问' }}</strong>
      <span>{{ !secureContext ? '请使用 HTTPS 或 localhost 打开本页面。' : '请改用支持摄像头访问的新版浏览器。' }}</span>
    </div>

    <section class="monitor-stat-grid" aria-label="监控设备统计">
      <div class="monitor-stat-card"><span>全部档案</span><strong>{{ statistics.total }}</strong><small>本地摄像头资产</small></div>
      <div class="monitor-stat-card accent"><span>当前工作站</span><strong>{{ statistics.current }}</strong><small>已绑定到本浏览器</small></div>
      <div class="monitor-stat-card success"><span>当前可用</span><strong>{{ statistics.available }}</strong><small>{{ detectionSummary }}</small></div>
      <div class="monitor-stat-card muted"><span>停用设备</span><strong>{{ statistics.disabled }}</strong><small>所有工作站档案</small></div>
    </section>

    <section class="monitor-device-toolbar">
      <div>
        <strong>监控资产清单</strong>
        <span>{{ detectionToolbarText }}</span>
      </div>
      <div class="monitor-device-filters">
        <el-input v-model="keyword" clearable placeholder="搜索名称、编码、用途或位置" />
        <el-select v-model="statusFilter" aria-label="状态筛选">
          <el-option label="全部状态" value="ALL" />
          <el-option label="当前可用" value="AVAILABLE" />
          <el-option label="未检测到" value="MISSING" />
          <el-option label="检测失败" value="DETECTION_ERROR" />
          <el-option label="已启用" value="ENABLED" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
      </div>
    </section>

    <section v-if="detectionState === 'error'" class="monitor-detection-error" role="alert" aria-live="assertive">
      <div>
        <strong>本机摄像头检测失败</strong>
        <p>{{ detectionError }}</p>
      </div>
      <el-button type="warning" :loading="detectionState === 'loading'" @click="detectLocalCameras">重新检测</el-button>
    </section>

    <section v-if="listError" class="monitor-list-error" role="alert">
      <div>
        <strong>监控设备档案加载失败</strong>
        <p>{{ listError }}</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadDevices">重新加载</el-button>
    </section>

    <section v-else v-loading="loading" class="monitor-asset-section">
      <div v-if="filteredDevices.length" class="monitor-asset-grid">
        <article v-for="record in filteredDevices" :key="record.id" class="monitor-asset-card">
          <div class="monitor-asset-head">
            <div>
              <span class="monitor-asset-code" :title="record.code">{{ record.code }}</span>
              <h2 :title="record.name">{{ record.name }}</h2>
            </div>
            <div class="monitor-asset-statuses">
              <span :class="['monitor-status-pill', record.bindingClientId === bindingClientId ? 'is-current' : 'is-remote']">
                {{ record.bindingClientId === bindingClientId ? '当前工作站' : '其他工作站' }}
              </span>
              <span :class="['monitor-status-pill', availabilityTone(record)]">{{ availabilityText(record) }}</span>
            </div>
          </div>

          <div class="monitor-asset-purpose">
            <span>监控用途</span>
            <strong :title="record.purpose || '未填写'">{{ record.purpose || '未填写' }}</strong>
          </div>

          <dl class="monitor-asset-meta">
            <div><dt>安装位置</dt><dd :title="record.location || '未填写'">{{ record.location || '未填写' }}</dd></div>
            <div><dt>系统标签</dt><dd :title="record.deviceLabel || '未记录设备标签'">{{ record.deviceLabel || '未记录设备标签' }}</dd></div>
            <div><dt>最近更新</dt><dd :title="formatDate(record.updatedAt || record.createdAt)">{{ formatDate(record.updatedAt || record.createdAt) }}</dd></div>
          </dl>

          <p v-if="record.remark" class="monitor-asset-remark" :title="record.remark">{{ record.remark }}</p>

          <div class="monitor-asset-actions">
            <el-button :type="canOpenMonitor(record) ? 'primary' : 'default'" @click="openMonitor(record)">查看监控</el-button>
            <el-button @click="openEdit(record)">编辑</el-button>
            <el-button @click="openRebind(record)">重新绑定</el-button>
            <el-button :type="record.status === 'ENABLED' ? 'warning' : 'success'" plain :loading="actionId === record.id" @click="toggleRecord(record)">
              {{ record.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" text :disabled="actionId === record.id" @click="removeRecord(record)">删除</el-button>
          </div>
        </article>
      </div>

      <div v-else class="monitor-empty-state">
        <span>{{ keyword || statusFilter !== 'ALL' ? '筛' : '监' }}</span>
        <h2>{{ keyword || statusFilter !== 'ALL' ? '没有符合条件的监控设备' : '尚未登记监控设备' }}</h2>
        <p>{{ keyword || statusFilter !== 'ALL' ? '请调整搜索词或状态筛选。' : '先检测本机摄像头，再登记生产用途和安装位置。' }}</p>
        <el-button v-if="!keyword && statusFilter === 'ALL'" type="primary" :loading="detectionState === 'loading'" :disabled="!environmentReady" @click="detectLocalCameras">检测本机摄像头</el-button>
      </div>
    </section>

    <el-dialog v-model="businessDialogVisible" :title="editingId ? '编辑监控设备' : '新增监控设备'" width="min(640px, calc(100vw - 28px))" destroy-on-close>
      <el-form ref="businessFormRef" :model="businessForm" :rules="businessRules" label-position="top">
        <div v-if="!editingId" class="monitor-dialog-binding">
          <strong>绑定当前工作站摄像头</strong>
          <p>绑定信息只在新增或明确重新绑定时修改，后续编辑不会改变摄像头来源。</p>
          <el-form-item label="物理摄像头" prop="sourceId">
            <el-select v-model="businessForm.sourceId" class="full" placeholder="请选择已检测到的摄像头">
              <el-option
                v-for="source in physicalSources"
                :key="source.deviceId"
                :value="source.deviceId"
                :label="sourceOptionLabel(source, null)"
                :disabled="sourceIsUsed(source, null)"
              />
            </el-select>
          </el-form-item>
        </div>
        <div v-else class="monitor-dialog-binding is-readonly">
          <strong>当前绑定</strong>
          <p :title="`${editingRecord?.deviceLabel || '未记录设备标签'} · ${editingRecord?.bindingClientId === bindingClientId ? '当前工作站' : '其他工作站'}`">{{ editingRecord?.deviceLabel || '未记录设备标签' }} · {{ editingRecord?.bindingClientId === bindingClientId ? '当前工作站' : '其他工作站' }}</p>
          <small>业务编辑不会修改摄像头绑定。如需调整来源，请使用“重新绑定”。</small>
        </div>

        <div class="monitor-form-grid">
          <el-form-item label="业务名称" prop="name"><el-input v-model="businessForm.name" maxlength="100" placeholder="例如：车床一号位监控" /></el-form-item>
          <el-form-item label="设备编码" prop="code"><el-input v-model="businessForm.code" maxlength="100" placeholder="可编辑的设备资产编码" /></el-form-item>
        </div>
        <el-form-item label="监控用途" prop="purpose"><el-input v-model="businessForm.purpose" maxlength="255" placeholder="例如：车床运行 / 人员作业" /></el-form-item>
        <el-form-item label="安装位置"><el-input v-model="businessForm.location" maxlength="255" placeholder="例如：机加车间 A 区 01 工位" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="businessForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="补充视角、责任班组或使用限制" /></el-form-item>
        <el-form-item label="运行状态"><el-switch v-model="businessForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="businessDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBusinessRecord">{{ editingId ? '保存业务信息' : '登记监控设备' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rebindDialogVisible" title="重新绑定摄像头" width="min(580px, calc(100vw - 28px))" destroy-on-close>
      <div v-if="rebindRecord" class="monitor-rebind-dialog">
        <el-alert
          v-if="rebindRecord.bindingClientId !== bindingClientId"
          title="此档案当前属于其他工作站"
          description="确认后，档案会转移到当前工作站并绑定您选择的摄像头。原工作站将不能继续查看此档案。"
          type="warning"
          show-icon
          :closable="false"
        />
        <div class="monitor-rebind-target">
          <span>待重新绑定</span>
          <strong :title="rebindRecord.name">{{ rebindRecord.name }}</strong>
          <small :title="`${rebindRecord.code} · ${rebindRecord.deviceLabel || '未记录设备标签'}`">{{ rebindRecord.code }} · {{ rebindRecord.deviceLabel || '未记录设备标签' }}</small>
        </div>
        <el-form label-position="top">
          <el-form-item label="选择当前工作站摄像头" required>
            <el-select v-model="rebindSourceId" class="full" placeholder="请选择已检测到的摄像头">
              <el-option
                v-for="source in physicalSources"
                :key="source.deviceId"
                :value="source.deviceId"
                :label="sourceOptionLabel(source, rebindRecord.id)"
                :disabled="sourceIsUsed(source, rebindRecord.id)"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <p class="monitor-rebind-note">系统只会使用您本次明确选择的物理摄像头，不会根据名称或系统标签自动匹配。</p>
      </div>
      <template #footer>
        <el-button @click="rebindDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="confirmRebind">确认重新绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { monitorDeviceApi } from '../api/video'
import { getOrCreateMonitorClientId } from '../utils/monitorClient'
import { normalizeVideoDevices, stopMediaStream } from '../utils/camera'

const router = useRouter()
const bindingClientId = getOrCreateMonitorClientId()
const secureContext = window.isSecureContext
const cameraSupported = Boolean(navigator.mediaDevices?.getUserMedia && navigator.mediaDevices?.enumerateDevices)
const environmentReady = secureContext && cameraSupported

const records = ref([])
const detectedDevices = ref([])
const detectionState = ref('idle')
const detectionError = ref('')
const loading = ref(false)
const saving = ref(false)
const actionId = ref(null)
const listError = ref('')
const keyword = ref('')
const statusFilter = ref('ALL')
const businessDialogVisible = ref(false)
const rebindDialogVisible = ref(false)
const editingId = ref(null)
const rebindRecord = ref(null)
const rebindSourceId = ref('')
const businessFormRef = ref(null)

const businessForm = reactive({
  sourceId: '',
  name: '',
  code: '',
  purpose: '',
  location: '',
  remark: '',
  enabled: true
})

const businessRules = {
  sourceId: [{ required: true, message: '请选择要绑定的物理摄像头', trigger: 'change' }],
  name: [{ required: true, message: '请填写业务名称', trigger: 'blur' }],
  code: [{ required: true, message: '请填写设备编码', trigger: 'blur' }],
  purpose: [{ required: true, message: '请填写监控用途', trigger: 'blur' }]
}

const physicalSources = computed(() => detectedDevices.value.filter((device) => device.deviceId))
const detectedDeviceIds = computed(() => new Set(physicalSources.value.map((device) => device.deviceId)))
const editingRecord = computed(() => records.value.find((record) => record.id === editingId.value) || null)
const workstationLabel = computed(() => `当前工作站 · ${bindingClientId.slice(-8).toUpperCase()}`)
const detectionSummary = computed(() => {
  if (detectionState.value === 'complete') return '已检测且处于启用状态'
  if (detectionState.value === 'error') return '检测失败，状态尚未确认'
  if (detectionState.value === 'loading') return '正在检测本机摄像头'
  return '检测后确认可用状态'
})
const detectionToolbarText = computed(() => {
  if (detectionState.value === 'complete') return `本机检测到 ${physicalSources.value.length} 个视频输入`
  if (detectionState.value === 'error') return '检测失败，未更改设备缺失状态'
  if (detectionState.value === 'loading') return '正在请求权限并读取本机摄像头'
  return '摄像头检测只会在您主动点击后进行'
})

const isCurrentStation = (record) => record.bindingClientId === bindingClientId
const isDetected = (record) => isCurrentStation(record) && detectedDeviceIds.value.has(record.browserDeviceId)
const canOpenMonitor = (record) => isCurrentStation(record) && record.status === 'ENABLED' && isDetected(record)

const statistics = computed(() => ({
  total: records.value.length,
  current: records.value.filter(isCurrentStation).length,
  available: records.value.filter((record) => record.status === 'ENABLED' && isDetected(record)).length,
  disabled: records.value.filter((record) => record.status === 'DISABLED').length
}))

const filteredDevices = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return records.value.filter((record) => {
    const matchesSearch = !search || [record.name, record.code, record.purpose, record.location]
      .some((value) => String(value || '').toLowerCase().includes(search))
    if (!matchesSearch) return false
    if (statusFilter.value === 'AVAILABLE') return record.status === 'ENABLED' && isDetected(record)
    if (statusFilter.value === 'MISSING') return detectionState.value === 'complete' && isCurrentStation(record) && record.status === 'ENABLED' && !isDetected(record)
    if (statusFilter.value === 'DETECTION_ERROR') return detectionState.value === 'error' && isCurrentStation(record) && record.status === 'ENABLED'
    if (statusFilter.value === 'ENABLED') return record.status === 'ENABLED'
    if (statusFilter.value === 'DISABLED') return record.status === 'DISABLED'
    return true
  })
})

const errorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback
const cameraErrorMessage = (error) => {
  const messages = {
    NotAllowedError: '摄像头权限被拒绝，请在浏览器设置中允许访问',
    NotFoundError: '未检测到可用的摄像头',
    NotReadableError: '摄像头正被其他应用占用或无法读取',
    SecurityError: '当前环境不允许访问摄像头'
  }
  return messages[error?.name] || error?.message || '本机摄像头检测失败'
}

const loadDevices = async () => {
  loading.value = true
  listError.value = ''
  try {
    const payload = await monitorDeviceApi.list()
    if (!Array.isArray(payload)) throw new Error('监控设备服务返回的数据格式不正确')
    records.value = payload.filter((record) => record.deviceType === 'LOCAL_CAMERA')
  } catch (error) {
    records.value = []
    listError.value = errorMessage(error, '监控设备服务暂不可用')
  } finally {
    loading.value = false
  }
}

const detectLocalCameras = async () => {
  if (!environmentReady || detectionState.value === 'loading') return
  detectionState.value = 'loading'
  detectionError.value = ''
  detectedDevices.value = []
  let permissionStream = null
  try {
    try {
      permissionStream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true })
    } finally {
      stopMediaStream(permissionStream)
      permissionStream = null
    }
    detectedDevices.value = normalizeVideoDevices(await navigator.mediaDevices.enumerateDevices())
    detectionState.value = 'complete'
    if (physicalSources.value.length) ElMessage.success(`已检测到 ${physicalSources.value.length} 个本机摄像头`)
    else ElMessage.warning('授权成功，但未读取到可绑定的摄像头')
  } catch (error) {
    detectionState.value = 'error'
    detectionError.value = cameraErrorMessage(error)
    detectedDevices.value = []
    ElMessage.error(detectionError.value)
  } finally {
    stopMediaStream(permissionStream)
  }
}

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}

const availabilityText = (record) => {
  if (record.status === 'DISABLED') return '已停用'
  if (!isCurrentStation(record)) return '其他工作站'
  if (isDetected(record)) return '当前可用'
  if (detectionState.value === 'complete') return '未检测到'
  if (detectionState.value === 'error') return '检测失败'
  if (detectionState.value === 'loading') return '检测中'
  return '待检测'
}

const availabilityTone = (record) => {
  if (record.status === 'DISABLED') return 'is-disabled'
  if (!isCurrentStation(record)) return 'is-remote'
  if (isDetected(record)) return 'is-available'
  if (detectionState.value === 'complete') return 'is-missing'
  if (detectionState.value === 'error') return 'is-error'
  return 'is-pending'
}

const generateDeviceCode = () => {
  const now = new Date()
  const date = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  let index = records.value.length + 1
  let code = ''
  do {
    code = `CAM-${date}-${String(index).padStart(3, '0')}`
    index += 1
  } while (records.value.some((record) => record.code === code))
  return code
}

const resetBusinessForm = (record = null) => {
  businessForm.sourceId = ''
  businessForm.name = record?.name || ''
  businessForm.code = record?.code || generateDeviceCode()
  businessForm.purpose = record?.purpose || ''
  businessForm.location = record?.location || ''
  businessForm.remark = record?.remark || ''
  businessForm.enabled = record ? record.status === 'ENABLED' : true
}

const sourceIsUsed = (source, excludedRecordId) => records.value.some((record) =>
  record.id !== excludedRecordId && record.bindingClientId === bindingClientId && record.browserDeviceId === source.deviceId)

const sourceOptionLabel = (source, excludedRecordId) => sourceIsUsed(source, excludedRecordId)
  ? `${source.label} · 已绑定当前工作站档案`
  : source.label

const openCreate = () => {
  if (detectionState.value !== 'complete') {
    ElMessage.warning(detectionState.value === 'error' ? '本机摄像头检测失败，请重新检测后再新增设备' : '请先点击“检测本机摄像头”完成授权和设备识别')
    return
  }
  if (!physicalSources.value.length) {
    ElMessage.warning('当前没有可登记的本机摄像头，请重新检测设备')
    return
  }
  editingId.value = null
  resetBusinessForm()
  businessForm.sourceId = physicalSources.value.find((source) => !sourceIsUsed(source, null))?.deviceId || ''
  businessDialogVisible.value = true
}

const openEdit = (record) => {
  editingId.value = record.id
  resetBusinessForm(record)
  businessDialogVisible.value = true
}

const saveBusinessRecord = async () => {
  try {
    await businessFormRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const businessData = {
      name: businessForm.name.trim(),
      code: businessForm.code.trim(),
      purpose: businessForm.purpose.trim(),
      location: businessForm.location.trim(),
      remark: businessForm.remark.trim(),
      enabled: businessForm.enabled
    }
    if (editingId.value) {
      await monitorDeviceApi.update(editingId.value, businessData)
      ElMessage.success('监控设备业务信息已更新')
    } else {
      const source = physicalSources.value.find((device) => device.deviceId === businessForm.sourceId)
      if (!source || sourceIsUsed(source, null)) throw new Error('请选择尚未绑定的本机摄像头')
      await monitorDeviceApi.create({
        ...businessData,
        bindingClientId,
        browserDeviceId: source.deviceId,
        browserGroupId: source.groupId,
        deviceLabel: source.label
      })
      ElMessage.success('监控设备已登记')
    }
    businessDialogVisible.value = false
    await loadDevices()
  } catch (error) {
    ElMessage.error(errorMessage(error, '监控设备保存失败'))
  } finally {
    saving.value = false
  }
}

const openRebind = (record) => {
  if (detectionState.value !== 'complete') {
    ElMessage.warning(detectionState.value === 'error' ? '本机摄像头检测失败，请重新检测后再进行绑定' : '请先检测当前工作站的物理摄像头，再进行重新绑定')
    return
  }
  if (!physicalSources.value.length) {
    ElMessage.warning('当前工作站未检测到可绑定的摄像头')
    return
  }
  rebindRecord.value = record
  rebindSourceId.value = physicalSources.value.find((source) => !sourceIsUsed(source, record.id))?.deviceId || ''
  rebindDialogVisible.value = true
}

const confirmRebind = async () => {
  const record = rebindRecord.value
  const source = physicalSources.value.find((device) => device.deviceId === rebindSourceId.value)
  if (!record || !source) {
    ElMessage.warning('请选择要绑定的当前工作站摄像头')
    return
  }
  if (sourceIsUsed(source, record.id)) {
    ElMessage.warning('该摄像头已绑定当前工作站的其他档案')
    return
  }
  saving.value = true
  try {
    await monitorDeviceApi.rebind(record.id, {
      bindingClientId,
      browserDeviceId: source.deviceId,
      browserGroupId: source.groupId,
      deviceLabel: source.label
    })
    ElMessage.success('监控设备已重新绑定到当前工作站')
    rebindDialogVisible.value = false
    await loadDevices()
  } catch (error) {
    ElMessage.error(errorMessage(error, '重新绑定失败'))
  } finally {
    saving.value = false
  }
}

const toggleRecord = async (record) => {
  actionId.value = record.id
  try {
    await monitorDeviceApi.toggle(record.id)
    ElMessage.success(record.status === 'ENABLED' ? '监控设备已停用' : '监控设备已启用')
    await loadDevices()
  } catch (error) {
    ElMessage.error(errorMessage(error, '监控设备状态更新失败'))
  } finally {
    actionId.value = null
  }
}

const removeRecord = async (record) => {
  try {
    await ElMessageBox.confirm(`删除后将无法从监控查看中选择“${record.name}”，是否继续？`, '删除监控设备', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    actionId.value = record.id
    await monitorDeviceApi.remove(record.id)
    ElMessage.success('监控设备已删除')
    await loadDevices()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '监控设备删除失败'))
  } finally {
    actionId.value = null
  }
}

const openMonitor = (record) => {
  if (!isCurrentStation(record)) {
    ElMessage.warning('该档案属于其他工作站，请先重新绑定到当前工作站')
    return
  }
  if (record.status !== 'ENABLED') {
    ElMessage.warning('该监控设备已停用，请先启用后再查看')
    return
  }
  if (!isDetected(record)) {
    ElMessage.warning(detectionState.value === 'complete' ? '当前未检测到绑定的摄像头，请重新检测或重新绑定' : detectionState.value === 'error' ? '本机摄像头检测失败，请重新检测后再查看' : '请先检测本机摄像头，确认设备可用后再查看')
    return
  }
  void router.push({ path: '/video/monitor', query: { deviceId: String(record.id) } })
}

const handlePhysicalDeviceChange = async () => {
  if (detectionState.value !== 'complete' || !cameraSupported) return
  try {
    detectedDevices.value = normalizeVideoDevices(await navigator.mediaDevices.enumerateDevices())
  } catch (error) {
    detectionState.value = 'error'
    detectionError.value = cameraErrorMessage(error)
    detectedDevices.value = []
    ElMessage.warning(detectionError.value)
  }
}

onMounted(() => {
  void loadDevices()
  navigator.mediaDevices?.addEventListener?.('devicechange', handlePhysicalDeviceChange)
})

onBeforeUnmount(() => {
  navigator.mediaDevices?.removeEventListener?.('devicechange', handlePhysicalDeviceChange)
})
</script>
