<template>
  <div class="local-camera-page">
    <header class="camera-hero">
      <div class="camera-hero-copy">
        <span class="camera-eyebrow">视频中心 · 当前工作站</span>
        <h1>监控查看</h1>
      </div>
      <div class="camera-hero-action">
        <span class="camera-status-announcer" role="status" aria-live="polite" aria-atomic="true">当前状态：{{ statusMeta.label }}</span>
        <div :class="['camera-state-badge', `is-${statusMeta.tone}`]">
          <i aria-hidden="true"></i>
          <span>{{ statusMeta.label }}</span>
          <strong v-if="cameraState.recording">{{ formattedRecordingElapsed }}</strong>
        </div>
        <el-button
          class="camera-primary-action"
          :type="cameraState.stream ? 'danger' : 'primary'"
          size="large"
          :loading="primaryActionLoading || (registryLoading && !cameraState.stream)"
          :disabled="primaryActionDisabled"
          @click="handlePrimaryAction"
        >
          {{ primaryActionText }}
        </el-button>
      </div>
    </header>

    <main class="camera-workspace">
      <section ref="stageRef" class="camera-stage" aria-label="工业监控实时画面">
        <div class="camera-viewport">
          <div class="camera-stage-grid" aria-hidden="true"></div>

          <div class="camera-stage-top">
            <div :title="selectedRecord?.name || statusMeta.label">
              <span class="stage-live-dot" :class="{ active: cameraState.stream }"></span>
              {{ selectedRecord?.name || statusMeta.label }}
            </div>
            <span v-if="cameraState.stream" class="stream-metrics">{{ streamMetrics }}</span>
          </div>

          <video
            ref="videoRef"
            class="camera-video"
            :class="{ mirrored }"
            autoplay
            muted
            playsinline
            @loadedmetadata="playCurrentStream"
          ></video>

          <div v-if="!cameraState.stream" class="camera-placeholder" :class="{ 'has-error': displayError }">
            <div class="camera-lens" aria-hidden="true"><span></span></div>
            <strong>{{ statusMeta.title }}</strong>
            <p>{{ statusMeta.description }}</p>
            <div v-if="displayError" class="camera-connection-error" role="alert" aria-live="assertive">{{ displayError }}</div>
          </div>

          <div v-if="playbackError && cameraState.stream" class="camera-playback-notice" role="status" aria-live="polite">
            <span>{{ playbackError }}</span>
            <el-button type="primary" plain size="small" @click="playCurrentStream">播放画面</el-button>
          </div>

          <div v-if="cameraState.recording" class="recording-indicator">
            <i aria-hidden="true"></i>
            本地录制 {{ formattedRecordingElapsed }}
          </div>
        </div>

        <div v-if="cameraState.qualityWarning" class="camera-quality-warning" role="status" aria-live="polite">
          <strong>画质提示</strong>
          <span>{{ cameraState.qualityWarning }}</span>
        </div>

        <div class="camera-control-dock">
          <el-button :disabled="!cameraState.stream" :class="{ 'is-active-control': mirrored }" @click="mirrored = !mirrored">
            {{ mirrored ? '取消镜像' : '镜像画面' }}
          </el-button>
          <el-button type="primary" plain :disabled="!cameraState.stream" @click="captureFrame">截取当前帧</el-button>
          <el-button
            class="recording-control"
            :type="cameraState.recording ? 'danger' : 'default'"
            :disabled="!cameraState.stream || !recordingSupported"
            :loading="recordingActionLoading"
            @click="toggleRecording"
          >
            <span class="recording-label-full">{{ cameraState.recording ? `停止录像 ${formattedRecordingElapsed}` : '开始本地录像' }}</span>
            <span class="recording-label-compact">{{ cameraState.recording ? '停止录像' : '开始录像' }}</span>
          </el-button>
          <el-button :disabled="!cameraState.stream" :aria-pressed="fullscreenActive" @click="toggleFullscreen">{{ fullscreenActive ? '退出全屏' : '全屏' }}</el-button>
        </div>
      </section>

      <aside class="camera-settings monitor-viewer-sidebar">
        <div class="settings-heading">
          <div>
            <span>当前工作站</span>
            <h2>监控设备</h2>
          </div>
          <el-button text type="primary" :loading="registryLoading" :disabled="switchingCamera || cameraState.recording || cameraState.status === 'connecting' || primaryActionLoading" @click="loadMonitorRecords">刷新档案</el-button>
        </div>

        <div class="monitor-viewer-tools">
          <el-button
            class="full"
            :loading="detectingDevices"
            :disabled="!environmentReady || cameraState.status === 'connecting' || switchingCamera || cameraState.recording"
            @click="detectLocalDevices"
          >
            检测本机摄像头
          </el-button>
          <small>{{ detectionHelperText }}</small>
        </div>

        <div v-if="registryError" class="monitor-registry-error" role="alert">
          <strong>{{ registryBlockingError ? '监控档案加载失败' : '监控档案刷新失败' }}</strong>
          <p>{{ registryError }}</p>
          <el-button type="primary" plain size="small" @click="loadMonitorRecords">重新加载</el-button>
        </div>

        <div v-if="!registryLoading && !monitorRecords.length && !registryBlockingError" class="monitor-viewer-empty">
          <span aria-hidden="true">监</span>
          <strong>当前工作站没有启用的监控设备</strong>
          <p>请先登记摄像头及其生产用途，再返回监控查看。</p>
          <el-button type="primary" @click="goToDeviceManagement">前往监控设备</el-button>
        </div>

        <div v-if="monitorRecords.length" class="monitor-source-list" aria-label="当前工作站监控设备">
          <button
            v-for="record in monitorRecords"
            :key="record.id"
            type="button"
            :class="['monitor-source-card', { active: record.id === selectedRecordId, switching: switchingTargetId === record.id }]"
            :disabled="cameraState.recording || switchingCamera || cameraState.status === 'connecting' || primaryActionLoading || registryLoading"
            :aria-pressed="record.id === selectedRecordId"
            :aria-label="`${record.name}；用途：${record.purpose || '未填写'}；位置：${record.location || '未填写'}；状态：${recordAvailabilityText(record)}`"
            @click="selectMonitorRecord(record)"
          >
            <span class="monitor-source-status" :class="recordAvailabilityTone(record)">{{ recordAvailabilityText(record) }}</span>
            <strong :title="record.name">{{ record.name }}</strong>
            <small :title="record.purpose || '未填写监控用途'">{{ record.purpose || '未填写监控用途' }}</small>
            <span class="monitor-source-location" :title="record.location || '未填写安装位置'">{{ record.location || '未填写安装位置' }}</span>
            <span class="monitor-source-label" :title="record.deviceLabel || '未记录设备标签'">{{ record.deviceLabel || '未记录设备标签' }}</span>
          </button>
        </div>

        <div v-if="selectedRecord" class="camera-facts monitor-viewer-facts">
          <div><span>当前监控</span><strong :title="selectedRecord.name">{{ selectedRecord.name }}</strong></div>
          <div><span>监控用途</span><strong :title="selectedRecord.purpose || '未填写'">{{ selectedRecord.purpose || '未填写' }}</strong></div>
          <div><span>安装位置</span><strong :title="selectedRecord.location || '未填写'">{{ selectedRecord.location || '未填写' }}</strong></div>
          <div><span>实际画面</span><strong>{{ cameraState.stream ? streamMetrics : '连接后显示' }}</strong></div>
          <div><span>安全环境</span><strong>{{ secureContext ? '已满足' : '需要 HTTPS 或 localhost' }}</strong></div>
          <div><span>画质策略</span><strong>自动最佳</strong></div>
          <div><span>最长录像</span><strong>单次 5 分钟</strong></div>
        </div>

        <div v-if="!recordingSupported" class="recording-support-note">当前浏览器不支持本地录像；实时预览、截图和全屏仍可使用。</div>

      <div class="recording-result-panel" :class="{ ready: cameraState.recordingResult }" role="status" aria-live="polite">
        <div class="result-icon" aria-hidden="true">↓</div>
        <div class="result-copy">
          <span>最新录像</span>
          <template v-if="cameraState.recordingResult">
            <h3>录像文件已生成</h3>
            <p v-if="recordingResultOwner" class="result-owner" :title="recordingResultOwnerLabel">归属：{{ recordingResultOwnerLabel }}</p>
            <p>{{ recordingResultSummary }}</p>
          </template>
          <template v-else>
            <h3>暂无可下载录像</h3>
          </template>
        </div>
        <div class="result-actions">
          <el-button type="primary" :disabled="!cameraState.recordingResult" @click="downloadLatestRecording">下载文件</el-button>
          <el-button v-if="cameraState.recordingResult" text @click="clearLatestRecording">清除</el-button>
        </div>
      </div>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resourceApi } from '../api/platform'
import { monitorDeviceApi } from '../api/video'
import { createCameraController, normalizeVideoDevices, stopMediaStream } from '../utils/camera'
import { getOrCreateMonitorClientId } from '../utils/monitorClient'

const route = useRoute()
const router = useRouter()
const bindingClientId = getOrCreateMonitorClientId()
const secureContext = window.isSecureContext
const cameraSupported = Boolean(navigator.mediaDevices?.getUserMedia && navigator.mediaDevices?.enumerateDevices)
const recordingSupported = Boolean(window.MediaRecorder)
const environmentReady = secureContext && cameraSupported
const currentOperator = () => {
  try {
    const user = JSON.parse(localStorage.getItem('iot_user') || '{}')
    return user.realName || user.username || 'admin'
  } catch (error) {
    return 'admin'
  }
}
const writeOperationLog = (action, detail) => resourceApi.create('operationLog', {
  moduleName: '视频中心',
  action,
  operator: currentOperator(),
  detail
}).catch(() => {})

const controller = createCameraController({
  mediaDevices: navigator.mediaDevices,
  MediaRecorderCtor: window.MediaRecorder,
  urlApi: URL
})

const videoRef = ref(null)
const stageRef = ref(null)
const cameraState = ref(controller.getSnapshot())
const monitorRecords = ref([])
const selectedRecordId = ref(null)
const detectedDevices = ref([])
const detectionState = ref('idle')
const detectionMessage = ref('')
const registryLoading = ref(false)
const registryError = ref('')
const registryLoaded = ref(false)
const querySelectionError = ref('')
const detectingDevices = ref(false)
const primaryActionLoading = ref(false)
const recordingActionLoading = ref(false)
const switchingCamera = ref(false)
const switchingTargetId = ref(null)
const mirrored = ref(true)
const playbackError = ref('')
const fullscreenActive = ref(false)
const activeRecordingOwner = ref(null)
const recordingResultOwner = ref(null)

let unsubscribe = null
const temporaryDownloadUrls = new Set()
const temporaryDownloadTimers = new Set()

const selectedRecord = computed(() => monitorRecords.value.find((record) => record.id === selectedRecordId.value) || null)
const switchingRecord = computed(() => monitorRecords.value.find((record) => record.id === switchingTargetId.value) || null)
const registryBlockingError = computed(() => Boolean(registryError.value && !registryLoaded.value && !cameraState.value.stream))
const detectedDeviceIds = computed(() => new Set(detectedDevices.value.filter((device) => device.deviceId).map((device) => device.deviceId)))
const streamSettings = computed(() => cameraState.value.stream?.getVideoTracks?.()[0]?.getSettings?.() || {})
const streamMetrics = computed(() => {
  const { width, height, frameRate } = streamSettings.value
  const dimensions = width && height ? `${width} × ${height}` : '自动最佳画质'
  return frameRate ? `${dimensions} · ${Math.round(frameRate)} fps` : dimensions
})

const isRecordAvailable = (record) => Boolean(record?.browserDeviceId) && (
  detectedDeviceIds.value.has(record.browserDeviceId) ||
  (cameraState.value.stream && selectedRecordId.value === record.id && cameraState.value.deviceId === record.browserDeviceId)
)

const selectedBindingInvalid = computed(() => Boolean(selectedRecord.value && !selectedRecord.value.browserDeviceId))
const selectedSourceMissing = computed(() => selectedBindingInvalid.value || (
  detectionState.value === 'complete' && selectedRecord.value && !isRecordAvailable(selectedRecord.value)
))
const recordingResultOwnerLabel = computed(() => {
  if (!recordingResultOwner.value) return ''
  return recordingResultOwner.value.code
    ? `${recordingResultOwner.value.name} · ${recordingResultOwner.value.code}`
    : recordingResultOwner.value.name
})

const statusMeta = computed(() => {
  if (cameraState.value.recording && selectedRecord.value) return { label: '录制中', title: selectedRecord.value.name, description: `${selectedRecord.value.purpose || '工业现场监控'} · 本地录像最长 5 分钟`, tone: 'recording' }
  if (cameraState.value.stream && selectedRecord.value) return { label: '实时', title: selectedRecord.value.name, description: `${selectedRecord.value.purpose || '工业现场监控'} · ${selectedRecord.value.location || '位置未填写'}`, tone: 'live' }
  if (registryLoading.value) return { label: '加载中', title: '正在加载监控档案', description: '请稍候，系统正在读取当前工作站的监控设备。', tone: 'progress' }
  if (registryBlockingError.value) return { label: '服务错误', title: '监控档案暂不可用', description: '未加载任何备用设备，请恢复服务后重新加载。', tone: 'error' }
  if (querySelectionError.value) return { label: '档案不可用', title: '指定监控档案不可用', description: querySelectionError.value, tone: 'warning' }
  if (!monitorRecords.value.length) return { label: '无设备', title: '当前工作站没有启用的监控设备', description: '请先前往监控设备页面登记或启用摄像头档案。', tone: 'muted' }
  if (!selectedRecord.value) return { label: '待选择', title: '请选择监控设备', description: '从右侧列表选择要查看的工业监控点位。', tone: 'idle' }
  if (!secureContext) return { label: '不安全上下文', title: '当前地址不能访问摄像头', description: '请使用 HTTPS 或 localhost 打开本页面。', tone: 'warning' }
  if (!cameraSupported) return { label: '不支持', title: '当前浏览器不支持摄像头访问', description: '请改用支持摄像头访问的新版浏览器。', tone: 'muted' }
  if (cameraState.value.status === 'connecting') return { label: switchingCamera.value ? '切换中' : '连接中', title: switchingCamera.value ? '正在切换监控设备' : '正在连接监控设备', description: `目标监控：${switchingRecord.value?.name || selectedRecord.value.name}`, tone: 'progress' }
  if (selectedSourceMissing.value) return { label: '未检测到', title: '绑定的摄像头当前不可用', description: '请确认设备已接入；若系统摄像头已变化，请前往监控设备重新绑定。', tone: 'warning' }
  if (cameraState.value.status === 'ended') return { label: '已断开', title: '监控连接已结束', description: '所选监控档案会保留，请检测设备并按需重新绑定。', tone: 'error' }
  if (cameraState.value.status === 'error') return { label: '错误', title: '监控设备连接失败', description: '系统不会改用其他摄像头，请检查权限、占用情况或绑定关系。', tone: 'error' }
  return { label: '待连接', title: selectedRecord.value.name, description: `${selectedRecord.value.purpose || '工业现场监控'} · ${selectedRecord.value.location || '位置未填写'}`, tone: 'idle' }
})

const displayError = computed(() => {
  if (registryBlockingError.value) return registryError.value
  if (querySelectionError.value) return querySelectionError.value
  if (detectionState.value === 'error' && detectionMessage.value) return detectionMessage.value
  return cameraState.value.error || ''
})

const primaryActionText = computed(() => {
  if (cameraState.value.stream) return '停止监控'
  if (registryBlockingError.value) return '重新加载设备'
  if ((querySelectionError.value && monitorRecords.value.length) || (!selectedRecord.value && monitorRecords.value.length)) return '选择其他监控设备'
  if (!monitorRecords.value.length) return '前往监控设备'
  if (selectedSourceMissing.value || cameraState.value.status === 'ended') return '前往重新绑定'
  if (!secureContext) return '请使用安全地址'
  if (!cameraSupported) return '当前浏览器不支持'
  if (cameraState.value.status === 'connecting') return switchingCamera.value ? '正在切换' : '正在连接'
  return '连接监控'
})

const primaryActionDisabled = computed(() => {
  if (primaryActionLoading.value || switchingCamera.value || cameraState.value.status === 'connecting') return true
  if (cameraState.value.stream) return false
  if (registryLoading.value) return true
  if (registryBlockingError.value || querySelectionError.value || !monitorRecords.value.length || !selectedRecord.value || selectedSourceMissing.value || cameraState.value.status === 'ended') return false
  return !environmentReady
})

const detectionHelperText = computed(() => {
  if (detectionState.value === 'complete') return `已检测到 ${detectedDevices.value.filter((device) => device.deviceId).length} 个本机摄像头`
  if (detectionState.value === 'error') return detectionMessage.value || '本机摄像头检测失败'
  return '检测会临时请求权限，不会保留预览流'
})

const formatDuration = (totalSeconds = 0) => {
  const seconds = Math.max(0, Math.floor(totalSeconds))
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

const formattedRecordingElapsed = computed(() => formatDuration(cameraState.value.recordingElapsed))
const recordingResultSummary = computed(() => {
  const result = cameraState.value.recordingResult
  if (!result) return ''
  const size = result.blob?.size || 0
  const readableSize = size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.ceil(size / 1024))} KB`
  return `${formatDuration(Math.round((result.duration || 0) / 1000))} · ${readableSize} · ${String(result.extension || 'webm').toUpperCase()}`
})

const errorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback
const cameraErrorMessage = (error) => {
  const messages = {
    NotAllowedError: '摄像头权限被拒绝，请在浏览器设置中允许访问',
    NotFoundError: '未检测到档案绑定的摄像头，请重新检测或重新绑定',
    OverconstrainedError: '档案绑定的摄像头已失效，请重新绑定',
    NotReadableError: '摄像头正被其他应用占用或无法读取',
    SecurityError: '当前环境不允许访问摄像头'
  }
  return messages[error?.name] || error?.message || '摄像头操作失败'
}

const syncRouteQuery = (recordId) => {
  const query = { ...route.query }
  if (recordId) query.deviceId = String(recordId)
  else delete query.deviceId
  if (String(route.query.deviceId || '') !== String(recordId || '')) void router.replace({ path: '/video/monitor', query })
}

const loadMonitorRecords = async () => {
  if (registryLoading.value) return
  registryLoading.value = true
  registryError.value = ''
  const hadLiveStream = Boolean(cameraState.value.stream)
  try {
    const payload = await monitorDeviceApi.list({ bindingClientId })
    if (!Array.isArray(payload)) throw new Error('监控设备服务返回的数据格式不正确')
    const nextRecords = payload.filter((record) =>
      record.deviceType === 'LOCAL_CAMERA' && record.bindingClientId === bindingClientId && record.status === 'ENABLED')
    const liveDeviceId = cameraState.value.deviceId
    const liveRecord = hadLiveStream && liveDeviceId
      ? nextRecords.find((record) => record.id === selectedRecordId.value && record.browserDeviceId === liveDeviceId) || null
      : null

    if (hadLiveStream && !liveRecord) await controller.stop()

    monitorRecords.value = nextRecords
    registryLoaded.value = true

    if (cameraState.value.stream && liveRecord) {
      selectedRecordId.value = liveRecord.id
      querySelectionError.value = ''
      syncRouteQuery(liveRecord.id)
      return
    }

    const queryId = String(route.query.deviceId || '').trim()
    const hasQueryDeviceId = queryId.length > 0
    if (hasQueryDeviceId) {
      const requested = nextRecords.find((record) => String(record.id) === queryId) || null
      selectedRecordId.value = requested?.id || null
      querySelectionError.value = requested
        ? ''
        : '该档案可能已停用、删除、转移到其他工作站，或不属于当前工作站。请从监控设备列表重新选择。'
      return
    }

    const previousSelection = nextRecords.find((record) => record.id === selectedRecordId.value) || null
    const nextSelected = previousSelection || nextRecords[0] || null
    selectedRecordId.value = nextSelected?.id || null
    querySelectionError.value = ''
    syncRouteQuery(selectedRecordId.value)
  } catch (error) {
    registryError.value = errorMessage(error, '监控设备服务暂不可用')
    if (!registryLoaded.value && !hadLiveStream) {
      monitorRecords.value = []
      selectedRecordId.value = null
    }
  } finally {
    registryLoading.value = false
  }
}

const enumerateDetectedDevices = async () => {
  const devices = normalizeVideoDevices(await navigator.mediaDevices.enumerateDevices())
  detectedDevices.value = devices.filter((device) => device.deviceId)
  return detectedDevices.value
}

const detectLocalDevices = async () => {
  if (!environmentReady || detectingDevices.value) return
  detectingDevices.value = true
  detectionMessage.value = ''
  let permissionStream = null
  try {
    if (cameraState.value.stream) {
      await controller.enumerateDevices()
      detectedDevices.value = cameraState.value.devices.filter((device) => device.deviceId)
    } else {
      try {
        permissionStream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true })
      } finally {
        stopMediaStream(permissionStream)
        permissionStream = null
      }
      await enumerateDetectedDevices()
      await controller.enumerateDevices()
    }
    detectionState.value = 'complete'
    ElMessage.success(`已检测到 ${detectedDevices.value.length} 个本机摄像头`)
  } catch (error) {
    detectionState.value = 'error'
    detectionMessage.value = cameraErrorMessage(error)
    detectedDevices.value = []
    ElMessage.error(detectionMessage.value)
  } finally {
    stopMediaStream(permissionStream)
    detectingDevices.value = false
  }
}

const recordAvailabilityText = (record) => {
  if (!record.browserDeviceId) return '绑定失效'
  if (isRecordAvailable(record)) return '当前可用'
  if (detectionState.value === 'complete') return '未检测到'
  if (detectionState.value === 'error') return '检测失败'
  return '待检测'
}

const recordAvailabilityTone = (record) => !record.browserDeviceId
  ? 'is-missing'
  : isRecordAvailable(record)
  ? 'is-available'
  : detectionState.value === 'complete'
    ? 'is-missing'
    : 'is-pending'

const playCurrentStream = async () => {
  if (!videoRef.value || !cameraState.value.stream) return
  try {
    await videoRef.value.play()
    playbackError.value = ''
  } catch (error) {
    console.warn('监控画面自动播放失败', error)
    playbackError.value = '浏览器未能自动播放画面，请手动开始播放。'
  }
}

const applySnapshot = (snapshot) => {
  const previousSnapshot = cameraState.value
  cameraState.value = snapshot
  if (snapshot.stream && snapshot.deviceId) {
    const streamRecord = monitorRecords.value.find((record) => record.browserDeviceId === snapshot.deviceId)
    if (streamRecord && selectedRecordId.value !== streamRecord.id) {
      selectedRecordId.value = streamRecord.id
      querySelectionError.value = ''
      syncRouteQuery(streamRecord.id)
    }
  }
  if (snapshot.stream) detectionState.value = 'complete'
  detectedDevices.value = snapshot.devices.filter((device) => device.deviceId)
  if (previousSnapshot.recording && !snapshot.recording) {
    if (snapshot.recordingResult && snapshot.recordingResult !== previousSnapshot.recordingResult) {
      recordingResultOwner.value = activeRecordingOwner.value || (selectedRecord.value
        ? { id: selectedRecord.value.id, name: selectedRecord.value.name, code: selectedRecord.value.code }
        : null)
      ElMessage.success('录像已停止，文件已生成')
    }
    activeRecordingOwner.value = null
  }
  if (previousSnapshot.recordingResult && !snapshot.recordingResult) recordingResultOwner.value = null
  const video = videoRef.value
  if (!video || video.srcObject === snapshot.stream) return
  video.srcObject = snapshot.stream || null
  if (snapshot.stream) void nextTick(() => playCurrentStream())
  else playbackError.value = ''
}

const connectMonitor = async () => {
  const record = selectedRecord.value
  if (!record || !environmentReady || primaryActionLoading.value) return
  if (!record.browserDeviceId) {
    ElMessage.warning('该监控档案缺少有效的摄像头绑定，请前往监控设备重新绑定')
    return
  }
  primaryActionLoading.value = true
  detectionMessage.value = ''
  try {
    await controller.connect(record.browserDeviceId)
    await writeOperationLog('连接摄像头', `${record.name || '本机摄像头'}：${record.location || record.purpose || '实时监控'}`)
  } catch (error) {
    const message = cameraErrorMessage(error)
    if (['NotFoundError', 'OverconstrainedError', 'ConstraintNotSatisfiedError'].includes(error?.name)) {
      detectionState.value = 'complete'
      await enumerateDetectedDevices().catch(() => { detectedDevices.value = [] })
    } else if (error?.name === 'NotAllowedError' || error?.name === 'SecurityError') {
      detectionState.value = 'error'
      detectionMessage.value = message
    }
    ElMessage.error(cameraState.value.error || message)
  } finally {
    primaryActionLoading.value = false
  }
}

const stopMonitor = async () => {
  if (primaryActionLoading.value) return
  primaryActionLoading.value = true
  try {
    await controller.stop()
  } catch (error) {
    ElMessage.error(errorMessage(error, '停止监控失败'))
  } finally {
    primaryActionLoading.value = false
  }
}

const goToDeviceManagement = () => router.push('/video/devices')

const focusMonitorSelection = async () => {
  querySelectionError.value = ''
  const query = { ...route.query }
  delete query.deviceId
  await router.replace({ path: '/video/monitor', query })
  await nextTick()
  document.querySelector('.local-camera-page .monitor-source-card')?.focus()
}

const handlePrimaryAction = () => {
  if (cameraState.value.stream) return stopMonitor()
  if (registryBlockingError.value) return loadMonitorRecords()
  if ((querySelectionError.value && monitorRecords.value.length) || (!selectedRecord.value && monitorRecords.value.length)) return focusMonitorSelection()
  if (!monitorRecords.value.length || selectedSourceMissing.value || cameraState.value.status === 'ended') return goToDeviceManagement()
  return connectMonitor()
}

const selectMonitorRecord = async (record) => {
  if (!record || record.id === selectedRecordId.value) return
  if (cameraState.value.recording || switchingCamera.value || cameraState.value.status === 'connecting' || primaryActionLoading.value || registryLoading.value) {
    syncRouteQuery(selectedRecordId.value)
    return
  }
  if (!cameraState.value.stream) {
    selectedRecordId.value = record.id
    querySelectionError.value = ''
    syncRouteQuery(record.id)
    return
  }
  if (!isRecordAvailable(record)) {
    ElMessage.warning('当前未检测到该监控设备绑定的摄像头，请先检测或重新绑定')
    syncRouteQuery(selectedRecordId.value)
    return
  }
  switchingCamera.value = true
  switchingTargetId.value = record.id
  try {
    const stream = await controller.switchCamera(record.browserDeviceId)
    if (stream) {
      selectedRecordId.value = record.id
      querySelectionError.value = ''
      syncRouteQuery(record.id)
      ElMessage.success(`已切换到“${record.name}”`)
    }
  } catch (error) {
    ElMessage.error(cameraState.value.error || cameraErrorMessage(error) || '监控设备切换失败，已保留原画面')
  } finally {
    switchingCamera.value = false
    switchingTargetId.value = null
  }
}

watch(() => route.query.deviceId, async (deviceId) => {
  if (registryLoading.value || !monitorRecords.value.length) return
  const queryId = String(deviceId || '').trim()
  if (!queryId) {
    querySelectionError.value = ''
    return
  }
  const record = monitorRecords.value.find((item) => String(item.id) === queryId)
  if (!record) {
    if (cameraState.value.stream) await controller.stop()
    selectedRecordId.value = null
    querySelectionError.value = '该档案可能已停用、删除、转移到其他工作站，或不属于当前工作站。请从监控设备列表重新选择。'
    return
  }
  querySelectionError.value = ''
  if (record.id === selectedRecordId.value) return
  void selectMonitorRecord(record)
})

const toggleRecording = async () => {
  if (!recordingSupported || recordingActionLoading.value) return
  recordingActionLoading.value = true
  const startingRecording = !cameraState.value.recording
  if (startingRecording) {
    activeRecordingOwner.value = selectedRecord.value
      ? { id: selectedRecord.value.id, name: selectedRecord.value.name, code: selectedRecord.value.code }
      : null
  }
  try {
    if (cameraState.value.recording) await controller.stopRecording()
    else await controller.startRecording()
  } catch (error) {
    if (startingRecording && !cameraState.value.recording) activeRecordingOwner.value = null
    ElMessage.error(cameraState.value.error || error?.message || '录像操作失败')
  } finally {
    recordingActionLoading.value = false
  }
}

const timestampedFilename = (prefix, extension) => {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  const timestamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  return `${prefix}-${timestamp}.${extension}`
}

const triggerDownload = (url, filename) => {
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()
}

const revokeTemporaryUrl = (url) => {
  if (!url || !temporaryDownloadUrls.delete(url)) return
  try { URL.revokeObjectURL(url) } catch (error) { console.warn('临时下载地址释放失败', error) }
}

const scheduleTemporaryUrlRelease = (url) => {
  if (!url) return
  temporaryDownloadUrls.add(url)
  const timer = window.setTimeout(() => {
    temporaryDownloadTimers.delete(timer)
    revokeTemporaryUrl(url)
  }, 1000)
  temporaryDownloadTimers.add(timer)
}

const clearTemporaryDownloads = () => {
  for (const timer of temporaryDownloadTimers) window.clearTimeout(timer)
  temporaryDownloadTimers.clear()
  for (const url of [...temporaryDownloadUrls]) revokeTemporaryUrl(url)
}

const captureFrame = async () => {
  const video = videoRef.value
  if (!video?.videoWidth || !video?.videoHeight) {
    ElMessage.warning('画面尚未准备完成，请稍后再截图')
    return
  }
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const context = canvas.getContext('2d')
  if (!context) {
    ElMessage.error('当前浏览器无法生成截图')
    return
  }
  if (mirrored.value) {
    context.translate(canvas.width, 0)
    context.scale(-1, 1)
  }
  context.drawImage(video, 0, 0, canvas.width, canvas.height)
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    ElMessage.error('截图生成失败')
    return
  }
  let temporaryUrl = ''
  try {
    temporaryUrl = URL.createObjectURL(blob)
    triggerDownload(temporaryUrl, timestampedFilename(`${selectedRecord.value?.name || '工业监控'}截图`, 'png'))
  } catch (error) {
    ElMessage.error(error?.message || '截图下载失败')
  } finally {
    if (temporaryUrl) scheduleTemporaryUrlRelease(temporaryUrl)
  }
}

const downloadLatestRecording = () => {
  const result = cameraState.value.recordingResult
  if (!result) return
  let url = result.url
  let temporaryUrl = ''
  try {
    if (!url && result.blob) {
      temporaryUrl = URL.createObjectURL(result.blob)
      url = temporaryUrl
    }
    if (!url) throw new Error('录像下载地址不可用')
    triggerDownload(url, timestampedFilename(`${recordingResultOwner.value?.name || '工业监控'}录像`, result.extension || 'webm'))
  } catch (error) {
    ElMessage.error(error?.message || '录像下载失败')
  } finally {
    if (temporaryUrl) scheduleTemporaryUrlRelease(temporaryUrl)
  }
}

const clearLatestRecording = () => {
  controller.releaseRecording()
  recordingResultOwner.value = null
  ElMessage.success('最新录像已清除')
}

const toggleFullscreen = async () => {
  try {
    const activeElement = document.fullscreenElement || document.webkitFullscreenElement
    if (activeElement) {
      const exitFullscreen = document.exitFullscreen || document.webkitExitFullscreen
      if (!exitFullscreen) throw new Error('当前浏览器无法退出全屏')
      await exitFullscreen.call(document)
      return
    }
    const requestFullscreen = stageRef.value?.requestFullscreen || stageRef.value?.webkitRequestFullscreen
    if (!requestFullscreen) throw new Error('当前浏览器不支持全屏显示')
    await requestFullscreen.call(stageRef.value)
  } catch (error) {
    ElMessage.warning(error?.message || '无法进入全屏')
  }
}

const syncFullscreenState = () => {
  const activeElement = document.fullscreenElement || document.webkitFullscreenElement
  fullscreenActive.value = activeElement === stageRef.value
}

onMounted(() => {
  unsubscribe = controller.subscribe(applySnapshot)
  document.addEventListener('fullscreenchange', syncFullscreenState)
  document.addEventListener('webkitfullscreenchange', syncFullscreenState)
  if (cameraSupported) void controller.enumerateDevices()
  void loadMonitorRecords()
})

onBeforeUnmount(() => {
  if (unsubscribe) unsubscribe()
  unsubscribe = null
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  document.removeEventListener('webkitfullscreenchange', syncFullscreenState)
  clearTemporaryDownloads()
  void controller.dispose()
})
</script>
