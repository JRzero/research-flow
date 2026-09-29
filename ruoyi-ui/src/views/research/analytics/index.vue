<template>
  <div class="analytics" v-loading="loading">
    <div class="finance">
      <div><span>项目预算</span><strong>¥{{ money(data.finance?.totalBudget) }}</strong></div>
      <div><span>已执行</span><strong>¥{{ money(data.finance?.usedBudget) }}</strong></div>
      <div><span>执行率</span><strong>{{ data.budgetExecutionRate||0 }}%</strong></div>
      <div><span>开放风险</span><strong>{{ data.riskCount||0 }}</strong></div>
    </div>
    <div class="grid">
      <section class="panel"><h3>项目状态</h3><div v-for="x in data.projectStatus||[]" :key="x.name" class="stat-row"><span>{{ projectStatus(x.name) }}</span><strong>{{ x.value }}</strong></div></section>
      <section class="panel"><h3>申报状态</h3><div v-for="x in data.proposalStatus||[]" :key="x.name" class="stat-row"><span>{{ proposalStatus(x.name) }}</span><strong>{{ x.value }}</strong></div></section>
    </div>
  </div>
</template>
<script setup>
import { getResearchAnalytics } from '@/api/research'
const loading=ref(false),data=reactive({})
async function load(){loading.value=true;try{const r=await getResearchAnalytics();Object.assign(data,r.data||{})}finally{loading.value=false}}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
function projectStatus(s){return ({PLANNING:'规划中',ACTIVE:'执行中',SUSPENDED:'暂停',CLOSING:'结项中',CLOSED:'已结项',TERMINATED:'已终止'})[s]||s}
function proposalStatus(s){return ({DRAFT:'草稿',UNDER_REVIEW:'评审中',APPROVED:'已批准',REJECTED:'未通过',REVISION_REQUIRED:'退回修改'})[s]||s}
onMounted(load)
</script>
<style scoped lang="scss">
.analytics{display:flex;flex-direction:column;gap:10px}.finance{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);overflow:hidden}.finance>div{padding:12px;border-right:1px solid var(--rf-border);display:flex;flex-direction:column;gap:4px}.finance>div:last-child{border-right:0}.finance span{font-size:11px;color:var(--rf-text-muted)}.finance strong{font-size:20px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.panel{padding:12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface)}.panel h3{margin:0 0 8px;font-size:13px}.stat-row{min-height:36px;border-top:1px solid var(--rf-border);display:flex;align-items:center;justify-content:space-between;font-size:12px}.stat-row strong{font-variant-numeric:tabular-nums}@media(max-width:700px){.finance{grid-template-columns:1fr 1fr}.grid{grid-template-columns:1fr}}
</style>
