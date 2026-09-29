<template>
  <div class="dense-page" v-loading="loading">
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="项目名称 / 编号" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </div>
      <div class="toolbar-right"><span class="count">{{ projects.length }} 个正式项目</span><el-button type="primary" plain @click="router.push('/research/proposals')">项目申请</el-button></div>
    </div>

    <div class="table-shell">
      <el-table :data="projects" size="small" stripe @row-click="row=>open(row.projectId)">
        <el-table-column prop="projectNo" label="项目编号" width="145" />
        <el-table-column prop="projectName" label="项目名称" min-width="260" show-overflow-tooltip />
        <el-table-column prop="piName" label="PI" width="90" />
        <el-table-column prop="deptName" label="部门" width="150" show-overflow-tooltip />
        <el-table-column label="进度" width="150">
          <template #default="{row}"><div class="progress-cell"><el-progress :percentage="row.progress||0" :stroke-width="6" :show-text="false" /><span>{{ row.progress||0 }}%</span></div></template>
        </el-table-column>
        <el-table-column label="预算执行" width="135" align="right">
          <template #default="{row}">{{ percent(row.usedBudget,row.currentBudget) }}%</template>
        </el-table-column>
        <el-table-column label="治理" width="135">
          <template #default="{row}">
            <span v-if="row.highRiskCount" class="mini-alert danger">{{ row.highRiskCount }} 高风险</span>
            <span v-else-if="row.openIssueCount" class="mini-alert warning">{{ row.openIssueCount }} 问题</span>
            <span v-else class="mini-alert ok">正常</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}"><span :class="['status',row.status.toLowerCase()]">{{ statusText(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="" width="72" fixed="right"><template #default="{row}"><el-button text type="primary" @click.stop="open(row.projectId)">进入</el-button></template></el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { Search } from '@element-plus/icons-vue'
import { listResearchProjects } from '@/api/research'
const router=useRouter(),loading=ref(false),projects=ref([]),query=reactive({keyword:'',status:''})
const statuses=[{value:'PLANNING',label:'规划中'},{value:'ACTIVE',label:'执行中'},{value:'SUSPENDED',label:'暂停'},{value:'CLOSING',label:'结项中'},{value:'CLOSED',label:'已结项'},{value:'TERMINATED',label:'已终止'}]
async function load(){loading.value=true;try{const r=await listResearchProjects(query);projects.value=r.data||[]}finally{loading.value=false}}
function open(id){router.push('/research/projects/'+id)}
function statusText(s){return ({PLANNING:'规划中',ACTIVE:'执行中',SUSPENDED:'暂停',CLOSING:'结项中',CLOSED:'已结项',TERMINATED:'已终止'})[s]||s}
function percent(a,b){const x=Number(a||0),y=Number(b||0);return y?Math.min(999,Math.round(x*100/y)):0}
onMounted(load)
</script>
<style scoped lang="scss">
.dense-page{display:flex;flex-direction:column;gap:10px}.toolbar{min-height:52px;padding:8px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;justify-content:space-between;gap:12px}.filters{display:flex;gap:8px}.filters .el-input{width:280px}.filters .el-select{width:150px}.toolbar-right{display:flex;align-items:center;gap:10px}.count{color:var(--rf-text-muted);font-size:12px}.table-shell{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.progress-cell{display:grid;grid-template-columns:1fr 34px;gap:7px;align-items:center;font-size:11px}.status,.mini-alert{display:inline-flex;align-items:center;min-height:23px;padding:0 7px;border-radius:999px;font-size:11px;font-weight:650}.status{background:var(--rf-surface-subtle);color:var(--rf-text-secondary)}.status.active{background:var(--rf-primary-soft);color:var(--rf-primary)}.status.closed{background:var(--rf-success-soft);color:var(--rf-success)}.status.closing,.status.planning{background:var(--rf-warning-soft);color:var(--rf-warning)}.mini-alert.danger{background:var(--rf-danger-soft);color:var(--rf-danger)}.mini-alert.warning{background:var(--rf-warning-soft);color:var(--rf-warning)}.mini-alert.ok{background:var(--rf-success-soft);color:var(--rf-success)}@media(max-width:700px){.toolbar,.filters{align-items:stretch;flex-direction:column}.filters .el-input,.filters .el-select{width:100%}}
</style>
