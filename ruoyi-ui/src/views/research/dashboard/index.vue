<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="welcome-panel" aria-labelledby="dashboard-welcome">
      <div class="welcome-copy">
        <span class="eyebrow">RESEARCH OPERATIONS</span>
        <h1 id="dashboard-welcome">{{ greeting }}，{{ userStore.nickName || userStore.name }}</h1>
        <p>先处理需要推进的事项，再进入项目细节。</p>
      </div>
      <div class="welcome-actions">
        <button class="priority-chip" type="button" @click="router.push('/research/approvals')">
          <span>待处理</span>
          <strong class="rf-tabular">{{ (data.pendingApproval || 0) + (data.pendingAcceptance || 0) }}</strong>
        </button>
        <button class="priority-chip danger" type="button" @click="router.push('/research/risks')">
          <span>风险项目</span>
          <strong class="rf-tabular">{{ data.riskCount || 0 }}</strong>
        </button>
        <el-button type="primary" @click="router.push('/research/projects')">
          查看全部项目
          <el-icon class="el-icon--right" aria-hidden="true"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </section>

    <section class="metric-grid" aria-label="科研项目核心指标">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-head">
          <span>{{ item.label }}</span>
          <div class="metric-icon" :class="item.tone" aria-hidden="true">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
        </div>
        <div class="metric-value rf-tabular">{{ item.value }}</div>
        <div class="metric-note">{{ item.note }}</div>
      </article>
    </section>

    <section class="content-grid">
      <div class="panel projects-panel">
        <div class="panel-head">
          <div>
            <h2>最近项目</h2>
            <p>按最近更新时间排序，进入项目工作空间继续推进。</p>
          </div>
          <el-button text type="primary" @click="router.push('/research/projects')">全部项目</el-button>
        </div>

        <div v-if="data.recentProjects?.length" class="project-list">
          <button
            v-for="project in data.recentProjects"
            :key="project.projectId"
            class="project-row"
            type="button"
            @click="openProject(project.projectId)"
          >
            <div class="project-main">
              <div class="project-title-line">
                <strong>{{ project.projectName }}</strong>
                <span class="status-pill" :class="statusClass(project.status)">
                  <i aria-hidden="true"></i>{{ statusText(project.status) }}
                </span>
              </div>
              <div class="project-meta">
                <span>{{ project.projectNo }}</span>
                <span>{{ project.ownerName || '未指定负责人' }}</span>
                <span>{{ project.deptName || '未指定部门' }}</span>
              </div>
            </div>
            <div class="project-progress">
              <div class="progress-label">
                <span>项目进度</span>
                <strong class="rf-tabular">{{ project.progress || 0 }}%</strong>
              </div>
              <el-progress :percentage="project.progress || 0" :stroke-width="7" :show-text="false" />
            </div>
            <el-icon class="row-arrow" aria-hidden="true"><ArrowRight /></el-icon>
          </button>
        </div>
        <el-empty v-else description="暂无项目" :image-size="76" />
      </div>

      <div class="panel risk-panel">
        <div class="panel-head">
          <div>
            <h2>需要关注</h2>
            <p>风险来自时间、进度与预算的确定性规则。</p>
          </div>
          <el-button text type="danger" @click="router.push('/research/risks')">查看全部</el-button>
        </div>

        <div v-if="data.riskProjects?.length" class="risk-list">
          <button
            v-for="project in data.riskProjects"
            :key="project.projectId"
            class="risk-item"
            type="button"
            @click="openProject(project.projectId)"
          >
            <div class="risk-icon" aria-hidden="true"><el-icon><WarningFilled /></el-icon></div>
            <div class="risk-content">
              <div class="risk-title">
                <strong>{{ project.projectName }}</strong>
                <span :class="['risk-level', project.riskLevel?.toLowerCase()]">
                  {{ project.riskLevel === 'HIGH' ? '高风险' : '需关注' }}
                </span>
              </div>
              <p>{{ project.riskReason }}</p>
            </div>
            <el-icon class="risk-arrow" aria-hidden="true"><ArrowRight /></el-icon>
          </button>
        </div>

        <div v-else class="healthy-state">
          <div class="healthy-icon" aria-hidden="true"><el-icon><CircleCheckFilled /></el-icon></div>
          <strong>当前没有明显风险</strong>
          <p>项目时间、进度与预算执行处于合理区间。</p>
        </div>
      </div>
    </section>

    <section class="panel budget-panel">
      <div class="panel-head budget-head">
        <div>
          <h2>经费执行</h2>
          <p>项目维度预算跟踪，不替代财务系统。</p>
        </div>
        <div class="budget-total rf-tabular">
          ¥{{ money(data.usedBudget) }}
          <span>/ ¥{{ money(data.totalBudget) }}</span>
        </div>
      </div>
      <div class="budget-progress">
        <el-progress :percentage="data.budgetExecutionRate || 0" :stroke-width="12" />
      </div>
      <div class="budget-foot">
        <span>整体预算执行率 <strong class="rf-tabular">{{ data.budgetExecutionRate || 0 }}%</strong></span>
        <span>项目平均完成度 <strong class="rf-tabular">{{ data.averageProgress || 0 }}%</strong></span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { getResearchDashboard } from '@/api/research'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const data = reactive({})

const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'
})

const metrics = computed(() => [
  { label: '项目总数', value: data.totalProjects || 0, note: '当前可见科研项目', icon: 'FolderOpened', tone: 'blue' },
  { label: '执行中', value: data.inProgress || 0, note: '正在推进的项目', icon: 'VideoPlay', tone: 'green' },
  { label: '待处理', value: (data.pendingApproval || 0) + (data.pendingAcceptance || 0), note: '审批 ' + (data.pendingApproval || 0) + ' · 验收 ' + (data.pendingAcceptance || 0), icon: 'Bell', tone: 'orange' },
  { label: '风险项目', value: data.riskCount || 0, note: '需要优先关注', icon: 'Warning', tone: 'red' }
])

async function load() {
  loading.value = true
  try {
    const res = await getResearchDashboard()
    Object.assign(data, res.data || {})
  } finally {
    loading.value = false
  }
}

function openProject(id) { router.push('/research/projects/' + id) }
function money(v) { return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function statusText(status) {
  return ({
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已立项',
    IN_PROGRESS: '执行中',
    PENDING_ACCEPTANCE: '待验收',
    COMPLETED: '已结项',
    REJECTED: '已驳回',
    TERMINATED: '已终止'
  })[status] || status
}
function statusClass(status) { return status?.toLowerCase().replaceAll('_', '-') || '' }

onMounted(load)
</script>

<style scoped lang="scss">
.dashboard-page { display: flex; flex-direction: column; gap: 20px; }

.welcome-panel {
  min-height: 128px;
  padding: 24px 26px;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: linear-gradient(120deg, rgba(37, 99, 235, .08), transparent 45%), var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.eyebrow { color: var(--rf-primary); font-size: 12px; font-weight: 750; letter-spacing: 1.5px; }
h1 { margin: 8px 0 6px; color: var(--rf-text); font-size: clamp(24px, 2.2vw, 30px); line-height: 1.2; letter-spacing: -.6px; }
.welcome-copy p { margin: 0; color: var(--rf-text-muted); font-size: 14px; line-height: 1.6; }
.welcome-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.priority-chip {
  min-width: 94px;
  min-height: 44px;
  padding: 7px 12px;
  border: 1px solid var(--rf-border);
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  cursor: pointer;
  transition: border-color var(--rf-motion-fast) ease, background var(--rf-motion-fast) ease;
}
.priority-chip:hover { border-color: var(--rf-primary-border); background: var(--rf-primary-soft); }
.priority-chip span { font-size: 12px; }
.priority-chip strong { color: var(--rf-primary); font-size: 17px; }
.priority-chip.danger strong { color: var(--rf-danger); }

.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.metric-card, .panel { border: 1px solid var(--rf-border); border-radius: var(--rf-radius-lg); background: var(--rf-surface); box-shadow: var(--rf-shadow-sm); }
.metric-card { padding: 18px; }
.metric-head { display: flex; justify-content: space-between; align-items: center; color: var(--rf-text-muted); font-size: 13px; font-weight: 550; }
.metric-icon { width: 36px; height: 36px; border-radius: 10px; display: grid; place-items: center; font-size: 18px; }
.metric-icon.blue { background: var(--rf-primary-soft); color: var(--rf-primary); }
.metric-icon.green { background: var(--rf-success-soft); color: var(--rf-success); }
.metric-icon.orange { background: var(--rf-warning-soft); color: var(--rf-warning); }
.metric-icon.red { background: var(--rf-danger-soft); color: var(--rf-danger); }
.metric-value { margin-top: 10px; color: var(--rf-text); font-size: 30px; font-weight: 760; letter-spacing: -1px; }
.metric-note { margin-top: 4px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }

.content-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(330px, .82fr); gap: 16px; }
.panel { padding: 20px; }
.panel-head { margin-bottom: 14px; display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.panel-head h2 { margin: 0; color: var(--rf-text); font-size: 16px; font-weight: 700; }
.panel-head p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }

.project-list { display: flex; flex-direction: column; }
.project-row {
  width: 100%;
  min-height: 78px;
  padding: 15px 4px;
  border: 0;
  border-top: 1px solid var(--rf-border);
  background: transparent;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 190px 26px;
  gap: 18px;
  align-items: center;
  text-align: left;
  color: inherit;
  cursor: pointer;
  transition: background var(--rf-motion-fast) ease;
}
.project-row:first-child { border-top: 0; }
.project-row:hover { background: var(--rf-surface-subtle); }
.project-row:focus-visible { border-radius: 10px; box-shadow: var(--rf-focus); }
.project-main { min-width: 0; }
.project-title-line { display: flex; align-items: center; gap: 9px; min-width: 0; }
.project-title-line strong { min-width: 0; overflow: hidden; color: var(--rf-text); font-size: 14px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.project-meta { margin-top: 7px; display: flex; flex-wrap: wrap; gap: 6px 14px; color: var(--rf-text-muted); font-size: 12px; }
.project-meta span + span::before { content: "·"; margin-right: 14px; color: var(--rf-border-strong); }
.project-progress { min-width: 0; }
.progress-label { margin-bottom: 7px; display: flex; justify-content: space-between; color: var(--rf-text-muted); font-size: 12px; }
.progress-label strong { color: var(--rf-text-secondary); }
.row-arrow { color: var(--rf-text-muted); font-size: 17px; }

.status-pill {
  flex: 0 0 auto;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  font-size: 12px;
  font-weight: 600;
}
.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: var(--rf-text-muted); }
.status-pill.in-progress { background: var(--rf-primary-soft); color: var(--rf-primary-hover); }
.status-pill.in-progress i { background: var(--rf-primary); }
.status-pill.pending-approval, .status-pill.pending-acceptance { background: var(--rf-warning-soft); color: var(--rf-warning); }
.status-pill.pending-approval i, .status-pill.pending-acceptance i { background: var(--rf-warning); }
.status-pill.completed { background: var(--rf-success-soft); color: var(--rf-success); }
.status-pill.completed i { background: var(--rf-success); }
.status-pill.rejected, .status-pill.terminated { background: var(--rf-danger-soft); color: var(--rf-danger); }
.status-pill.rejected i, .status-pill.terminated i { background: var(--rf-danger); }

.risk-list { display: flex; flex-direction: column; gap: 8px; }
.risk-item {
  width: 100%;
  min-height: 76px;
  padding: 13px;
  border: 1px solid color-mix(in srgb, var(--rf-danger) 20%, var(--rf-border));
  border-radius: 12px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 22px;
  gap: 11px;
  align-items: center;
  background: var(--rf-danger-soft);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--rf-motion-fast) ease, transform var(--rf-motion-fast) ease;
}
.risk-item:hover { border-color: color-mix(in srgb, var(--rf-danger) 42%, var(--rf-border)); transform: translateY(-1px); }
.risk-icon { width: 38px; height: 38px; border-radius: 10px; display: grid; place-items: center; background: color-mix(in srgb, var(--rf-danger) 12%, transparent); color: var(--rf-danger); font-size: 18px; }
.risk-content { min-width: 0; }
.risk-title { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.risk-title strong { min-width: 0; overflow: hidden; color: var(--rf-text); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.risk-level { flex: 0 0 auto; font-size: 12px; font-weight: 650; color: var(--rf-danger); }
.risk-level:not(.high) { color: var(--rf-warning); }
.risk-content p { margin: 5px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.5; }
.risk-arrow { color: var(--rf-text-muted); }

.healthy-state { padding: 26px 12px; text-align: center; }
.healthy-icon { color: var(--rf-success); font-size: 34px; }
.healthy-state strong { display: block; margin-top: 8px; color: var(--rf-text); font-size: 14px; }
.healthy-state p { margin: 6px auto 0; max-width: 320px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }

.budget-head { align-items: center; }
.budget-total { color: var(--rf-text); font-size: 20px; font-weight: 720; }
.budget-total span { color: var(--rf-text-muted); font-size: 12px; font-weight: 450; }
.budget-foot { margin-top: 10px; display: flex; justify-content: space-between; gap: 12px; color: var(--rf-text-muted); font-size: 12px; }
.budget-foot strong { color: var(--rf-text-secondary); }

@media (max-width: 1100px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
  .welcome-panel { align-items: flex-start; flex-direction: column; }
  .welcome-actions { width: 100%; justify-content: flex-start; }
}
@media (max-width: 700px) {
  .dashboard-page { gap: 14px; }
  .welcome-panel { min-height: 0; padding: 18px; }
  .welcome-actions { width: 100%; display: grid; grid-template-columns: 1fr 1fr; }
  .welcome-actions > .el-button { grid-column: 1 / -1; width: 100%; }
  .priority-chip { width: 100%; }
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .metric-card { padding: 14px; }
  .metric-value { font-size: 25px; }
  .panel { padding: 16px; }
  .project-row { grid-template-columns: minmax(0, 1fr) 24px; gap: 10px; }
  .project-progress { display: none; }
  .project-title-line { align-items: flex-start; flex-direction: column; }
  .project-meta span + span::before { display: none; }
  .budget-head { align-items: flex-start; flex-direction: column; }
  .budget-foot { flex-direction: column; gap: 5px; }
}
</style>
