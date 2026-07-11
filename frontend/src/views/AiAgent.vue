<template>
  <div class="ai-agent-page page-stack">
    <section class="module-head ai-hero">
      <div>
        <el-tag effect="dark" class="hero-tag">智能辅助</el-tag>
        <h1>AI 智能体</h1>
      </div>
      <div class="module-actions">
        <el-button :loading="loadingAgents" @click="loadAgents">刷新能力</el-button>
      </div>
    </section>

    <el-alert
      v-if="agentFallback"
      title="平台服务暂不可用，当前为演示能力"
      type="warning"
      show-icon
      :closable="false"
      class="ai-alert"
    />

    <section class="ai-layout">
      <aside class="agent-panel">
        <div class="panel-title">
          <span>智能体能力</span>
          <strong>{{ agents.length }}</strong>
        </div>
        <button
          v-for="agent in agents"
          :key="agent.code || agent.id"
          type="button"
          class="agent-card"
          :class="{ active: selectedAgent?.code === agent.code }"
          @click="selectAgent(agent)"
        >
          <span>{{ agent.scenario || '平台智能辅助' }}</span>
          <strong>{{ agent.name || agent.title }}</strong>
          <em>{{ agent.description || agent.remark || '根据平台运行数据生成处置建议' }}</em>
        </button>
      </aside>

      <main class="chat-panel">
        <section class="config-card">
          <div class="config-head">
            <div>
              <strong>大模型接入配置</strong>
              <span>默认使用课程接口文档中的 MaaS 兼容地址；API Key 可在此粘贴，也可留空使用本机示例配置。</span>
            </div>
            <el-button :loading="testingModels" @click="testModels">测试连接</el-button>
          </div>
          <div class="config-grid">
            <el-input v-model="aiConfig.baseUrl" placeholder="接入地址" />
            <el-select v-model="aiConfig.model" filterable allow-create default-first-option placeholder="选择或输入模型">
              <el-option v-for="model in modelOptions" :key="model" :label="model" :value="model" />
            </el-select>
            <el-input v-model="aiConfig.apiKey" type="password" show-password placeholder="API Key（可留空使用本机示例配置）" />
          </div>
          <p class="config-tip">{{ modelNotice || '未填写 API Key 时，后端会优先尝试本机示例配置；若不可用则自动返回本地演示建议。' }}</p>
        </section>

        <div class="chat-head">
          <div>
            <span>当前能力</span>
            <strong>{{ selectedAgent?.name || '智能助手' }}</strong>
          </div>
          <el-tag :type="lastSource === 'LIVE' ? 'success' : 'info'" effect="plain">
            {{ sourceLabel }}
          </el-tag>
        </div>

        <div class="quick-grid">
          <button v-for="item in quickQuestions" :key="item" type="button" @click="askQuick(item)">
            {{ item }}
          </button>
        </div>

        <div class="reply-box">
          <div v-if="thinking" class="thinking">
            <span class="pulse" />正在生成建议，通常需要数秒...
          </div>
          <template v-else>
            <div class="reply-meta">
              <span>{{ lastNotice || '请选择智能体并输入问题' }}</span>
            </div>
            <div class="markdown-body" v-html="renderedReply" />
          </template>
        </div>

        <div class="input-card">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入需要分析的设备运行问题，例如：注塑机温度持续偏高，应该如何处置？"
          />
          <div class="input-actions">
            <span>建议描述设备、告警、最近数据或现场现象，便于生成更贴合的建议。</span>
            <el-button type="primary" :loading="thinking" @click="sendQuestion">生成建议</el-button>
          </div>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiAgentApi } from '../api/platform'
import { quietError } from '../api/http'

const fallbackAgents = [
  { id: 'demo-alarm', code: 'alarm-explain', name: '告警解释助手', scenario: '告警解释', description: '解释告警触发原因，给出处置步骤。' },
  { id: 'demo-inspection', code: 'inspection-plan', name: '巡检计划助手', scenario: '巡检计划', description: '根据设备状态生成巡检重点和记录项。' },
  { id: 'demo-status', code: 'device-status', name: '设备状态分析助手', scenario: '状态分析', description: '汇总在线、遥测、告警和任务状态。' }
]

const quickQuestions = [
  '解释当前高温告警的可能原因',
  '生成今日设备巡检计划',
  '总结设备在线状态和风险点',
  '优化温度阈值规则配置'
]

const agents = ref([])
const selectedAgent = ref(null)
const question = ref('')
const reply = ref('')
const lastNotice = ref('')
const lastSource = ref('DEMO')
const loadingAgents = ref(false)
const thinking = ref(false)
const agentFallback = ref(false)
const testingModels = ref(false)
const modelNotice = ref('')
const modelOptions = ref(['deepseek-v4-flash', 'deepseek-v4-pro', 'kimi-k2.6', 'glm-5.1', 'qwen3.5'])
const aiConfig = ref({
  baseUrl: localStorage.getItem('iot_ai_base_url') || 'https://maas.icompify.com:32788/v1',
  model: localStorage.getItem('iot_ai_model') || 'deepseek-v4-flash',
  apiKey: ''
})

const sourceLabel = computed(() => lastSource.value === 'LIVE' ? '智能服务生成' : '本地演示建议')
const renderedReply = computed(() => renderMarkdown(reply.value || '可以询问高温告警原因、巡检计划、设备状态分析或规则阈值优化建议。'))

const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, (char) => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  "'": '&#39;',
  '"': '&quot;'
}[char]))

const renderInline = (text) => escapeHtml(text)
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  .replace(/\*([^*]+)\*/g, '<em>$1</em>')

const renderMarkdown = (text) => {
  const blocks = []
  const tokenized = String(text || '').replace(/```([\s\S]*?)```/g, (_, code) => {
    const token = `@@CODE_${blocks.length}@@`
    blocks.push(`<pre><code>${escapeHtml(code.trim())}</code></pre>`)
    return token
  })
  const lines = tokenized.split(/\r?\n/)
  const html = []
  let list = null
  const closeList = () => { if (list) { html.push(`</${list}>`); list = null } }
  lines.forEach((line) => {
    if (!line.trim()) { closeList(); return }
    if (/^@@CODE_\d+@@$/.test(line.trim())) { closeList(); html.push(line.trim()); return }
    const heading = line.match(/^(#{1,3})\s+(.+)$/)
    if (heading) { closeList(); html.push(`<h${heading[1].length}>${renderInline(heading[2])}</h${heading[1].length}>`); return }
    const unordered = line.match(/^[-*]\s+(.+)$/)
    if (unordered) { if (list !== 'ul') { closeList(); list = 'ul'; html.push('<ul>') } html.push(`<li>${renderInline(unordered[1])}</li>`); return }
    const ordered = line.match(/^\d+\.\s+(.+)$/)
    if (ordered) { if (list !== 'ol') { closeList(); list = 'ol'; html.push('<ol>') } html.push(`<li>${renderInline(ordered[1])}</li>`); return }
    closeList()
    html.push(`<p>${renderInline(line)}</p>`)
  })
  closeList()
  return html.join('').replace(/@@CODE_(\d+)@@/g, (_, index) => blocks[Number(index)] || '')
}

const selectAgent = (agent) => {
  selectedAgent.value = agent
  lastNotice.value = `${agent.name || '智能体'} 已就绪`
}

const loadAgents = async () => {
  loadingAgents.value = true
  try {
    const rows = await aiAgentApi.list()
    agents.value = Array.isArray(rows) && rows.length ? rows : fallbackAgents
    agentFallback.value = !Array.isArray(rows) || !rows.length
  } catch (error) {
    quietError(error, '平台服务暂不可用，当前为演示能力')
    agents.value = fallbackAgents
    agentFallback.value = true
  } finally {
    loadingAgents.value = false
    if (!selectedAgent.value) selectAgent(agents.value[0])
  }
}

const sendQuestion = async () => {
  const text = question.value.trim()
  if (!text) {
    ElMessage.info('请先输入需要分析的问题')
    return
  }
  thinking.value = true
  try {
    localStorage.setItem('iot_ai_base_url', aiConfig.value.baseUrl)
    localStorage.setItem('iot_ai_model', aiConfig.value.model)
    const result = await aiAgentApi.chat({
      agentCode: selectedAgent.value?.code,
      message: text,
      context: `当前智能体：${selectedAgent.value?.name || '智能助手'}；场景：${selectedAgent.value?.scenario || '平台运维'}`,
      baseUrl: aiConfig.value.baseUrl,
      model: aiConfig.value.model,
      apiKey: aiConfig.value.apiKey
    })
    reply.value = result?.reply || '暂未生成建议，请稍后重试。'
    lastSource.value = result?.source || 'DEMO'
    lastNotice.value = result?.notice || (lastSource.value === 'LIVE' ? '智能服务已生成建议' : '已切换为本地演示建议')
  } catch (error) {
    quietError(error, '平台服务暂不可用，当前为本地演示建议')
    lastSource.value = 'DEMO'
    lastNotice.value = '平台服务暂不可用，当前为本地演示建议'
    reply.value = '本地演示建议：先确认设备在线状态和最近遥测趋势，再核对告警等级、规则阈值与现场处置记录；如异常持续，建议安排巡检并保留处理结论。'
  } finally {
    thinking.value = false
  }
}

const askQuick = (item) => {
  question.value = item
  sendQuestion()
}

const testModels = async () => {
  testingModels.value = true
  try {
    localStorage.setItem('iot_ai_base_url', aiConfig.value.baseUrl)
    const result = await aiAgentApi.models({
      baseUrl: aiConfig.value.baseUrl,
      apiKey: aiConfig.value.apiKey
    })
    if (Array.isArray(result?.models) && result.models.length) {
      modelOptions.value = result.models
      if (!modelOptions.value.includes(aiConfig.value.model)) aiConfig.value.model = modelOptions.value[0]
    }
    modelNotice.value = result?.notice || '模型连接测试完成'
    lastSource.value = result?.source || lastSource.value
    ElMessage.success(modelNotice.value)
  } catch (error) {
    quietError(error, '平台服务暂不可用，当前保留默认模型')
    modelNotice.value = '平台服务暂不可用，当前保留默认模型'
  } finally {
    testingModels.value = false
  }
}

onMounted(loadAgents)
</script>

<style scoped>
.ai-agent-page {
  --ai-blue: #2559d6;
  --ai-violet: #7c3aed;
  --ai-ink: #17233c;
}

.ai-hero {
  background:
    radial-gradient(circle at 85% 12%, rgba(124, 58, 237, .18), transparent 30%),
    linear-gradient(135deg, rgba(255,255,255,.96), rgba(235,240,255,.92));
}

.ai-alert {
  border-radius: 16px;
}

.ai-layout {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 18px;
}

.agent-panel,
.chat-panel {
  border: 1px solid rgba(37, 89, 214, .12);
  border-radius: 28px;
  background: rgba(255,255,255,.92);
  box-shadow: 0 24px 70px rgba(25, 45, 95, .12);
}

.agent-panel {
  padding: 18px;
}

.panel-title,
.chat-head,
.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.panel-title {
  margin-bottom: 14px;
  color: rgba(23,35,60,.66);
}

.panel-title strong {
  color: var(--ai-violet);
  font-size: 28px;
}

.agent-card {
  width: 100%;
  margin-bottom: 12px;
  padding: 16px;
  border: 1px solid rgba(37,89,214,.12);
  border-radius: 20px;
  background: linear-gradient(135deg, #fff, #f7f9ff);
  text-align: left;
  cursor: pointer;
  transition: .18s ease;
}

.agent-card:hover,
.agent-card.active {
  transform: translateY(-2px);
  border-color: rgba(124,58,237,.36);
  box-shadow: 0 16px 34px rgba(37,89,214,.14);
}

.agent-card span,
.agent-card strong,
.agent-card em {
  display: block;
}

.agent-card span {
  color: var(--ai-violet);
  font-size: 12px;
  font-weight: 800;
}

.agent-card strong {
  margin: 7px 0;
  color: var(--ai-ink);
  font-size: 17px;
}

.agent-card em {
  color: rgba(23,35,60,.62);
  font-style: normal;
  line-height: 1.6;
}

.chat-panel {
  padding: 22px;
}

.config-card {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid rgba(124,58,237,.14);
  border-radius: 22px;
  background: linear-gradient(135deg, #ffffff, #f7f9ff);
}

.config-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.config-head strong,
.config-head span {
  display: block;
}

.config-head strong {
  color: var(--ai-ink);
  font-size: 17px;
}

.config-head span,
.config-tip {
  color: rgba(23,35,60,.58);
  font-size: 13px;
  line-height: 1.6;
}

.config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(180px, .75fr) minmax(220px, 1fr);
  gap: 10px;
}

.config-tip {
  margin: 10px 0 0;
}

.chat-head {
  margin-bottom: 18px;
}

.chat-head span,
.reply-meta {
  color: rgba(23,35,60,.58);
  font-size: 13px;
}

.chat-head strong {
  display: block;
  margin-top: 4px;
  color: var(--ai-ink);
  font-size: 26px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.quick-grid button {
  min-height: 64px;
  padding: 12px;
  border: 1px solid rgba(37,89,214,.14);
  border-radius: 16px;
  background: #f7f9ff;
  color: var(--ai-ink);
  cursor: pointer;
}

.reply-box {
  min-height: 220px;
  margin-bottom: 16px;
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 92% 8%, rgba(124,58,237,.16), transparent 28%),
    linear-gradient(135deg, #17233c, #223b78);
  color: #fff;
}

.markdown-body { margin-top: 12px; line-height: 1.9; }
.markdown-body :deep(p) { margin: 8px 0; }
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) { margin: 12px 0 8px; color: #fff; line-height: 1.35; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) { margin: 8px 0 8px 20px; padding: 0; }
.markdown-body :deep(li) { margin: 5px 0; }
.markdown-body :deep(code) { padding: 2px 6px; border-radius: 7px; background: rgba(255,255,255,.14); color: #bfffe4; }
.markdown-body :deep(pre) { overflow: auto; padding: 12px; border-radius: 14px; background: rgba(0,0,0,.28); }
.markdown-body :deep(pre code) { padding: 0; background: transparent; }
.markdown-body :deep(strong) { color: #bfffe4; }

.thinking {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 170px;
  font-weight: 800;
}

.pulse {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #8bffcf;
  box-shadow: 0 0 0 0 rgba(139,255,207,.6);
  animation: pulse 1.2s infinite;
}

.input-card {
  padding: 16px;
  border-radius: 22px;
  background: #f7f9ff;
}

.input-actions {
  margin-top: 12px;
  color: rgba(23,35,60,.58);
  font-size: 13px;
}

@keyframes pulse {
  70% { box-shadow: 0 0 0 12px rgba(139,255,207,0); }
  100% { box-shadow: 0 0 0 0 rgba(139,255,207,0); }
}

@media (max-width: 1180px) {
  .ai-layout { grid-template-columns: 1fr; }
  .config-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 640px) {
  .quick-grid { grid-template-columns: 1fr; }
  .input-actions { align-items: stretch; flex-direction: column; }
}
</style>
