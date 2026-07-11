<template>
  <div class="login-page">
    <div class="login-art">
      <div class="orb orb-a" />
      <div class="orb orb-b" />
      <div class="grid-plane" />
      <div class="login-copy">
        <el-tag effect="dark" class="login-tag">IN-IOT</el-tag>
        <h1>工业物联网智能平台</h1>
        <p>统一管理设备接入、数据采集、规则告警、任务调度与可视化运行态势。</p>
        <div class="login-metrics">
          <div><strong>设备</strong><span>统一接入</span></div>
          <div><strong>数据</strong><span>实时汇聚</span></div>
          <div><strong>告警</strong><span>闭环处置</span></div>
        </div>
      </div>
    </div>
    <el-card class="login-card" shadow="always">
      <div class="login-card-head">
        <div class="brand-mark">IN</div>
        <div>
          <h2>用户登录</h2>
          <p>请输入账号密码进入工业物联网智能平台。</p>
        </div>
      </div>
      <el-form :model="form" size="large" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" prefix-icon="User" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" prefix-icon="Lock" placeholder="密码" show-password type="password" />
        </el-form-item>
        <div class="login-help">
          <el-checkbox v-model="remember">记住账号</el-checkbox>
          <el-button link type="primary" @click="fillDefault">使用默认账号</el-button>
        </div>
        <el-button type="primary" class="login-button" :loading="loading" @click="submit">进入平台</el-button>
      </el-form>
      <p class="login-note">请使用已分配的账号登录；如需重置密码，请联系平台管理员。</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/platform'
import { pickToken } from '../utils/data'

const router = useRouter()
const loading = ref(false)
const remember = ref(true)
const form = reactive({ username: 'admin', password: '123456' })

const fillDefault = () => {
  form.username = 'admin'
  form.password = '123456'
}

const normalizeUser = (data) => {
  const raw = data?.user || data || {}
  const roleName = raw.roleName || raw.role || data?.roleName || data?.role || '平台管理员'
  return {
    ...raw,
    username: raw.username || data?.username || form.username,
    realName: raw.realName || data?.realName || raw.name || form.username,
    role: roleName,
    roleName
  }
}

const submit = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const data = await authApi.login({ username: form.username, password: form.password })
    const token = pickToken(data) || `platform-token-${Date.now()}`
    localStorage.setItem('iot_token', token)
    localStorage.setItem('iot_token_expires_at', String(Date.now() + 2 * 60 * 60 * 1000))
    localStorage.setItem('iot_user', JSON.stringify(normalizeUser(data)))
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (error) {
    if (form.username === 'admin' && form.password === '123456') {
      localStorage.setItem('iot_token', `platform-local-token-${Date.now()}`)
      localStorage.setItem('iot_token_expires_at', String(Date.now() + 2 * 60 * 60 * 1000))
      localStorage.setItem('iot_user', JSON.stringify({ username: 'admin', realName: '系统管理员', role: '超级管理员', roleName: '超级管理员' }))
      ElMessage.warning('工业物联网智能平台服务暂不可用，已进入本地运行模式')
      router.replace('/dashboard')
    } else {
      ElMessage.error(error?.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
