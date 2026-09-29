<template>
  <div class="page" v-loading="loading">
    <div class="overview">
      <div><span>待评审申请</span><strong>{{ data.proposals?.length||0 }}</strong></div>
      <div><span>待审批变更</span><strong>{{ data.changes?.length||0 }}</strong></div>
      <div><span>待验收</span><strong>{{ data.acceptances?.length||0 }}</strong></div>
    </div>

    <div class="panel">
      <el-tabs v-model="tab">
        <el-tab-pane :label="'项目申请 '+(data.proposals?.length||0)" name="proposal">
          <el-table :data="data.proposals||[]">
            <el-table-column prop="proposalNo" label="申请编号" width="160"/>
            <el-table-column prop="title" label="项目名称" min-width="250"/>
            <el-table-column prop="applicantName" label="申请人" width="100"/>
            <el-table-column label="申请预算" width="120" align="right"><template #default="{row}">¥{{ money(row.requestedBudget) }}</template></el-table-column>
            <el-table-column prop="submittedAt" label="提交时间" width="160"/>
            <el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button text type="primary" @click="openAward(row)">批复</el-button><el-button text type="danger" @click="rejectProposalRow(row)">拒绝</el-button></template></el-table-column>
          </el-table>
          <el-empty v-if="!data.proposals?.length" description="暂无待评审申请"/>
        </el-tab-pane>

        <el-tab-pane :label="'项目变更 '+(data.changes?.length||0)" name="change">
          <el-table :data="data.changes||[]">
            <el-table-column prop="changeNo" label="变更编号" width="165"/>
            <el-table-column prop="projectName" label="项目" min-width="220"/>
            <el-table-column prop="title" label="变更事项" min-width="220"/>
            <el-table-column prop="applicantName" label="申请人" width="100"/>
            <el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button text type="primary" @click="reviewChange(row,true)">批准</el-button><el-button text type="danger" @click="reviewChange(row,false)">拒绝</el-button></template></el-table-column>
          </el-table>
          <el-empty v-if="!data.changes?.length" description="暂无待审批变更"/>
        </el-tab-pane>

        <el-tab-pane :label="'项目验收 '+(data.acceptances?.length||0)" name="acceptance">
          <el-table :data="data.acceptances||[]">
            <el-table-column prop="acceptanceNo" label="验收编号" width="165"/>
            <el-table-column prop="projectName" label="项目" min-width="260"/>
            <el-table-column prop="applicantName" label="申请人" width="100"/>
            <el-table-column prop="submittedAt" label="提交时间" width="160"/>
            <el-table-column label="操作" width="170" fixed="right"><template #default="{row}"><el-button text type="success" @click="reviewAccept(row,true)">验收通过</el-button><el-button text type="danger" @click="reviewAccept(row,false)">退回</el-button></template></el-table-column>
          </el-table>
          <el-empty v-if="!data.acceptances?.length" description="暂无待验收项目"/>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="awardVisible" title="立项批复" width="760px" destroy-on-close>
      <div class="award-note">申请信息保留不变，以下字段形成正式 Award，并作为项目计划的批准来源。</div>
      <el-form :model="awardForm" label-position="top" class="award-form">
        <el-form-item label="批准项目名称"><el-input v-model="awardForm.approvedTitle"/></el-form-item>
        <div class="grid3">
          <el-form-item label="批准开始日期"><el-date-picker v-model="awardForm.approvedStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
          <el-form-item label="批准结束日期"><el-date-picker v-model="awardForm.approvedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
          <el-form-item label="批准预算"><el-input-number v-model="awardForm.approvedBudget" :min="0" :step="10000" style="width:100%"/></el-form-item>
        </div>
        <div class="compare-line"><span>申请预算 ¥{{ money(selectedProposal.requestedBudget) }}</span><span>→</span><strong>批准预算 ¥{{ money(awardForm.approvedBudget) }}</strong></div>
        <div class="grid2">
          <el-form-item label="批准范围"><el-input v-model="awardForm.approvedScope" type="textarea" :rows="3"/></el-form-item>
          <el-form-item label="批准目标"><el-input v-model="awardForm.approvedObjectives" type="textarea" :rows="3"/></el-form-item>
        </div>
        <el-form-item label="批准成果"><el-input v-model="awardForm.approvedOutputs" type="textarea" :rows="2" placeholder="正式批复成果要求"/></el-form-item>
        <el-form-item label="审批意见"><el-input v-model="awardForm.comment" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="awardVisible=false">取消</el-button><el-button type="primary" :loading="approving" @click="confirmAward">批准并立项</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getApprovalCenter,getProposal,approveProposal,rejectProposal,approveChange,rejectChange,reviewAcceptance } from '@/api/research'

const {proxy}=getCurrentInstance()
const loading=ref(false),approving=ref(false),tab=ref('proposal'),awardVisible=ref(false)
const data=reactive({proposals:[],changes:[],acceptances:[]})
const selectedProposal=reactive({})
const awardForm=reactive({approvedTitle:'',approvedStartDate:'',approvedEndDate:'',approvedBudget:0,approvedScope:'',approvedObjectives:'',approvedOutputs:'',comment:''})

async function load(){loading.value=true;try{const r=await getApprovalCenter();Object.assign(data,r.data||{})}finally{loading.value=false}}
async function ask(title,message){try{const {value}=await ElMessageBox.prompt(message,title,{inputType:'textarea',confirmButtonText:'确认',cancelButtonText:'取消'});return value||''}catch{return null}}

async function openAward(row){
  const r=await getProposal(row.proposalId)
  const d=r.data||{}
  Object.keys(selectedProposal).forEach(k=>delete selectedProposal[k])
  Object.assign(selectedProposal,d)
  Object.assign(awardForm,{
    approvedTitle:d.title||'',
    approvedStartDate:d.plannedStartDate||'',
    approvedEndDate:d.plannedEndDate||'',
    approvedBudget:Number(d.requestedBudget||0),
    approvedScope:d.projectScope||'',
    approvedObjectives:d.objectives||'',
    approvedOutputs:(d.expectedOutputs||[]).map(o=>o.name||o.outputType).join('、'),
    comment:''
  })
  awardVisible.value=true
}
async function confirmAward(){
  if(!awardForm.approvedTitle||!awardForm.approvedStartDate||!awardForm.approvedEndDate||Number(awardForm.approvedBudget)<=0)return proxy.$modal.msgWarning('请填写完整批复信息')
  approving.value=true
  try{
    await approveProposal(selectedProposal.proposalId,{...awardForm})
    proxy.$modal.msgSuccess('已生成立项批复和正式项目')
    awardVisible.value=false
    load()
  }finally{approving.value=false}
}
async function rejectProposalRow(row){const c=await ask('拒绝项目申请','请输入拒绝原因');if(c===null)return;await rejectProposal(row.proposalId,c);proxy.$modal.msgSuccess('已拒绝申请');load()}
async function reviewChange(row,ok){const c=await ask(ok?'批准变更':'拒绝变更',ok?'填写审批意见':'请输入拒绝原因');if(c===null)return;if(ok)await approveChange(row.projectId,row.changeId,c);else await rejectChange(row.projectId,row.changeId,c);proxy.$modal.msgSuccess(ok?'变更已批准，待项目侧应用':'变更已拒绝');load()}
async function reviewAccept(row,ok){const c=await ask(ok?'验收通过':'退回验收',ok?'填写验收意见':'填写退回要求');if(c===null)return;await reviewAcceptance(row.projectId,ok?'approve':'return',c);proxy.$modal.msgSuccess(ok?'验收已通过，项目进入结项阶段':'验收已退回');load()}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
onMounted(load)
</script>

<style scoped lang="scss">
.page{display:flex;flex-direction:column;gap:8px}.overview{display:grid;grid-template-columns:repeat(3,1fr);gap:7px}.overview>div{padding:8px 10px;border:1px solid var(--rf-border);border-radius:8px;background:var(--rf-surface);display:flex;justify-content:space-between;align-items:center}.overview span{font-size:10px;color:var(--rf-text-muted)}.overview strong{font-size:18px}.panel{padding:0 10px 8px;border:1px solid var(--rf-border);border-radius:9px;background:var(--rf-surface)}:deep(.el-tabs__header){margin-bottom:6px}:deep(.el-tabs__item){height:40px;font-size:11px}.award-note{margin-bottom:10px;padding:8px 10px;border-radius:8px;background:var(--rf-primary-soft);color:var(--rf-text-secondary);font-size:11px;line-height:1.5}.grid2{display:grid;grid-template-columns:1fr 1fr;gap:10px}.grid3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px}.compare-line{margin:-2px 0 10px;padding:7px 10px;border:1px solid var(--rf-border);border-radius:8px;display:flex;align-items:center;gap:10px;background:var(--rf-surface-subtle);font-size:11px;color:var(--rf-text-muted)}.compare-line strong{color:var(--rf-primary)}@media(max-width:700px){.grid2,.grid3{grid-template-columns:1fr}.overview{grid-template-columns:1fr 1fr 1fr}.overview>div{align-items:flex-start;flex-direction:column}.overview strong{font-size:16px}}
</style>
