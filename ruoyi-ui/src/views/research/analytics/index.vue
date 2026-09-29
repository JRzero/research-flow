<template>
  <div v-loading="loading" class="analytics-page">
    <section class="metric-row" aria-label="科研项目运营核心指标">
      <article>
        <div class="metric-label"><el-icon aria-hidden="true"><FolderOpened /></el-icon><span>项目总数</span></div>
        <strong class="rf-tabular">{{ dashboard.totalProjects || 0 }}</strong>
        <p>当前科研项目资产规模</p>
      </article>
      <article>
        <div class="metric-label"><el-icon aria-hidden="true"><Wallet /></el-icon><span>整体预算</span></div>
        <strong class="rf-tabular">¥{{ money(dashboard.totalBudget) }}</strong>
        <p>项目总预算汇总</p>
      </article>
      <article>
        <div class="metric-label"><el-icon aria-hidden="true"><DataLine /></el-icon><span>预算执行率</span></div>
        <strong class="rf-tabular">{{ dashboard.budgetExecutionRate || 0 }}%</strong>
        <p>已发生支出 / 总预算</p>
      </article>
      <article>
        <div class="metric-label"><el-icon aria-hidden="true"><TrendCharts /></el-icon><span>平均完成度</span></div>
        <strong class="rf-tabular">{{ dashboard.averageProgress || 0 }}%</strong>
        <p>当前项目平均执行进度</p>
      </article>
    </section>

    <section class="analytics-grid">
      <div class="panel lifecycle-panel">
        <div class="panel-title">
          <div><h2>项目状态分布</h2><p>从生命周期视角观察项目所处阶段。</p></div>
          <span>共 {{ dashboard.totalProjects || 0 }} 个项目</span>
        </div>

        <div class="status-bars">
          <div v-for="s in statuses" :key="s.label" class="status-row">
            <div class="status-label">
              <span><i :class="s.tone" aria-hidden="true"></i>{{ s.label }}</span>
              <strong class="rf-tabular">{{ s.value }}</strong>
            </div>
            <div
              class="bar-track"
              role="progressbar"
              :aria-label="s.label + '项目占比'"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-valuenow="percentage(s.value)"
            >
              <div class="bar-fill" :class="s.tone" :style="{ width: percentage(s.value) + '%' }"></div>
            </div>
            <div class="status-percent rf-tabular">{{ percentage(s.value) }}%</div>
          </div>
        </div>
      </div>

      <div class="panel health-panel">
        <div class="panel-title">
          <div><h2>运行健康度</h2><p>综合风险项目占比的管理视角。</p></div>
        </div>
        <div
          class="health-ring"
          :style="{ background: healthGradient }"
          role="img"
          :aria-label="'项目运行健康度 ' + healthScore + ' 分'"
        >
          <div>
            <strong class="rf-tabular">{{ healthScore }}</strong>
            <span>/ 100</span>
          </div>
        </div>
        <div class="health-note" :class="{ warning: dashboard.riskCount }">
          <el-icon aria-hidden="true">
            <WarningFilled v-if="dashboard.riskCount" />
            <CircleCheckFilled v-else />
          </el-icon>
          <span>{{ dashboard.riskCount ? dashboard.riskCount + ' 个项目需要优先关注' : '当前项目整体运行平稳' }}</span>
        </div>
      </div>
    </section>

    <section class="panel budget-section">
      <div class="panel-title">
        <div>
          <h2>预算执行概览</h2>
          <p>对比预算执行率与项目平均完成度，识别潜在失衡。</p>
        </div>
        <span>项目预算视角</span>
      </div>

      <div class="budget-layout">
        <div class="budget-number">
          <span>已执行</span>
          <strong class="rf-tabular">¥{{ money(dashboard.usedBudget) }}</strong>
          <small class="rf-tabular">总预算 ¥{{ money(dashboard.totalBudget) }}</small>
        </div>
        <div class="budget-progress">
          <div class="progress-head">
            <span>预算执行率</span>
            <strong class="rf-tabular">{{ dashboard.budgetExecutionRate || 0 }}%</strong>
          </div>
          <el-progress :percentage="dashboard.budgetExecutionRate || 0" :stroke-width="14" :show-text="false" />
          <div class="progress-head secondary">
            <span>平均完成度</span>
            <strong class="rf-tabular">{{ dashboard.averageProgress || 0 }}%</strong>
          </div>
          <el-progress :percentage="dashboard.averageProgress || 0" :stroke-width="10" :show-text="false" status="success" />
        </div>
      </div>

      <div class="budget-insight">
        <el-icon aria-hidden="true"><DataAnalysis /></el-icon>
        <span>
          预算执行率与项目平均完成度相差
          <strong class="rf-tabular">{{ progressBudgetGap }}%</strong>。
          {{ progressBudgetGap >= 20 ? '建议结合风险项目检查预算消耗与任务进展是否匹配。' : '当前两项指标差异处于可继续观察范围。' }}
        </span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { getResearchDashboard } from '@/api/research'

const loading = ref(false)
const dashboard = reactive({})

const statuses = computed(() => [
  { label: '待审批', value: dashboard.pendingApproval || 0, tone: 'warning' },
  { label: '执行中', value: dashboard.inProgress || 0, tone: 'primary' },
  { label: '待验收', value: dashboard.pendingAcceptance || 0, tone: 'accent' },
  { label: '已结项', value: dashboard.completed || 0, tone: 'success' }
])

const healthScore = computed(() => {
  const total = Number(dashboard.totalProjects || 0)
  if (!total) return 100
  return Math.max(0, Math.round(100 - Number(dashboard.riskCount || 0) / total * 45))
})

const healthGradient = computed(() =>
  'conic-gradient(var(--rf-success) ' + healthScore.value + '%, var(--rf-border) 0)'
)

const progressBudgetGap = computed(() =>
  Math.abs(Number(dashboard.budgetExecutionRate || 0) - Number(dashboard.averageProgress || 0))
)

function percentage(v) {
  const total = Number(dashboard.totalProjects || 0)
  return total ? Math.round(Number(v || 0) * 100 / total) : 0
}

function money(v) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

async function load() {
  loading.value = true
  try {
    const res = await getResearchDashboard()
    Object.assign(dashboard, res.data || {})
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.analytics-page { display: flex; flex-direction: column; gap: 16px; }

.metric-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.metric-row article, .panel {
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  background: var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.metric-row article { min-width: 0; padding: 17px; }
.metric-label { display: flex; align-items: center; gap: 7px; color: var(--rf-text-muted); font-size: 12px; }
.metric-label .el-icon { color: var(--rf-primary); font-size: 16px; }
.metric-row strong { display: block; margin-top: 10px; overflow-wrap: anywhere; color: var(--rf-text); font-size: clamp(22px, 2.2vw, 28px); font-weight: 750; letter-spacing: -.6px; }
.metric-row p { margin: 4px 0 0; color: var(--rf-text-muted); font-size: 11px; line-height: 1.45; }

.analytics-grid { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(300px, .65fr); gap: 14px; }
.panel { padding: 20px; }
.panel-title { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-title h2 { margin: 0; color: var(--rf-text); font-size: 15px; font-weight: 700; }
.panel-title p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.panel-title > span { color: var(--rf-text-muted); font-size: 11px; white-space: nowrap; }

.status-bars { margin-top: 22px; display: flex; flex-direction: column; gap: 17px; }
.status-row { display: grid; grid-template-columns: minmax(120px, .75fr) minmax(180px, 1.5fr) 46px; gap: 14px; align-items: center; }
.status-label { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--rf-text-secondary); font-size: 12px; }
.status-label > span { display: flex; align-items: center; gap: 7px; }
.status-label i { width: 7px; height: 7px; border-radius: 50%; background: var(--rf-text-muted); }
.status-label i.warning { background: var(--rf-warning); }
.status-label i.primary { background: var(--rf-primary); }
.status-label i.accent { background: #7c3aed; }
.status-label i.success { background: var(--rf-success); }
.status-label strong { color: var(--rf-text); }
.bar-track { height: 9px; overflow: hidden; border-radius: 999px; background: var(--rf-border); }
.bar-fill { height: 100%; border-radius: 999px; background: var(--rf-text-muted); transition: width var(--rf-motion-slow) ease; }
.bar-fill.warning { background: var(--rf-warning); }
.bar-fill.primary { background: var(--rf-primary); }
.bar-fill.accent { background: #7c3aed; }
.bar-fill.success { background: var(--rf-success); }
.status-percent { color: var(--rf-text-muted); font-size: 12px; text-align: right; }

.health-panel { display: flex; flex-direction: column; }
.health-ring {
  width: 156px;
  height: 156px;
  margin: 25px auto 16px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
}
.health-ring::after { content: ""; position: absolute; inset: 12px; border-radius: 50%; background: var(--rf-surface); }
.health-ring > div { position: relative; z-index: 1; text-align: center; }
.health-ring strong { color: var(--rf-text); font-size: 31px; font-weight: 760; }
.health-ring span { color: var(--rf-text-muted); font-size: 11px; }
.health-note {
  min-height: 42px;
  padding: 9px 11px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  background: var(--rf-success-soft);
  color: var(--rf-success);
  font-size: 12px;
  text-align: center;
}
.health-note.warning { background: var(--rf-warning-soft); color: var(--rf-warning); }

.budget-layout { margin-top: 24px; display: grid; grid-template-columns: minmax(220px, .55fr) minmax(0, 1.45fr); gap: 26px; align-items: center; }
.budget-number { min-width: 0; padding-right: 24px; border-right: 1px solid var(--rf-border); }
.budget-number > span { display: block; color: var(--rf-text-muted); font-size: 12px; }
.budget-number strong { display: block; margin-top: 5px; overflow-wrap: anywhere; color: var(--rf-text); font-size: 30px; font-weight: 760; letter-spacing: -.8px; }
.budget-number small { display: block; margin-top: 4px; color: var(--rf-text-muted); font-size: 11px; }
.progress-head { margin: 0 0 7px; display: flex; align-items: center; justify-content: space-between; color: var(--rf-text-secondary); font-size: 12px; }
.progress-head strong { color: var(--rf-text); }
.progress-head.secondary { margin-top: 15px; }
.budget-insight {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 10px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.budget-insight .el-icon { margin-top: 2px; flex: 0 0 auto; color: var(--rf-primary); font-size: 16px; }
.budget-insight strong { color: var(--rf-text); }

@media (max-width: 980px) {
  .metric-row { grid-template-columns: repeat(2, 1fr); }
  .analytics-grid { grid-template-columns: 1fr; }
}
@media (max-width: 680px) {
  .metric-row { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .metric-row article { padding: 14px; }
  .panel { padding: 16px; }
  .status-row { grid-template-columns: 1fr 42px; gap: 8px 12px; }
  .bar-track { grid-column: 1 / -1; grid-row: 2; }
  .status-percent { grid-column: 2; grid-row: 1; }
  .status-label strong { display: none; }
  .budget-layout { grid-template-columns: 1fr; gap: 18px; }
  .budget-number { padding-right: 0; padding-bottom: 18px; border-right: 0; border-bottom: 1px solid var(--rf-border); }
}
</style>
