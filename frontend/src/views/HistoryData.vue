<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">数据中心</el-tag><h1>历史数据</h1><p>查询设备遥测数据，验证模拟上报是否写入平台。</p></div>
      <div class="module-actions"><el-input v-model="query.deviceName" placeholder="设备名称" clearable class="search-input" /><el-select v-model="query.metric" clearable placeholder="指标" style="width: 140px"><el-option label="temperature" value="temperature" /><el-option label="humidity" value="humidity" /></el-select><el-button type="primary" @click="load">查询</el-button></div>
    </section>
    <el-row :gutter="18">
      <el-col :xs="24" :lg="9"><el-card class="blueprint-card" shadow="never"><h3>数据摘要</h3><div class="big-number">{{ rows.length }}</div><p>当前查询结果条数</p><div class="telemetry-strip"><span v-for="item in rows.slice(0, 5)" :key="item.id">{{ item.value }}{{ item.unit }}</span></div></el-card></el-col>
      <el-col :xs="24" :lg="15"><el-card class="table-card" shadow="never"><el-table :data="filteredRows" stripe><el-table-column prop="deviceName" label="设备" min-width="190" /><el-table-column prop="metric" label="指标" width="130" /><el-table-column label="数值" width="120"><template #default="{ row }"><b>{{ row.value }}</b> {{ row.unit }}</template></el-table-column><el-table-column prop="time" label="上报时间" width="190" /></el-table></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { resourceApi } from '../api/platform'
import { quietError } from '../api/http'
import { fallbackRows } from '../utils/fallback'
import { clone, normalizeList } from '../utils/data'
const rows = ref([])
const query = reactive({ deviceName: '', metric: '' })
const filteredRows = computed(() => rows.value.filter((row) => (!query.deviceName || row.deviceName?.includes(query.deviceName)) && (!query.metric || row.metric === query.metric)))
const load = async () => { try { const payload = await resourceApi.list('historicalData', query); rows.value = normalizeList(payload) } catch (error) { quietError(error, '数据服务暂不可用，当前为演示数据'); rows.value = clone(fallbackRows.historicalData) } }
onMounted(load)
</script>
