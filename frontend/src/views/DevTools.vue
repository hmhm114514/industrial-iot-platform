<template>
  <div class="page-stack dev-tools-page">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">开发工具</el-tag><h1>开发工具</h1></div>
    </section>
    <el-tabs v-model="activeTab" class="tool-tabs">
      <el-tab-pane label="代码生成" name="code">
        <el-row :gutter="18">
          <el-col :xs="24" :lg="8">
            <el-card class="tool-card" shadow="never">
              <el-form :model="codeForm" label-width="90px">
                <el-form-item label="模块名称"><el-input v-model="codeForm.moduleName" /></el-form-item>
                <el-form-item label="实体名称"><el-input v-model="codeForm.entityName" /></el-form-item>
                <el-form-item label="字段列表"><el-input v-model="codeForm.fields" type="textarea" :rows="5" placeholder="name:String,status:String,temperature:Double" /></el-form-item>
                <el-form-item label="模板类型"><el-select v-model="codeForm.template" class="full"><el-option label="Spring 实体" value="entity" /><el-option label="Vue 列表配置" value="vue" /><el-option label="接口封装" value="api" /></el-select></el-form-item>
              </el-form>
              <el-button type="primary" @click="generateCode">生成代码</el-button>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="16"><el-card class="tool-card" shadow="never"><pre class="code-box">{{ codeOutput }}</pre><el-button @click="copy(codeOutput)">复制代码</el-button></el-card></el-col>
        </el-row>
      </el-tab-pane>
      <el-tab-pane label="报文生成" name="message">
        <el-row :gutter="18">
          <el-col :xs="24" :lg="8">
            <el-card class="tool-card" shadow="never">
              <el-form :model="messageForm" label-width="90px">
                <el-form-item label="设备"><el-select v-model="messageForm.deviceId" class="full" filterable><el-option v-for="d in devices" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item>
                <el-form-item label="温度"><el-input-number v-model="messageForm.temperature" class="full" :min="-20" :max="120" /></el-form-item>
                <el-form-item label="湿度"><el-input-number v-model="messageForm.humidity" class="full" :min="0" :max="100" /></el-form-item>
                <el-form-item label="压力"><el-input-number v-model="messageForm.pressure" class="full" :min="0" :max="1000" /></el-form-item>
              </el-form>
              <div class="tool-actions"><el-button @click="generateMessage">生成报文</el-button><el-button type="primary" :loading="sending" @click="sendMessage">模拟上报</el-button></div>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="16"><el-card class="tool-card" shadow="never"><pre class="code-box">{{ messageOutput }}</pre><el-button @click="copy(messageOutput)">复制报文</el-button></el-card></el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { resourceApi, telemetryApi } from '../api/platform'
import { quietError } from '../api/http'

const activeTab = ref('code')
const devices = ref([])
const sending = ref(false)
const codeForm = reactive({ moduleName: '设备巡检', entityName: 'InspectionTask', fields: 'name:String,status:String,owner:String,createdAt:LocalDateTime', template: 'entity' })
const messageForm = reactive({ deviceId: null, temperature: 36.5, humidity: 55, pressure: 101.3 })
const codeOutput = ref('点击“生成代码”后将在这里显示代码片段。')
const messageOutput = ref('点击“生成报文”后将在这里显示设备报文。')

const parseFields = () => codeForm.fields.split(',').map((item) => item.trim()).filter(Boolean).map((item) => {
  const [name, type = 'String'] = item.split(':').map((x) => x.trim())
  return { name, type }
})

const generateCode = () => {
  const fields = parseFields()
  if (codeForm.template === 'entity') {
    codeOutput.value = `@Entity\nclass ${codeForm.entityName} extends BaseEntity {\n${fields.map((f) => `    public ${f.type} ${f.name};`).join('\n')}\n}`
  } else if (codeForm.template === 'vue') {
    codeOutput.value = `columns: [\n${fields.map((f) => `  { prop: '${f.name}', label: '${f.name}', minWidth: 140 }`).join(',\n')}\n],\nfields: [\n${fields.map((f) => `  textField('${f.name}', '${f.name}')`).join(',\n')}\n]`
  } else {
    const endpoint = codeForm.entityName.replace(/[A-Z]/g, (m, i) => `${i ? '-' : ''}${m.toLowerCase()}`)
    codeOutput.value = `export const ${codeForm.entityName[0].toLowerCase()}${codeForm.entityName.slice(1)}Api = {\n  list: (params) => http.get('/${endpoint}', { params }),\n  create: (data) => http.post('/${endpoint}', data),\n  update: (id, data) => http.put('/${endpoint}/' + id, data),\n  remove: (id) => http.delete('/${endpoint}/' + id)\n}`
  }
}

const generateMessage = () => {
  const device = devices.value.find((item) => item.id === messageForm.deviceId)
  messageOutput.value = JSON.stringify({
    deviceId: messageForm.deviceId,
    deviceName: device?.name,
    temperature: messageForm.temperature,
    humidity: messageForm.humidity,
    pressure: messageForm.pressure,
    timestamp: new Date().toISOString(),
    values: { temperature: messageForm.temperature, humidity: messageForm.humidity, pressure: messageForm.pressure }
  }, null, 2)
}

const sendMessage = async () => {
  if (!messageForm.deviceId) return ElMessage.warning('请先选择设备')
  sending.value = true
  try {
    await telemetryApi.simulate({ ...messageForm, timestamp: new Date().toISOString() })
    generateMessage()
    ElMessage.success('报文已上报，可在控制台、历史数据和历史告警中查看联动结果')
  } catch (error) {
    quietError(error, '数据服务暂不可用，报文未写入平台')
  } finally {
    sending.value = false
  }
}

const copy = async (value) => {
  await navigator.clipboard?.writeText(value)
  ElMessage.success('已复制')
}

const loadDevices = async () => {
  try {
    devices.value = await resourceApi.list('device')
    if (devices.value[0]) messageForm.deviceId = devices.value[0].id
    generateMessage()
  } catch (error) {
    quietError(error, '设备数据服务暂不可用')
  }
}

onMounted(() => { generateCode(); loadDevices() })
</script>

<style scoped>
.tool-card { border-radius: 22px; }
.tool-card :deep(.el-card__body) { display: flex; flex-direction: column; gap: 14px; }
.code-box { min-height: 360px; margin: 0; padding: 18px; border-radius: 16px; overflow: auto; color: #dff7ff; background: #07162e; line-height: 1.7; }
.tool-actions { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
