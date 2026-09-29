<template>
  <div v-loading="loading" class="analytics-page">
    <div class="metric-row">
      <div><span>项目总数</span><strong>{{ dashboard.totalProjects || 0 }}</strong></div>
      <div><span>整体预算</span><strong>¥{{ money(dashboard.totalBudget) }}</strong></div>
      <div><span>预算执行率</span><strong>{{ dashboard.budgetExecutionRate || 0 }}%</strong></div>
      <div><span>平均完成度</span><strong>{{ dashboard.averageProgress || 0 }}%</strong></div>
    </div>
    <div class="analytics-grid">
      <section class="panel">
        <div class="panel-title"><h3>项目状态分布</h3><span>生命周期视角</span></div>
        <div class="status-bars">
          <div v-for="s in statuses" :key="s.label" class="status-row">
            <div class="status-label"><span>{{ s.label }}</span><strong>{{ s.value }}</strong></div>
            <div class="bar-track"><div class="bar-fill" :style="{width: `${percentage(s.value)}%`} "></div></div>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-title"><h3>运行健康度</h3><span>进度与风险</span></div>
        <div class="health-ring" :style="{ background: healthGradient }"><div><strong>{{ healthScore }}</strong><span>/ 100</span></div></div>
        <div class="health-note">{{ dashboard.riskCount ? `${dashboard.riskCount} 个项目需要优先关注` : '当前项目整体运行平稳' }}</div>
      </section>
    </div>
    <section class="panel budget-section">
      <div class="panel-title"><h3>预算执行概览</h3><span>用于科研管理视角的项目预算跟踪</span></div>
      <div class="budget-number"><strong>¥{{ money(dashboard.usedBudget) }}</strong><span>已执行 / ¥{{ money(dashboard.totalBudget) }} 总预算</span></div>
      <el-progress :percentage="dashboard.budgetExecutionRate || 0" :stroke-width="16" />
      <div class="budget-insight">预算执行率与项目平均完成度相差 <strong>{{ Math.abs((dashboard.budgetExecutionRate||0)-(dashboard.averageProgress||0)) }}%</strong>，可结合风险项目进一步检查。</div>
    </section>
  </div>
</template>
<script setup>
import { getResearchDashboard } from '@/api/research'
const loading=ref(false); const dashboard=reactive({})
const statuses=computed(()=>[
 {label:'待审批',value:dashboard.pendingApproval||0},{label:'执行中',value:dashboard.inProgress||0},{label:'待验收',value:dashboard.pendingAcceptance||0},{label:'已结项',value:dashboard.completed||0}
])
const healthScore=computed(()=>{const total=Number(dashboard.totalProjects||0);if(!total)return 100;return Math.max(0,Math.round(100-Number(dashboard.riskCount||0)/total*45))})
const healthGradient=computed(()=>`conic-gradient(#35a86b ${healthScore.value}%, #edf1f5 0)`)
function percentage(v){const total=Number(dashboard.totalProjects||0);return total?Math.max(5,Math.round(v*100/total)):0}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
async function load(){loading.value=true;try{const res=await getResearchDashboard();Object.assign(dashboard,res.data||{})}finally{loading.value=false}}
onMounted(load)
</script>
<style scoped lang="scss">
.analytics-page{display:flex;flex-direction:column;gap:18px}.metric-row{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.metric-row>div,.panel{background:#fff;border:1px solid #e7ecf2;border-radius:15px;padding:20px}.metric-row span{display:block;color:#8c98a9;font-size:11px}.metric-row strong{display:block;margin-top:8px;font-size:26px}.analytics-grid{display:grid;grid-template-columns:1.5fr .7fr;gap:16px}.panel-title{display:flex;justify-content:space-between;align-items:center}.panel-title h3{margin:0;font-size:15px}.panel-title span{color:#9aa4b2;font-size:11px}.status-bars{margin-top:22px;display:flex;flex-direction:column;gap:17px}.status-label{display:flex;justify-content:space-between;font-size:12px}.bar-track{height:8px;background:#eef2f6;border-radius:99px;margin-top:7px;overflow:hidden}.bar-fill{height:100%;background:linear-gradient(90deg,#2f80ed,#56ccf2);border-radius:99px}.health-ring{width:150px;height:150px;margin:24px auto 14px;border-radius:50%;display:grid;place-items:center;position:relative}.health-ring:after{content:'';position:absolute;inset:12px;border-radius:50%;background:#fff}.health-ring>div{z-index:1;text-align:center}.health-ring strong{font-size:30px}.health-ring span{color:#9aa4b2;font-size:11px}.health-note{text-align:center;color:#738094;font-size:12px}.budget-number{margin:26px 0 18px;display:flex;align-items:end;gap:10px}.budget-number strong{font-size:30px}.budget-number span{color:#98a3b2;font-size:11px;padding-bottom:5px}.budget-insight{margin-top:14px;padding:12px 14px;border-radius:9px;background:#f7f9fc;color:#718096;font-size:11px}@media(max-width:900px){.metric-row{grid-template-columns:repeat(2,1fr)}.analytics-grid{grid-template-columns:1fr}}
</style>
