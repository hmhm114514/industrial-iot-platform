<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">任务中心</el-tag><h1>任务中心</h1><p>管理定时任务，支持启停、查看执行日志。</p></div>
      <div class="module-actions"><el-input v-model="keyword" placeholder="任务名称" clearable class="search-input" /><el-button @click="load">查询</el-button><el-button type="primary" @click="openCreate">新增任务</el-button></div>
    </section>
    <el-card class="table-card" shadow="never">
      <el-table :data="filteredRows" stripe>
        <el-table-column prop="name" label="任务名称" min-width="190" />
        <el-table-column prop="cron" label="Cron" width="160" />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'running' ? 'success' : 'info'" round>{{ row.status === 'running' ? '运行中' : '已停止' }}</el-tag></template></el-table-column>
        <el-table-column prop="lastRun" label="最近执行" width="190" />
        <el-table-column prop="successRate" label="成功率" width="100" />
        <el-table-column label="启停" width="90"><template #default="{ row }"><el-switch :model-value="row.status === 'running'" @change="(v) => toggle(row, v)" /></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="success" @click="openLogs(row)">日志</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="editingId ? '编辑任务' : '新增任务'" width="520px"><el-form :model="form" label-width="90px"><el-form-item label="任务名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="Cron"><el-input v-model="form.cron" /></el-form-item><el-form-item label="状态"><el-switch v-model="form.running" active-text="运行" inactive-text="停止" /></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
    <el-drawer v-model="drawer" title="任务执行日志" size="520px"><el-timeline><el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.time" :type="log.status === '成功' ? 'success' : 'danger'"><b>{{ log.taskName }}</b><p>{{ log.message }}</p></el-timeline-item></el-timeline></el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resourceApi, taskApi } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'
const rows = ref([])
const logs = ref([])
const keyword = ref('')
const visible = ref(false)
const drawer = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', cron: '0 */5 * * * ?', running: true })
const filteredRows = computed(() => rows.value.filter((row) => !keyword.value || row.name?.includes(keyword.value)))
const load = async () => { try { const payload = await resourceApi.list('task', { keyword: keyword.value }); rows.value = normalizeList(payload) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); rows.value = clone(fallbackRows.task) } }
const openCreate = () => { editingId.value = null; Object.assign(form, { name: '', cron: '0 */5 * * * ?', running: true }); visible.value = true }
const openEdit = (row) => { editingId.value = row.id; Object.assign(form, { name: row.name, cron: row.cron, running: row.status === 'running' }); visible.value = true }
const save = async () => { const payload = { name: form.name, cron: form.cron, running: form.running, status: form.running ? 'RUNNING' : 'STOPPED' }; try { editingId.value ? await resourceApi.update('task', editingId.value, payload) : await resourceApi.create('task', payload); ElMessage.success('任务已保存'); await load() } catch (error) { quietError(error, '任务保存失败，后端数据未变更') } visible.value = false }
const toggle = async (row, running) => { try { running ? await taskApi.start(row.id) : await taskApi.stop(row.id); ElMessage.success(running ? '任务已启动' : '任务已停止'); await load() } catch (error) { quietError(error, '任务启停失败，后端数据未变更') } }
const remove = async (row) => { await ElMessageBox.confirm(`确认删除任务“${row.name}”？`, '提示', { type: 'warning' }); try { await resourceApi.remove('task', row.id); await load() } catch (error) { quietError(error, '任务删除失败，后端数据未变更') } }
const openLogs = async (row) => { drawer.value = true; try { const payload = await taskApi.logs({ taskId: row.id }); logs.value = normalizeList(payload) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); logs.value = clone(fallbackRows.taskLog) } }
onMounted(load)
</script>
