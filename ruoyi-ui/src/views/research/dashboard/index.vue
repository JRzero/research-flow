<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="hero-panel">
      <div>
        <div class="eyebrow">RESEARCH OPERATIONS</div>
        <h1>{{ greeting }}，{{ userStore.nickName || userStore.name }}</h1>
        <p>今天优先处理需要推进的项目，而不是浏览一堆后台菜单。</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/research/projects')">
        查看全部项目 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
    </section>

    <section class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-head">
          <span>{{ item.label }}</span>
          <div class="metric-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></div>
        </div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-note">{{ item.note }}</div>
      </div>
    </section>

    <section class="content-grid">
      <div class="panel projects-panel">
        <div class="panel-head">
          <div><h3>最近项目</h3><p>按最近更新时间排序</p></div>
          <el-button text type="primary" @click="router.push('/research/projects')">全部项目</el-button>
        </div>
        <div v-if="data.recentProjects?.length" class="project-list">
          <button v-for="project in data.recentProjects" :key="project.projectId" class="project-row" @click="openProject(project.projectId)">
            <div class="project-main">
              <div class="project-title-line">
                <strong>{{ project.projectName }}</strong>
                <span class="status-pill" :class="statusClass(project.status)">{{ statusText(project.status) }}</span>
              </div>
              <div class="project-meta">{{ project.projectNo }} · {{ project.ownerName || '未指定负责人' }} · {{ project.deptName || '未指定部门' }}</div>
            </div>
            <div class="project-progress">
              <div class="progress-label"><span>项目进度</span><strong>{{ project.progress || 0 }}%</strong></div>
              <el-progress :percentage="project.progress || 0" :stroke-width="7" :show-text="false" />
            </div>
            <el-icon class="row-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
        <el-empty v-else description="暂无项目" :image-size="80" />
      </div>

      <div class="panel risk-panel">
        <div class="panel-head">
          <div><h3>需要关注</h3><p>由确定性规则识别的项目风险</p></div>
          <el-button text type="danger" @click="router.push('/research/risks')">查看全部</el-button>
        </div>
        <div v-if="data.riskProjects?.length" class="risk-list">
          <div v-for="project in data.riskProjects" :key="project.projectId" class="risk-item" @click="openProject(project.projectId)">
            <div class="risk-icon"><el-icon><WarningFilled /></el-icon></div>
            <div class="risk-content">
              <div class="risk-title"><strong>{{ project.projectName }}</strong><span :class="['risk-level', project.riskLevel?.toLowerCase()]">{{ project.riskLevel === 'HIGH' ? '高风险' : '需关注' }}</span></div>
              <p>{{ project.riskReason }}</p>
            </div>
          </div>
        </div>
        <div v-else class="healthy-state">
          <div class="healthy-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <strong>当前没有明显风险</strong>
          <p>项目时间、进度与预算执行保持在合理区间。</p>
        </div>
      </div>
    </section>

    <section class="panel budget-panel">
      <div class="panel-head">
        <div><h3>经费执行</h3><p>项目维度预算跟踪，不替代财务系统</p></div>
        <div class="budget-total">¥ {{ money(data.usedBudget) }} <span>/ ¥ {{ money(data.totalBudget) }}</span></div>
      </div>
      <el-progress :percentage="data.budgetExecutionRate || 0" :stroke-width="12" />
      <div class="budget-foot"><span>整体预算执行率 {{ data.budgetExecutionRate || 0 }}%</span><span>项目平均完成度 {{ data.averageProgress || 0 }}%</span></div>
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
  { label: '待处理', value: (data.pendingApproval || 0) + (data.pendingAcceptance || 0), note: `审批 ${data.pendingApproval || 0} · 验收 ${data.pendingAcceptance || 0}`, icon: 'Bell', tone: 'orange' },
  { label: '风险项目', value: data.riskCount || 0, note: '需要优先关注', icon: 'Warning', tone: 'red' }
])

async function load() {
  loading.value = true
  try { const res = await getResearchDashboard(); Object.assign(data, res.data || {}) } finally { loading.value = false }
}
function openProject(id) { router.push(`/research/projects/${id}`) }
function money(v) { return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function statusText(status) { return ({DRAFT:'草稿',PENDING_APPROVAL:'待审批',APPROVED:'已立项',IN_PROGRESS:'执行中',PENDING_ACCEPTANCE:'待验收',COMPLETED:'已结项',REJECTED:'已驳回',TERMINATED:'已终止'})[status] || status }
function statusClass(status) { return status?.toLowerCase().replace('_','-') || '' }
onMounted(load)
</script>

<style scoped lang="scss">
.dashboard-page { display:flex; flex-direction:column; gap:22px; }
.hero-panel { min-height:150px; padding:28px 32px; border-radius:18px; color:#fff; display:flex; align-items:center; justify-content:space-between; background: radial-gradient(circle at 80% 20%,rgba(86,204,242,.35),transparent 30%), linear-gradient(135deg,#102a43,#174f8a); box-shadow:0 18px 40px rgba(16,42,67,.16); }
.eyebrow { font-size:11px; letter-spacing:2.2px; color:#8fdcff; font-weight:700; }
h1 { margin:8px 0 7px; font-size:28px; } .hero-panel p { margin:0; color:#bed0e3; font-size:14px; }
.metric-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px; }
.metric-card,.panel { background:#fff; border:1px solid #e9edf3; border-radius:16px; box-shadow:0 4px 18px rgba(20,37,63,.035); }
.metric-card { padding:20px; } .metric-head { display:flex; justify-content:space-between; align-items:center; color:#758297; font-size:13px; }
.metric-icon { width:36px;height:36px;border-radius:10px;display:grid;place-items:center;font-size:18px; }.metric-icon.blue{background:#eaf3ff;color:#2f80ed}.metric-icon.green{background:#ebfaf2;color:#219653}.metric-icon.orange{background:#fff4e5;color:#f2994a}.metric-icon.red{background:#fff0f0;color:#eb5757}
.metric-value { margin-top:12px;font-size:30px;font-weight:750;letter-spacing:-1px;}.metric-note{margin-top:5px;color:#9aa5b5;font-size:12px}
.content-grid { display:grid;grid-template-columns:minmax(0,1.55fr) minmax(340px,.8fr);gap:16px;}.panel{padding:22px}.panel-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:18px}.panel-head h3{margin:0;font-size:16px}.panel-head p{margin:5px 0 0;color:#98a3b3;font-size:12px}
.project-list{display:flex;flex-direction:column}.project-row{border:0;border-top:1px solid #edf0f4;background:transparent;padding:16px 4px;display:grid;grid-template-columns:minmax(0,1fr) 190px 24px;gap:20px;align-items:center;text-align:left;cursor:pointer}.project-row:first-child{border-top:0}.project-row:hover strong{color:#2f80ed}.project-title-line{display:flex;align-items:center;gap:10px}.project-title-line strong{font-size:14px;transition:.2s}.project-meta{margin-top:6px;font-size:12px;color:#99a3b2}.project-progress{min-width:0}.progress-label{display:flex;justify-content:space-between;font-size:11px;color:#8b96a7;margin-bottom:7px}.progress-label strong{color:#4c596b}.row-arrow{color:#b6bec9}
.status-pill{display:inline-flex;padding:3px 8px;border-radius:99px;font-size:10px;background:#f1f4f8;color:#627084}.status-pill.in-progress{background:#edf6ff;color:#2878d0}.status-pill.pending-approval,.status-pill.pending-acceptance{background:#fff6e8;color:#c97710}.status-pill.completed{background:#ebf8f0;color:#238a51}.status-pill.rejected{background:#fff0f0;color:#d94b4b}
.risk-list{display:flex;flex-direction:column;gap:10px}.risk-item{padding:14px;border-radius:12px;background:#fff8f6;border:1px solid #ffe7df;display:flex;gap:12px;cursor:pointer}.risk-icon{width:34px;height:34px;flex:0 0 auto;border-radius:9px;background:#ffe7df;color:#d9593a;display:grid;place-items:center}.risk-content{min-width:0;flex:1}.risk-title{display:flex;justify-content:space-between;gap:8px;font-size:13px}.risk-level{font-size:10px;padding:3px 7px;border-radius:99px;background:#ffeadf;color:#c85230}.risk-level.high{background:#ffe1e1;color:#c43232}.risk-content p{margin:6px 0 0;color:#8e6c62;font-size:11px;line-height:1.55}.healthy-state{text-align:center;padding:30px 12px}.healthy-icon{font-size:36px;color:#27ae60}.healthy-state strong{display:block;margin-top:10px}.healthy-state p{color:#9aa5b5;font-size:12px}
.budget-total{font-size:19px;font-weight:700}.budget-total span{font-size:12px;color:#a0a9b6;font-weight:400}.budget-foot{display:flex;justify-content:space-between;margin-top:10px;color:#8c97a7;font-size:11px}
@media(max-width:1100px){.metric-grid{grid-template-columns:repeat(2,1fr)}.content-grid{grid-template-columns:1fr}.hero-panel{align-items:flex-start;gap:20px;flex-direction:column}.project-row{grid-template-columns:minmax(0,1fr) 160px 20px}}@media(max-width:650px){.metric-grid{grid-template-columns:1fr}.project-row{grid-template-columns:1fr}.project-progress,.row-arrow{display:none}}
</style>
