<template>
  <div class="dashboard" v-loading="loading">
    <section class="metrics">
      <button class="metric" @click="router.push('/research/projects')"><span>正式项目</span><strong>{{ data.totalProjects||0 }}</strong><small>执行中 {{ data.activeProjects||0 }}</small></button>
      <button class="metric" @click="router.push('/research/proposals')"><span>待评审申请</span><strong>{{ data.pendingProposals||0 }}</strong><small>草稿 {{ data.draftProposals||0 }}</small></button>
      <button class="metric risk" @click="router.push('/research/risks')"><span>开放风险</span><strong>{{ data.openRisks||0 }}</strong><small>问题 {{ data.openIssues||0 }}</small></button>
      <div class="metric"><span>预算执行</span><strong>{{ data.budgetExecutionRate||0 }}%</strong><small>¥{{ compact(data.usedBudget) }} / ¥{{ compact(data.totalBudget) }}</small></div>
    </section>

    <section class="grid">
      <div class="panel projects">
        <div class="panel-head"><div><strong>最近项目</strong><span>优先显示正在执行的科研项目</span></div><el-button text type="primary" @click="router.push('/research/projects')">全部</el-button></div>
        <div class="rows">
          <button v-for="p in data.recentProjects||[]" :key="p.projectId" class="row" @click="router.push('/research/projects/'+p.projectId)">
            <div class="main"><strong>{{ p.projectName }}</strong><span>{{ p.projectNo }} · {{ p.piName }}</span></div>
            <span :class="['badge',tone(p.status)]">{{ statusText(p.status) }}</span>
            <div class="prog"><el-progress :percentage="p.progress||0" :show-text="false" :stroke-width="5"/><b>{{ p.progress||0 }}%</b></div>
            <span class="money">¥{{ compact(p.currentBudget) }}</span><el-icon><ArrowRight/></el-icon>
          </button>
          <el-empty v-if="!data.recentProjects?.length" description="暂无正式项目" :image-size="54"/>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head"><div><strong>高优先级风险</strong><span>HIGH / CRITICAL</span></div><el-button text type="danger" @click="router.push('/research/risks')">全部</el-button></div>
        <div class="risk-list">
          <button v-for="r in data.priorityRisks||[]" :key="r.riskId" @click="router.push('/research/projects/'+r.projectId)">
            <span :class="['level',r.riskLevel?.toLowerCase()]">{{ r.riskLevel }}</span><div><strong>{{ r.title }}</strong><p>{{ r.projectName }}</p></div><el-icon><ArrowRight/></el-icon>
          </button>
          <div v-if="!data.priorityRisks?.length" class="healthy"><el-icon><CircleCheckFilled/></el-icon><span>当前没有高优先级风险</span></div>
        </div>
      </div>
    </section>

    <section class="panel summary">
      <div><span>项目平均完成度</span><strong>{{ data.averageProgress||0 }}%</strong></div>
      <el-progress :percentage="data.averageProgress||0" :show-text="false" :stroke-width="7"/>
      <div class="summary-note">项目计划、预算、风险和问题采用结构化事实记录，AI 仅用于辅助解释与总结。</div>
    </section>
  </div>
</template>
<script setup>
import { getResearchDashboard } from '@/api/research'
const router=useRouter(),loading=ref(false),data=reactive({})
async function load(){loading.value=true;try{const r=await getResearchDashboard();Object.assign(data,r.data||{})}finally{loading.value=false}}
function compact(v){const n=Number(v||0);return n>=10000?(n/10000).toFixed(n%10000===0?0:1)+'万':n.toLocaleString()}
function statusText(s){return ({PLANNING:'计划中',ACTIVE:'执行中',SUSPENDED:'暂停',CLOSING:'结项中',CLOSED:'已结项'})[s]||s}
function tone(s){return s==='ACTIVE'?'success':s==='PLANNING'||s==='CLOSING'?'warning':'neutral'}
onMounted(load)
</script>
<style scoped lang="scss">
.dashboard{display:flex;flex-direction:column;gap:12px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.metric{min-height:92px;padding:12px 14px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);text-align:left;color:inherit;display:flex;flex-direction:column;cursor:pointer}.metric span{color:var(--rf-text-muted);font-size:11px}.metric strong{margin:5px 0 2px;font-size:24px;line-height:1}.metric small{color:var(--rf-text-secondary);font-size:11px}.metric.risk strong{color:var(--rf-danger)}.grid{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(300px,.75fr);gap:10px}.panel{border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);overflow:hidden}.panel-head{min-height:48px;padding:8px 12px;border-bottom:1px solid var(--rf-border);display:flex;align-items:center;justify-content:space-between}.panel-head>div{display:flex;flex-direction:column;gap:2px}.panel-head strong{font-size:13px}.panel-head span{font-size:10px;color:var(--rf-text-muted)}.row{width:100%;min-height:52px;padding:7px 11px;border:0;border-top:1px solid var(--rf-border);background:none;color:inherit;display:grid;grid-template-columns:minmax(200px,1fr) 70px 130px 90px 18px;gap:10px;align-items:center;text-align:left;cursor:pointer}.row:first-child{border-top:0}.row:hover{background:var(--rf-surface-subtle)}.main{min-width:0}.main strong,.main span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.main strong{font-size:12px}.main span{margin-top:3px;color:var(--rf-text-muted);font-size:10px}.badge,.level{justify-self:start;padding:3px 6px;border-radius:999px;font-size:10px;background:var(--rf-surface-subtle)}.badge.success{background:var(--rf-success-soft);color:var(--rf-success)}.badge.warning{background:var(--rf-warning-soft);color:var(--rf-warning)}.prog{display:grid;grid-template-columns:1fr 28px;gap:5px;align-items:center}.prog b,.money{font-size:10px}.risk-list button{width:100%;min-height:54px;padding:8px 11px;border:0;border-top:1px solid var(--rf-border);background:none;color:inherit;display:grid;grid-template-columns:auto 1fr 16px;gap:8px;align-items:center;text-align:left;cursor:pointer}.risk-list button:first-child{border-top:0}.risk-list strong{font-size:11px}.risk-list p{margin:3px 0 0;color:var(--rf-text-muted);font-size:10px}.level.high{background:var(--rf-danger-soft);color:var(--rf-danger)}.level.critical{background:var(--rf-danger);color:#fff}.healthy{min-height:150px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:7px;color:var(--rf-success);font-size:12px}.healthy .el-icon{font-size:28px}.summary{padding:12px;display:grid;grid-template-columns:150px minmax(180px,380px) 1fr;gap:14px;align-items:center}.summary>div:first-child{display:flex;justify-content:space-between;font-size:11px}.summary-note{color:var(--rf-text-muted);font-size:10px;text-align:right}
@media(max-width:950px){.metrics{grid-template-columns:1fr 1fr}.grid{grid-template-columns:1fr}.summary{grid-template-columns:1fr}}@media(max-width:520px){.metrics{grid-template-columns:1fr 1fr}.row{grid-template-columns:1fr auto}.row>.prog,.row>.money{display:none}}
</style>
