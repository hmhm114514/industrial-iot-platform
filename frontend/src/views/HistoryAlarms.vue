<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">告警闭环</el-tag><h1>历史告警</h1><p>查询规则触发的告警，并完成处置闭环。</p></div>
      <div class="module-actions"><el-input v-model="keyword" placeholder="设备 / 告警标题" clearable class="search-input" /><el-button type="primary" @click="load">查询</el-button></div>
    </section>
    <el-card class="table-card" shadow="never">
      <el-table :data="filteredRows" stripe>
        <el-table-column prop="title" label="告警标题" min-width="180" />
        <el-table-column prop="deviceName" label="设备" min-width="190" />
        <el-table-column prop="level" label="等级" width="90"><template #default="{ row }"><el-tag :type="row.level === '高' ? 'danger' : 'warning'" round>{{ row.level }}</el-tag></template></el-table-column>
        <el-table-column prop="value" label="触发值" width="130" />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === '已处理' ? 'success' : 'danger'" round>{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="handler" label="处理人" width="120" />
        <el-table-column prop="time" label="时间" width="190" />
        <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button link type="primary" :disabled="row.status === '已处理'" @click="openHandle(row)">处置</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" title="告警处置" width="520px"><el-form :model="form" label-width="90px"><el-form-item label="告警"><el-input v-model="form.title" disabled /></el-form-item><el-form-item label="处置意见"><el-input v-model="form.remark" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="submitHandle">确认处置</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { alarmApi, resourceApi } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'
const rows = ref([])
const keyword = ref('')
const visible = ref(false)
const currentId = ref(null)
const form = reactive({ title: '', remark: '已现场复核并恢复正常。' })
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || JSON.stringify(row).includes(keyword.value)))
const load = async () => { try { const payload = await resourceApi.list('historicalAlarm', { keyword: keyword.value }); rows.value = normalizeList(payload) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); rows.value = clone(fallbackRows.historicalAlarm) } }
const openHandle = (row) => { currentId.value = row.id; form.title = row.title; form.remark = '已现场复核并恢复正常。'; visible.value = true }
const submitHandle = async () => { const user = JSON.parse(localStorage.getItem('iot_user') || '{}'); try { await alarmApi.handle(currentId.value, { remark: form.remark, handler: user.realName || user.username || 'admin' }); ElMessage.success('告警已处置'); await load() } catch (error) { quietError(error, '告警处置失败，后端数据未变更') } visible.value = false }
onMounted(load)
</script>
