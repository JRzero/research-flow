<template>
  <div class="page" v-loading="loading">
    <div class="overview"><div><span>待评审申请</span><strong>{{ data.proposals?.length||0 }}</strong></div><div><span>待审批变更</span><strong>{{ data.changes?.length||0 }}</strong></div><div><span>待验收</span><strong>{{ data.acceptances?.length||0 }}</strong></div></div>
    <div class="panel"><el-tabs v-model="tab">
      <el-tab-pane :label="'项目申请 '+(data.proposals?.length||0)" name="proposal">
        <el-table :data="data.proposals||[]"><el-table-column prop="proposalNo" label="申请编号" width="160"/><el-table-column prop="title" label="项目名称" min-width="250"/><el-table-column prop="applicantName" label="申请人" width="100"/><el-table-column label="预算" width="110" align="right"><template #default="{row}">¥{{ money(row.requestedBudget) }}</template></el-table-column><el-table-column prop="submittedAt" label="提交时间" width="160"/><el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button text type="primary" @click="reviewProposal(row,true)">批准</el-button><el-button text type="danger" @click="reviewProposal(row,false)">拒绝</el-button></template></el-table-column></el-table>
        <el-empty v-if="!data.proposals?.length" description="暂无待评审申请"/>
      </el-tab-pane>
      <el-tab-pane :label="'项目变更 '+(data.changes?.length||0)" name="change">
        <el-table :data="data.changes||[]"><el-table-column prop="changeNo" label="变更编号" width="165"/><el-table-column prop="projectName" label="项目" min-width="220"/><el-table-column prop="title" label="变更事项" min-width="220"/><el-table-column prop="applicantName" label="申请人" width="100"/><el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button text type="primary" @click="reviewChange(row,true)">批准</el-button><el-button text type="danger" @click="reviewChange(row,false)">拒绝</el-button></template></el-table-column></el-table>
        <el-empty v-if="!data.changes?.length" description="暂无待审批变更"/>
      </el-tab-pane>
      <el-tab-pane :label="'项目验收 '+(data.acceptances?.length||0)" name="acceptance">
        <el-table :data="data.acceptances||[]"><el-table-column prop="acceptanceNo" label="验收编号" width="165"/><el-table-column prop="projectName" label="项目" min-width="260"/><el-table-column prop="applicantName" label="申请人" width="100"/><el-table-column prop="submittedAt" label="提交时间" width="160"/><el-table-column label="操作" width="170" fixed="right"><template #default="{row}"><el-button text type="success" @click="reviewAccept(row,true)">验收通过</el-button><el-button text type="danger" @click="reviewAccept(row,false)">退回</el-button></template></el-table-column></el-table>
        <el-empty v-if="!data.acceptances?.length" description="暂无待验收项目"/>
      </el-tab-pane>
    </el-tabs></div>
  </div>
</template>
<script setup>
import { getApprovalCenter,approveProposal,rejectProposal,approveChange,rejectChange,reviewAcceptance } from '@/api/research'
const {proxy}=getCurrentInstance(),loading=ref(false),tab=ref('proposal'),data=reactive({proposals:[],changes:[],acceptances:[]})
async function load(){loading.value=true;try{const r=await getApprovalCenter();Object.assign(data,r.data||{})}finally{loading.value=false}}
async function ask(title,message){try{const {value}=await ElMessageBox.prompt(message,title,{inputType:'textarea',confirmButtonText:'确认',cancelButtonText:'取消'});return value||''}catch{return null}}
async function reviewProposal(row,ok){const c=await ask(ok?'批准项目申请':'拒绝项目申请',ok?'填写审批意见，可留空':'请输入拒绝原因');if(c===null)return;if(ok)await approveProposal(row.proposalId,{comment:c});else await rejectProposal(row.proposalId,c);proxy.$modal.msgSuccess(ok?'已批准并生成正式项目':'已拒绝申请');load()}
async function reviewChange(row,ok){const c=await ask(ok?'批准变更':'拒绝变更',ok?'填写审批意见':'请输入拒绝原因');if(c===null)return;if(ok)await approveChange(row.projectId,row.changeId,c);else await rejectChange(row.projectId,row.changeId,c);proxy.$modal.msgSuccess(ok?'变更已批准，待项目侧应用':'变更已拒绝');load()}
async function reviewAccept(row,ok){const c=await ask(ok?'验收通过':'退回验收',ok?'填写验收意见':'填写退回要求');if(c===null)return;await reviewAcceptance(row.projectId,ok?'approve':'return',c);proxy.$modal.msgSuccess(ok?'验收已通过，项目进入结项阶段':'验收已退回');load()}
function money(v){return Number(v||0).toLocaleString()}
onMounted(load)
</script>
<style scoped lang="scss">
.page{display:flex;flex-direction:column;gap:10px}.overview{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.overview>div{padding:10px 12px;border:1px solid var(--rf-border);border-radius:9px;background:var(--rf-surface);display:flex;justify-content:space-between;align-items:center}.overview span{font-size:11px;color:var(--rf-text-muted)}.overview strong{font-size:20px}.panel{padding:0 12px 10px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface)}:deep(.el-tabs__header){margin-bottom:8px}:deep(.el-tabs__item){height:44px;font-size:12px}@media(max-width:600px){.overview{grid-template-columns:1fr 1fr 1fr}.overview>div{align-items:flex-start;flex-direction:column}.overview strong{font-size:17px}}
</style>
