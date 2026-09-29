<template>
  <div class="dense-page" v-loading="loading">
    <div class="summary-bar"><strong>{{ risks.length }}</strong><span>条开放风险</span><span class="hint">概率 × 影响形成确定性风险等级，AI 只负责解释。</span></div>
    <div class="table-shell">
      <el-table :data="risks" size="small" stripe @row-click="row=>router.push('/research/projects/'+row.projectId)">
        <el-table-column label="等级" width="95"><template #default="{row}"><span :class="['risk',row.riskLevel.toLowerCase()]">{{ riskText(row.riskLevel) }}</span></template></el-table-column>
        <el-table-column prop="riskNo" label="风险编号" width="115" />
        <el-table-column prop="title" label="风险" min-width="220" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="P × I" width="90" align="center"><template #default="{row}">{{ row.probability }} × {{ row.impact }}</template></el-table-column>
        <el-table-column prop="score" label="分值" width="70" align="center" />
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column prop="source" label="来源" width="80" />
        <el-table-column prop="status" label="状态" width="95" />
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { listResearchRisks } from '@/api/research'
const router=useRouter(),loading=ref(false),risks=ref([])
async function load(){loading.value=true;try{const r=await listResearchRisks();risks.value=r.data||[]}finally{loading.value=false}}
function riskText(v){return ({LOW:'低',MEDIUM:'中',HIGH:'高',CRITICAL:'严重'})[v]||v}
onMounted(load)
</script>
<style scoped lang="scss">
.dense-page{display:flex;flex-direction:column;gap:10px}.summary-bar{min-height:48px;padding:8px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;gap:7px;color:var(--rf-text-muted);font-size:12px}.summary-bar strong{font-size:20px;color:var(--rf-danger)}.hint{margin-left:auto}.table-shell{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.risk{display:inline-flex;min-height:23px;padding:0 7px;border-radius:999px;align-items:center;font-size:11px;font-weight:700}.risk.low{background:var(--rf-success-soft);color:var(--rf-success)}.risk.medium{background:var(--rf-warning-soft);color:var(--rf-warning)}.risk.high,.risk.critical{background:var(--rf-danger-soft);color:var(--rf-danger)}
</style>
