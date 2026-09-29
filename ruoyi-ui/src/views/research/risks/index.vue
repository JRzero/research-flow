<template>
  <div class="page" v-loading="loading">
    <div class="legend"><div><strong>Risk</strong><span>尚未发生，需要提前应对</span></div><div><strong>Issue</strong><span>已经发生，需要跟踪解决</span></div></div>
    <div class="panel"><el-tabs v-model="tab">
      <el-tab-pane :label="'风险 '+risks.length" name="risk">
        <el-table :data="risks" @row-click="row=>router.push('/research/projects/'+row.projectId)">
          <el-table-column label="等级" width="85"><template #default="{row}"><span :class="['level',row.riskLevel?.toLowerCase()]">{{ row.riskLevel }}</span></template></el-table-column>
          <el-table-column prop="riskNo" label="编号" width="105"/><el-table-column label="风险" min-width="250"><template #default="{row}"><strong>{{ row.title }}</strong><div class="sub">{{ row.description||'-' }}</div></template></el-table-column>
          <el-table-column label="项目" min-width="210"><template #default="{row}">{{ row.projectName }}<div class="sub">{{ row.projectNo }}</div></template></el-table-column><el-table-column prop="ownerName" label="责任人" width="95"/><el-table-column label="状态" width="95"><template #default="{row}">{{ riskStatus(row.status) }}</template></el-table-column>
        </el-table><el-empty v-if="!risks.length" description="暂无风险"/>
      </el-tab-pane>
      <el-tab-pane :label="'问题 '+issues.length" name="issue">
        <el-table :data="issues" @row-click="row=>router.push('/research/projects/'+row.projectId)">
          <el-table-column label="严重度" width="85"><template #default="{row}"><span :class="['level',row.severity?.toLowerCase()]">{{ row.severity }}</span></template></el-table-column><el-table-column prop="issueNo" label="编号" width="105"/>
          <el-table-column label="问题" min-width="250"><template #default="{row}"><strong>{{ row.title }}</strong><div class="sub">{{ row.description||'-' }}</div></template></el-table-column><el-table-column prop="projectName" label="项目" min-width="210"/><el-table-column prop="ownerName" label="责任人" width="95"/><el-table-column prop="dueDate" label="目标解决" width="110"/><el-table-column label="状态" width="95"><template #default="{row}">{{ issueStatus(row.status) }}</template></el-table-column>
        </el-table><el-empty v-if="!issues.length" description="暂无问题"/>
      </el-tab-pane>
    </el-tabs></div>
  </div>
</template>
<script setup>
import { getGovernance } from '@/api/research'
const router=useRouter(),loading=ref(false),tab=ref('risk'),risks=ref([]),issues=ref([])
async function load(){loading.value=true;try{const r=await getGovernance();risks.value=r.data?.risks||[];issues.value=r.data?.issues||[]}finally{loading.value=false}}
function riskStatus(s){return ({OPEN:'开放',MONITORING:'监控中',OCCURRED:'已发生',CLOSED:'已关闭'})[s]||s}
function issueStatus(s){return ({OPEN:'开放',IN_PROGRESS:'处理中',RESOLVED:'已解决',CLOSED:'已关闭'})[s]||s}
onMounted(load)
</script>
<style scoped lang="scss">
.page{display:flex;flex-direction:column;gap:10px}.legend{display:flex;gap:8px}.legend>div{flex:1;padding:8px 10px;border:1px solid var(--rf-border);border-radius:8px;background:var(--rf-surface);display:flex;gap:8px;align-items:center}.legend strong{font-size:11px}.legend span{font-size:10px;color:var(--rf-text-muted)}.panel{padding:0 12px 10px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface)}:deep(.el-tabs__header){margin-bottom:8px}.sub{max-width:420px;margin-top:2px;color:var(--rf-text-muted);font-size:10px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.level{padding:3px 6px;border-radius:999px;background:var(--rf-surface-subtle);font-size:9px;font-weight:700}.level.medium{background:var(--rf-warning-soft);color:var(--rf-warning)}.level.high{background:var(--rf-danger-soft);color:var(--rf-danger)}.level.critical{background:var(--rf-danger);color:white}
</style>
