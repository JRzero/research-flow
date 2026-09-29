<template>
  <div class="page" v-loading="loading">
    <div class="toolbar"><div class="filters"><el-input v-model="query.keyword" placeholder="项目名称或编号" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load"/><el-select v-model="query.status" clearable placeholder="全部状态" @change="load"><el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value"/></el-select></div><div class="count"><strong>{{ rows.length }}</strong> 个正式项目</div></div>
    <div class="panel">
      <el-table :data="rows" @row-click="row=>router.push('/research/projects/'+row.projectId)">
        <el-table-column prop="projectNo" label="项目编号" width="145"/>
        <el-table-column label="项目" min-width="250"><template #default="{row}"><strong class="title">{{ row.projectName }}</strong><div class="sub">{{ row.piName }} · {{ row.deptName||'-' }}</div></template></el-table-column>
        <el-table-column label="状态" width="92"><template #default="{row}"><span :class="['status',tone(row.status)]">{{ statusText(row.status) }}</span></template></el-table-column>
        <el-table-column label="进度" width="150"><template #default="{row}"><div class="progress"><el-progress :percentage="row.progress||0" :show-text="false" :stroke-width="6"/><b>{{ row.progress||0 }}%</b></div></template></el-table-column>
        <el-table-column label="预算" width="110" align="right"><template #default="{row}">¥{{ compact(row.currentBudget) }}</template></el-table-column>
        <el-table-column label="风险/问题" width="105" align="center"><template #default="{row}"><span :class="{alert:(row.openRiskCount||0)+(row.openIssueCount||0)>0}">{{ row.openRiskCount||0 }} / {{ row.openIssueCount||0 }}</span></template></el-table-column>
        <el-table-column prop="plannedEndDate" label="计划结束" width="110"/>
        <el-table-column label="" width="48"><template #default><el-icon><ArrowRight/></el-icon></template></el-table-column>
      </el-table><el-empty v-if="!rows.length&&!loading" description="暂无正式项目"/>
    </div>
  </div>
</template>
<script setup>
import { Search } from '@element-plus/icons-vue'
import { listResearchProjects } from '@/api/research'
const router=useRouter(),loading=ref(false),rows=ref([]),query=reactive({keyword:'',status:''})
const statuses=[['PLANNING','计划中'],['ACTIVE','执行中'],['SUSPENDED','已暂停'],['CLOSING','结项中'],['CLOSED','已结项'],['TERMINATED','已终止']].map(([value,label])=>({value,label}))
async function load(){loading.value=true;try{const r=await listResearchProjects(query);rows.value=r.data||[]}finally{loading.value=false}}
function statusText(s){return ({PLANNING:'计划中',ACTIVE:'执行中',SUSPENDED:'已暂停',CLOSING:'结项中',CLOSED:'已结项',TERMINATED:'已终止'})[s]||s}
function tone(s){return s==='ACTIVE'?'success':s==='PLANNING'||s==='CLOSING'?'warning':s==='TERMINATED'?'danger':'neutral'}
function compact(v){const n=Number(v||0);return n>=10000?(n/10000).toFixed(n%10000===0?0:1)+'万':n.toLocaleString()}
onMounted(load)
</script>
<style scoped lang="scss">
.page{display:flex;flex-direction:column;gap:10px}.toolbar{padding:10px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;justify-content:space-between;gap:12px}.filters{display:flex;gap:8px}.filters .el-input{width:300px}.filters .el-select{width:150px}.count{font-size:11px;color:var(--rf-text-muted)}.count strong{font-size:18px;color:var(--rf-text)}.panel{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.title{font-size:13px}.sub{margin-top:2px;color:var(--rf-text-muted);font-size:11px}.status{padding:3px 7px;border-radius:999px;background:var(--rf-surface-subtle);font-size:11px}.status.success{background:var(--rf-success-soft);color:var(--rf-success)}.status.warning{background:var(--rf-warning-soft);color:var(--rf-warning)}.status.danger{background:var(--rf-danger-soft);color:var(--rf-danger)}.progress{display:grid;grid-template-columns:1fr 34px;gap:7px;align-items:center}.progress b{font-size:11px}.alert{color:var(--rf-danger);font-weight:700}
@media(max-width:700px){.toolbar,.filters{align-items:stretch;flex-direction:column}.filters .el-input,.filters .el-select{width:100%}}
</style>
