<template>
  <div class="page-stack">
    <section class="module-head">
      <div><el-tag effect="dark" class="hero-tag">视频中心</el-tag><h1>视频广场</h1><p>黑色视频播放器占位，支持 1 / 4 / 9 分屏切换，便于演示视频接入能力。</p></div>
      <div class="module-actions"><el-segmented v-model="split" :options="[1, 4, 9]" /><el-button type="primary">刷新播放地址</el-button></div>
    </section>
    <div :class="['video-wall', `grid-${split}`]">
      <div v-for="i in split" :key="i" class="player-tile">
        <div class="rec-dot" />
        <div class="play-icon">▶</div>
        <strong>{{ cameras[i - 1]?.name || `备用通道 ${i}` }}</strong>
        <span>{{ cameras[i - 1]?.location || '未绑定设备' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const split = ref(4)
const cameras = [
  { name: '厂区门岗球机', location: '北门 / 在线' },
  { name: '一号车间枪机', location: '产线 A / 在线' },
  { name: '仓储冷链摄像头', location: '冷库 / 离线' },
  { name: '园区道路云台', location: '主干道 / 在线' },
  { name: '能源站监控', location: '配电房 / 在线' },
  { name: '装卸区摄像头', location: '月台 / 在线' }
]
</script>
