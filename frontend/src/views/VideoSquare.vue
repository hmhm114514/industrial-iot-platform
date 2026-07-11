<template>
  <div class="page-stack">
    <section class="module-head">
      <div>
        <el-tag effect="dark" class="hero-tag">视频中心</el-tag>
        <h1>视频广场</h1>
      </div>
      <div class="module-actions">
        <el-segmented v-model="split" :options="[1, 4, 9]" />
        <el-button type="primary" :loading="loading" @click="load">刷新播放地址</el-button>
      </div>
    </section>

    <el-alert
      v-if="apiError"
      title="视频数据服务暂不可用，请检查后端接口或稍后重试"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-empty v-if="!loading && !apiError && !channelCards.length" description="暂无国标视频设备，请先在国标设备中绑定通道" />

    <div v-if="loading || channelCards.length" :class="['video-wall', `grid-${split}`]" v-loading="loading">
      <div v-for="i in split" :key="i" :class="['player-tile', { offline: tileAt(i)?.offline, empty: !tileAt(i) }]">
        <template v-if="tileAt(i)">
          <el-tag class="video-status" :type="tileAt(i).offline ? 'info' : tileAt(i).playUrl ? 'success' : 'warning'" effect="dark">
            {{ tileAt(i).statusText }}
          </el-tag>
          <div class="play-icon">{{ tileAt(i).playUrl && !tileAt(i).offline ? '▶' : '—' }}</div>
          <strong>{{ tileAt(i).name }}</strong>
          <span>{{ tileAt(i).location }} / {{ tileAt(i).channel }}</span>
          <small>{{ tileAt(i).message }}</small>
          <div class="video-meta">
            <em>{{ tileAt(i).streamName }}</em>
            <b v-if="tileAt(i).alarmCount">{{ tileAt(i).alarmCount }} 条视频告警</b>
          </div>
          <div v-if="tileAt(i).playUrl" class="video-actions">
            <el-button size="small" @click="copyPlayUrl(tileAt(i).playUrl)">复制播放地址</el-button>
            <el-button size="small" type="primary" plain @click="openPlayUrl(tileAt(i).playUrl)">打开地址</el-button>
          </div>
        </template>
        <template v-else>
          <div class="play-icon muted">—</div>
          <strong>未绑定通道</strong>
          <span>当前分屏暂无对应视频设备</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { resourceApi } from '../api/platform'

const split = ref(4)
const loading = ref(false)
const apiError = ref(false)
const devices = ref([])
const streams = ref([])
const videoAlarms = ref([])

const isOnline = (item = {}) => ['ONLINE', 'online', 'healthy', 'RUNNING', 'running'].includes(item.status)
const isClosedAlarm = (item = {}) => ['CLOSED', 'closed', '已处理'].includes(item.status)

const channelCards = computed(() => devices.value.map((device) => {
  const deviceStreams = streams.value.filter((stream) => String(stream.videoDeviceId) === String(device.id))
  const stream = deviceStreams.find((item) => item.playUrl) || deviceStreams[0] || {}
  const alarms = videoAlarms.value.filter((alarm) => String(alarm.videoDeviceId) === String(device.id) && !isClosedAlarm(alarm))
  const offline = !isOnline(device)
  const playUrl = stream.playUrl || device.streamUrl || ''
  return {
    id: device.id,
    name: device.name || `视频设备#${device.id}`,
    channel: device.channel || device.channelNo || device.code || '未配置通道编码',
    location: device.location || '未配置位置',
    offline,
    playUrl,
    streamName: stream.name || (playUrl ? '设备原始播放地址' : '未绑定拉流代理'),
    alarmCount: alarms.length,
    statusText: offline ? '离线' : playUrl ? '已配置播放源' : '等待流代理',
    message: offline ? '设备离线，暂不可播放' : playUrl ? '播放源来自接口配置，请使用地址按钮打开' : '未配置播放源 / 等待流代理接入'
  }
}))

const tileAt = (index) => channelCards.value[index - 1]

const load = async () => {
  loading.value = true
  try {
    const [deviceRows, streamRows, alarmRows] = await Promise.all([
      resourceApi.list('gbDevice'),
      resourceApi.list('streamProxy'),
      resourceApi.list('videoAlarm')
    ])
    devices.value = Array.isArray(deviceRows) ? deviceRows : []
    streams.value = Array.isArray(streamRows) ? streamRows : []
    videoAlarms.value = Array.isArray(alarmRows) ? alarmRows : []
    apiError.value = false
  } catch (error) {
    console.warn('视频数据服务暂不可用', error)
    devices.value = []
    streams.value = []
    videoAlarms.value = []
    apiError.value = true
  } finally {
    loading.value = false
  }
}

const copyPlayUrl = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('播放地址已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制地址')
  }
}

const openPlayUrl = (url) => window.open(url, '_blank', 'noopener,noreferrer')

onMounted(load)
</script>
