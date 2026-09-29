<template>
  <div class="dense-page" v-loading="loading">
    <div class="summary-bar"><strong>{{ queue.length }}</strong><span>项待处理业务</span><span class="hint">申报评审、项目变更和验收统一进入这里。</span></div>
    <div class="table-shell">
      <el-table :data="queue" size="small" stripe>
        <el-table-column label="类型" width="110"><template #default="{row}"><span class="type">{{ typeText(row.businessType) }}</span></template></el-table-column>
        <el-table-column prop="businessNo" label="业务编号" width="155" />
        <el-table-column prop="title" label="事项" min-width="260" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="提交人" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="165" />
        <el-table-column label="状态" width="100"><template #default="{row}"><span class="status">{{ statusText(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.businessType==='PROPOSAL'" text type="primary" @click="reviewProposal(row,true)">通过</el-button>
            <el-button v-if="row.businessType==='PROPOSAL'" text type="danger" @click="reviewProposal(row,false)">驳回</el-button>
            <el-button v-if="row.businessType==='CHANGE_REQUEST'" text type="primary" @click="reviewChange(row,true)">批准</el-button>
            <el-button v-if="row.businessType==='CHANGE_REQUEST'" text type="danger" @click="reviewChange(row,false)">拒绝</el-button>
            <el-button v-if="row.businessType==='ACCEPTANCE'" text type="success" @click="reviewAcceptance(row,true)">验收通过</el-button>
            <el-button v-if="row.businessType==='ACCEPTANCE'" text type="danger" @click="reviewAcceptance(row,false)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { listResearchApprovals,approveResearchProposal,rejectResearchProposal,approveResearchChange,rejectResearchChange,approveResearchAcceptance,rejectResearchAcceptance } from '@/api/research'
const {proxy}=getCurrentInstance(),loading=ref(false),queue=ref([])
async function load(){loading.value=true;try{const r=await listResearchApprovals();queue.value=r.data||[]}finally{loading.value=false}}
async function ask(title){try{const {value}=await ElMessageBox.prompt('填写处理意见（可选）',title,{confirmButtonText:'确认',cancelButtonText:'取消',inputType:'textarea'});return value||''}catch{return null}}
async function reviewProposal(row,ok){const c=await ask(ok?'通过申报':'驳回申报');if(c===null)return;ok?await approveResearchProposal(row.businessId,c):await rejectResearchProposal(row.businessId,c);proxy.$modal.msgSuccess('已处理');load()}
async function reviewChange(row,ok){const c=await ask(ok?'批准变更':'拒绝变更');if(c===null)return;ok?await approveResearchChange(row.projectId,row.businessId,c):await rejectResearchChange(row.projectId,row.businessId,c);proxy.$modal.msgSuccess('已处理');load()}
async function reviewAcceptance(row,ok){const c=await ask(ok?'验收通过':'退回验收');if(c===null)return;ok?await approveResearchAcceptance(row.projectId,c):await rejectResearchAcceptance(row.projectId,c);proxy.$modal.msgSuccess('已处理');load()}
function typeText(t){return ({PROPOSAL:'项目申报',CHANGE_REQUEST:'项目变更',ACCEPTANCE:'项目验收'})[t]||t}
function statusText(s){return ({SUBMITTED:'待处理',UNDER_REVIEW:'评审中',ASSESSING:'评估中'})[s]||s}
onMounted(load)
</script>
<style scoped lang="scss">
.dense-page{display:flex;flex-direction:column;gap:10px}.summary-bar{min-height:48px;padding:8px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;gap:7px;font-size:12px;color:var(--rf-text-muted)}.summary-bar strong{color:var(--rf-primary);font-size:20px}.hint{margin-left:auto}.table-shell{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.type,.status{display:inline-flex;min-height:23px;padding:0 7px;border-radius:999px;align-items:center;font-size:11px;font-weight:650}.type{background:var(--rf-primary-soft);color:var(--rf-primary)}.status{background:var(--rf-warning-soft);color:var(--rf-warning)}
</style>
