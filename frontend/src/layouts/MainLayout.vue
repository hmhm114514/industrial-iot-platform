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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { filterMenusByRole, findMenuTrail } from '../config/menu'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const dark = ref(localStorage.getItem('iot_theme') === 'dark')
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const readUser = () => {
  try { return JSON.parse(localStorage.getItem('iot_user') || '{}') } catch { return {} }
}
const userInfo = ref(readUser())

const visibleMenus = computed(() => filterMenusByRole(undefined, userInfo.value))
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录用户')
const userRole = computed(() => userInfo.value.roleName || userInfo.value.role || '平台管理员')
const accountLabel = computed(() => `${displayName.value}${userInfo.value.username ? ` / ${userInfo.value.username}` : ''} · ${userRole.value}`)
const avatarText = computed(() => (displayName.value || 'U').slice(0, 1).toUpperCase())
const trail = computed(() => findMenuTrail(route.path, visibleMenus.value))

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

onMounted(applyTheme)
</script>
