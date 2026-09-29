<template>
  <div class="dashboard" v-loading="loading">
    <div class="metric-strip">
      <div v-for="m in metrics" :key="m.label" class="metric"><span>{{ m.label }}</span><strong>{{ m.value }}</strong></div>
    </div>
    <div class="grid">
      <section class="panel">
        <div class="panel-head"><strong>最近项目</strong><el-button text type="primary" @click="router.push('/research/projects')">全部项目</el-button></div>
        <div class="rows">
          <button v-for="p in data.recentProjects||[]" :key="p.projectId" class="row" @click="router.push('/research/projects/'+p.projectId)">
            <span class="main"><strong>{{ p.projectName }}</strong><small>{{ p.projectNo }} · {{ statusText(p.status) }}</small></span>
            <span class="progress">{{ p.progress||0 }}%</span>
          </button>
          <el-empty v-if="!(data.recentProjects||[]).length" description="暂无项目" :image-size="60" />
        </div>
      </section>
      <section class="panel">
        <div class="panel-head"><strong>待处理</strong><el-button text type="primary" @click="router.push('/research/approvals')">审批中心</el-button></div>
        <div class="rows">
          <div v-for="a in data.pendingApprovals||[]" :key="a.businessType+'-'+a.businessId" class="row plain">
            <span class="main"><strong>{{ a.title }}</strong><small>{{ a.businessNo }} · {{ typeText(a.businessType) }}</small></span><span class="badge">待处理</span>
          </div>
          <el-empty v-if="!(data.pendingApprovals||[]).length" description="暂无待办" :image-size="60" />
        </div>
      </section>
      <section class="panel wide">
        <div class="panel-head"><strong>高风险关注</strong><el-button text type="primary" @click="router.push('/research/risks')">风险登记册</el-button></div>
        <div class="risk-row" v-for="r in data.risks||[]" :key="r.riskId">
          <span :class="['risk',r.riskLevel?.toLowerCase()]">{{ r.riskLevel }}</span>
          <strong>{{ r.title }}</strong><span>{{ r.projectName }}</span><span>{{ r.score }} 分</span>
        </div>
        <el-empty v-if="!(data.risks||[]).length" description="暂无高风险" :image-size="60" />
      </section>
    </div>
  </div>
</template>
<script setup>
import { getResearchDashboard } from '@/api/research'
const router=useRouter(),loading=ref(false),data=reactive({})
const metrics=computed(()=>[
  {label:'项目申请',value:data.proposalCount||0},{label:'待评审',value:data.pendingReview||0},{label:'正式项目',value:data.projectCount||0},
  {label:'执行中',value:data.activeProjects||0},{label:'高风险',value:data.highRiskCount||0},{label:'开放问题',value:data.openIssueCount||0},
  {label:'预算执行',value:(data.budgetExecutionRate||0)+'%'}
])
async function load(){loading.value=true;try{const r=await getResearchDashboard();Object.assign(data,r.data||{})}finally{loading.value=false}}
function statusText(s){return ({PLANNING:'规划中',ACTIVE:'执行中',CLOSING:'结项中',CLOSED:'已结项'})[s]||s}
function typeText(t){return ({PROPOSAL:'申报',CHANGE_REQUEST:'变更',ACCEPTANCE:'验收'})[t]||t}
onMounted(load)
</script>
<style scoped lang="scss">
.dashboard{display:flex;flex-direction:column;gap:10px}.metric-strip{display:grid;grid-template-columns:repeat(7,1fr);border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.metric{min-width:0;padding:10px 12px;border-right:1px solid var(--rf-border);display:flex;flex-direction:column;gap:4px}.metric:last-child{border-right:0}.metric span{color:var(--rf-text-muted);font-size:11px}.metric strong{font-size:20px;color:var(--rf-text);font-variant-numeric:tabular-nums}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.panel{border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);overflow:hidden}.panel.wide{grid-column:1/-1}.panel-head{min-height:44px;padding:6px 12px;border-bottom:1px solid var(--rf-border);display:flex;align-items:center;justify-content:space-between}.panel-head strong{font-size:13px}.rows{display:flex;flex-direction:column}.row{width:100%;min-height:50px;padding:7px 12px;border:0;border-bottom:1px solid var(--rf-border);background:transparent;display:flex;align-items:center;gap:10px;text-align:left;cursor:pointer}.row:last-child{border-bottom:0}.row:hover{background:var(--rf-surface-subtle)}.row.plain{cursor:default}.main{min-width:0;flex:1;display:flex;flex-direction:column;gap:3px}.main strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:12px;color:var(--rf-text)}.main small{color:var(--rf-text-muted);font-size:11px}.progress{font-size:12px;font-weight:700;color:var(--rf-primary)}.badge,.risk{min-height:22px;padding:0 7px;border-radius:999px;display:inline-flex;align-items:center;font-size:10px;font-weight:700}.badge{background:var(--rf-warning-soft);color:var(--rf-warning)}.risk-row{min-height:42px;padding:6px 12px;border-bottom:1px solid var(--rf-border);display:grid;grid-template-columns:75px minmax(180px,1fr) minmax(180px,1fr) 60px;gap:10px;align-items:center;font-size:11px}.risk{background:var(--rf-danger-soft);color:var(--rf-danger)}.risk-row>span:not(.risk){color:var(--rf-text-muted)}@media(max-width:1100px){.metric-strip{grid-template-columns:repeat(4,1fr)}.metric:nth-child(4){border-right:0}.grid{grid-template-columns:1fr}}@media(max-width:680px){.metric-strip{grid-template-columns:repeat(2,1fr)}.metric{border-bottom:1px solid var(--rf-border)}.grid{grid-template-columns:1fr}.risk-row{grid-template-columns:70px 1fr}.risk-row>span:nth-child(n+3){display:none}}
</style>
