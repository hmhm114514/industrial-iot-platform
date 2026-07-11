<template>
  <el-container class="shell">
    <el-aside :width="collapsed ? '72px' : '252px'" class="sidebar">
      <div class="brand" :class="{ collapsed }">
        <div class="brand-mark">II</div>
        <div v-if="!collapsed">
          <strong>工业物联平台</strong>
          <span>Industrial IoT</span>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          router
          background-color="transparent"
          text-color="rgba(223,237,255,.72)"
          active-text-color="#ffffff"
        >
          <template v-for="item in visibleMenus" :key="item.title">
            <el-sub-menu v-if="item.children" :index="item.title">
              <template #title>
                <el-icon><component :is="Icons[item.icon] || Icons.Menu" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                <span>{{ child.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="Icons[item.icon] || Icons.Menu" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button :icon="collapsed ? Icons.Expand : Icons.Fold" circle @click="collapsed = !collapsed" />
          <div>
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item v-for="item in trail" :key="item">{{ item }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-kicker">{{ today }} · 设备接入、数据治理、规则告警一体化</div>
          </div>
        </div>
        <div class="topbar-actions">
          <el-switch
            v-model="dark"
            inline-prompt
            active-text="夜"
            inactive-text="日"
            @change="applyTheme"
          />
          <el-tag effect="dark" type="success" round>平台服务</el-tag>
          <el-popover placement="bottom-end" width="320" trigger="click">
            <template #reference>
              <el-badge :value="unreadAlarmCount" :hidden="!unreadAlarmCount" :max="99">
                <el-button class="message-button" :icon="Icons.Bell" circle />
              </el-badge>
            </template>
            <div class="message-popover">
              <div class="message-head">
                <strong>告警消息</strong>
                <el-button v-if="unreadAlarmCount" link type="primary" @click="markAlarmsRead">全部已读</el-button>
              </div>
              <div v-if="highAlarms.length" class="message-list">
                <button v-for="alarm in highAlarms" :key="alarm.id" class="message-item" @click="openAlarmPage">
                  <span>{{ alarm.title || alarm.name || '高等级告警' }}</span>
                  <small>{{ alarm.deviceName || '未知设备' }} · {{ alarm.time || alarm.createdAt || '-' }}</small>
                </button>
              </div>
              <el-empty v-else description="暂无高等级告警" :image-size="72" />
            </div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <div class="user-chip">
              <el-avatar :size="32">{{ avatarText }}</el-avatar>
              <span>{{ accountLabel }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="identity">当前身份：{{ userInfo.roleName || userInfo.role || '平台管理员' }}</el-dropdown-item>
                <el-dropdown-item divided command="logout">切换账号 / 退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { filterMenusByRole, findMenuTrail } from '../config/menu'
import { resourceApi } from '../api/platform'
import { closeRealtime, connectRealtime } from '../api/realtime'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const dark = ref(localStorage.getItem('iot_theme') === 'dark')
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const readUser = () => {
  try { return JSON.parse(localStorage.getItem('iot_user') || '{}') } catch { return {} }
}
const userInfo = ref(readUser())
const highAlarms = ref([])
const readAlarmIds = ref(JSON.parse(localStorage.getItem('iot_read_alarm_ids') || '[]'))
const notifiedAlarmIds = ref(JSON.parse(localStorage.getItem('iot_notified_alarm_ids') || '[]'))
let alarmTimer

const visibleMenus = computed(() => filterMenusByRole(undefined, userInfo.value))
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录用户')
const userRole = computed(() => userInfo.value.roleName || userInfo.value.role || '平台管理员')
const accountLabel = computed(() => `${displayName.value}${userInfo.value.username ? ` / ${userInfo.value.username}` : ''} · ${userRole.value}`)
const avatarText = computed(() => (displayName.value || 'U').slice(0, 1).toUpperCase())
const trail = computed(() => findMenuTrail(route.path, visibleMenus.value))
const unreadAlarmCount = computed(() => highAlarms.value.filter((alarm) => !readAlarmIds.value.includes(alarm.id)).length)

const applyTheme = () => {
  document.body.classList.toggle('dark', dark.value)
  localStorage.setItem('iot_theme', dark.value ? 'dark' : 'light')
}

const handleCommand = (command) => {
  if (command === 'identity') {
    ElMessage.info(`${userInfo.value.username || '当前用户'} · ${userRole.value}`)
    return
  }
  if (command === 'logout') {
    localStorage.removeItem('iot_token')
    localStorage.removeItem('iot_user')
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

const persistAlarmState = () => {
  localStorage.setItem('iot_read_alarm_ids', JSON.stringify(readAlarmIds.value))
  localStorage.setItem('iot_notified_alarm_ids', JSON.stringify(notifiedAlarmIds.value))
}

const isHighAlarm = (alarm) => {
  const level = String(alarm.level || alarm.alarmLevel || '').toUpperCase()
  const status = String(alarm.status || '').toUpperCase()
  return ['高', 'HIGH'].includes(level) && !['已处理', 'CLOSED', 'CLOSE'].includes(status)
}

const loadAlarmMessages = async () => {
  try {
    const rows = await resourceApi.list('historicalAlarm')
    highAlarms.value = rows.filter(isHighAlarm).sort((a, b) => new Date(b.createdAt || b.time || 0) - new Date(a.createdAt || a.time || 0))
    highAlarms.value
      .filter((alarm) => !notifiedAlarmIds.value.includes(alarm.id))
      .forEach((alarm) => {
        ElMessage({
          message: `高等级告警：${alarm.deviceName || '未知设备'} - ${alarm.title || alarm.name || alarm.value || '触发高等级告警'}`,
          type: 'error',
          showClose: true,
          grouping: true,
          duration: 10000
        })
        notifiedAlarmIds.value.push(alarm.id)
      })
    readAlarmIds.value = readAlarmIds.value.filter((id) => highAlarms.value.some((alarm) => alarm.id === id))
    notifiedAlarmIds.value = notifiedAlarmIds.value.filter((id) => highAlarms.value.some((alarm) => alarm.id === id))
    persistAlarmState()
  } catch {
    highAlarms.value = []
  }
}

const markAlarmsRead = () => {
  readAlarmIds.value = highAlarms.value.map((alarm) => alarm.id)
  persistAlarmState()
}

const openAlarmPage = () => {
  markAlarmsRead()
  router.push('/data/alarms')
}

const handleRealtime = (event) => {
  if (['telemetry', 'alarm'].includes(event.detail?.type)) loadAlarmMessages()
}

onMounted(() => {
  applyTheme()
  connectRealtime()
  window.addEventListener('iot-realtime', handleRealtime)
  loadAlarmMessages()
  alarmTimer = window.setInterval(loadAlarmMessages, 10000)
})
onBeforeUnmount(() => {
  window.removeEventListener('iot-realtime', handleRealtime)
  window.clearInterval(alarmTimer)
  closeRealtime()
})
</script>
