<template>
  <div v-loading="loading" class="risk-page">
    <div class="risk-explain">
      <div class="icon"><el-icon><Warning /></el-icon></div>
      <div><strong>风险判断基于确定性规则</strong><p>当前 MVP 比较时间进度、任务完成度和预算执行率。AI 可用于解释风险，但不决定项目业务状态。</p></div>
    </div>
    <div v-if="risks.length" class="risk-grid">
      <article v-for="p in risks" :key="p.projectId" class="risk-card" @click="router.push(`/research/projects/${p.projectId}`)">
        <div class="risk-card-head"><span :class="['risk-tag',p.riskLevel?.toLowerCase()]">{{ p.riskLevel==='HIGH'?'高风险':'需关注' }}</span><span>{{ p.projectNo }}</span></div>
        <h3>{{ p.projectName }}</h3>
        <p>{{ p.riskReason }}</p>
        <div class="risk-metrics">
          <div><span>项目进度</span><strong>{{ p.progress || 0 }}%</strong></div>
          <div><span>预算执行</span><strong>{{ budgetRate(p) }}%</strong></div>
          <div><span>计划结束</span><strong>{{ p.plannedEndDate }}</strong></div>
        </div>
        <div class="risk-footer">查看项目详情 <el-icon><ArrowRight /></el-icon></div>
      </article>
    </div>
    <el-empty v-else description="当前没有需要关注的风险项目" />
  </div>
</template>
<script setup>
import { listResearchRisks } from '@/api/research'
const router=useRouter(); const loading=ref(false); const risks=ref([])
async function load(){loading.value=true;try{const res=await listResearchRisks();risks.value=res.data||res||[]}finally{loading.value=false}}
function budgetRate(p){const t=Number(p.totalBudget||0);return t?Math.round(Number(p.usedBudget||0)*100/t):0}
onMounted(load)
</script>
<style scoped lang="scss">
.risk-page{display:flex;flex-direction:column;gap:18px}.risk-explain{display:flex;gap:14px;align-items:center;background:#fff9f5;border:1px solid #ffe4d7;border-radius:14px;padding:18px}.risk-explain .icon{width:42px;height:42px;border-radius:12px;background:#ffeadf;color:#d15e39;display:grid;place-items:center;font-size:20px}.risk-explain strong{font-size:14px}.risk-explain p{margin:5px 0 0;color:#8f756b;font-size:12px}.risk-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.risk-card{background:#fff;border:1px solid #e7ecf2;border-left:4px solid #f2994a;border-radius:14px;padding:20px;cursor:pointer}.risk-card:has(.risk-tag.high){border-left-color:#eb5757}.risk-card-head{display:flex;justify-content:space-between;color:#9aa4b2;font-size:11px}.risk-tag{padding:4px 8px;border-radius:99px;background:#fff0df;color:#bd6b11}.risk-tag.high{background:#ffe7e7;color:#c83f3f}.risk-card h3{margin:14px 0 7px;font-size:16px}.risk-card>p{color:#886d65;font-size:12px;min-height:38px}.risk-metrics{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:18px}.risk-metrics div{background:#f8fafc;padding:11px;border-radius:9px;display:flex;flex-direction:column;gap:5px}.risk-metrics span{font-size:10px;color:#96a1b0}.risk-metrics strong{font-size:12px}.risk-footer{margin-top:16px;border-top:1px solid #edf0f4;padding-top:13px;color:#397fc8;font-size:11px;display:flex;align-items:center;gap:5px}@media(max-width:850px){.risk-grid{grid-template-columns:1fr}}
</style>
