<template>
  <div class="page" v-loading="loading">
    <div class="toolbar">
      <div class="filters"><el-input v-model="query.keyword" clearable placeholder="申请名称或编号" :prefix-icon="Search" @keyup.enter="load" @clear="load"/><el-select v-model="query.status" clearable placeholder="全部状态" @change="load"><el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value"/></el-select></div>
      <el-button v-if="canCreate" type="primary" @click="openCreate"><el-icon><Plus/></el-icon>新建申请</el-button>
    </div>
    <div class="summary"><span>申请与正式项目分离，批准结果通过 Award 固化。</span><strong>{{ rows.length }} 项</strong></div>
    <div class="panel">
      <el-table :data="rows" @row-click="openDetail">
        <el-table-column prop="proposalNo" label="申请编号" width="165"/>
        <el-table-column label="项目名称" min-width="260"><template #default="{row}"><strong class="title">{{ row.title }}</strong><div class="sub">{{ row.applicantName }} · {{ row.deptName||'-' }}</div></template></el-table-column>
        <el-table-column label="申请预算" width="120" align="right"><template #default="{row}">¥{{ money(row.requestedBudget) }}</template></el-table-column>
        <el-table-column prop="plannedEndDate" label="计划结束" width="112"/>
        <el-table-column label="附件" width="72" align="center"><template #default="{row}">{{ row.documentCount||0 }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{row}"><span :class="['status',statusTone(row.status)]">{{ statusText(row.status) }}</span></template></el-table-column>
        <el-table-column label="" width="55"><template #default><el-icon><ArrowRight/></el-icon></template></el-table-column>
      </el-table>
      <el-empty v-if="!rows.length&&!loading" description="暂无项目申请"/>
    </div>

    <el-dialog v-model="formVisible"  :title="form.proposalId ? '编辑科研项目申请' : '新建科研项目申请'" width="860px" destroy-on-close>
      <el-form :model="form" label-position="top" class="compact-form">
        <div class="grid2"><el-form-item label="项目名称"><el-input v-model="form.title"/></el-form-item><el-form-item label="计划周期"><el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%"/></el-form-item></div>
        <div class="grid2"><el-form-item label="申请预算（元）"><el-input-number v-model="form.requestedBudget" :min="0" :step="10000" style="width:100%"/></el-form-item><el-form-item label="成功标准"><el-input v-model="form.successCriteria"/></el-form-item></div>
        <el-form-item label="项目背景"><el-input v-model="form.background" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="研究目标"><el-input v-model="form.objectives" type="textarea" :rows="2"/></el-form-item>
        <div class="grid2"><el-form-item label="项目范围"><el-input v-model="form.projectScope" type="textarea" :rows="2"/></el-form-item><el-form-item label="非项目范围"><el-input v-model="form.outOfScope" type="textarea" :rows="2"/></el-form-item></div>
        <el-form-item label="研究内容"><el-input v-model="form.researchContent" type="textarea" :rows="3"/></el-form-item>
        <div v-if="!form.proposalId" class="section-head"><strong>预期成果</strong><el-button text type="primary" @click="addOutputLine">+ 添加</el-button></div>
        <div v-if="!form.proposalId" v-for="(o,i) in form.expectedOutputs" :key="'o'+i" class="output-row"><el-select v-model="o.outputType"><el-option v-for="t in outputTypes" :key="t" :value="t"/></el-select><el-input v-model="o.name" placeholder="成果名称"/><el-input-number v-model="o.targetQuantity" :min="1" :controls="false"/><el-input v-model="o.targetDescription" placeholder="目标说明"/><el-button text type="danger" @click="form.expectedOutputs.splice(i,1)">删除</el-button></div>
        <div class="section-head"><strong>预算明细</strong><el-button text type="primary" @click="addBudgetLine">+ 添加</el-button></div>
        <div v-for="(line,i) in form.budgetLines" :key="i" class="line-row"><el-input v-model="line.category" placeholder="类别"/><el-input-number v-model="line.amount" :min="0" :controls="false"/><el-input v-model="line.description" placeholder="说明"/><el-button text type="danger" @click="form.budgetLines.splice(i,1)">删除</el-button></div>
        <el-form-item label="申报附件" class="upload-field"><ResearchDocumentUpload v-model="form.documents"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="申请详情" size="620px">
      <template v-if="detail.proposalId">
        <div class="drawer-head"><div><span>{{ detail.proposalNo }}</span><h2>{{ detail.title }}</h2></div><span :class="['status',statusTone(detail.status)]">{{ statusText(detail.status) }}</span></div>
        <div class="info-grid"><div><span>申请人</span><strong>{{ detail.applicantName }}</strong></div><div><span>申请预算</span><strong>¥{{ money(detail.requestedBudget) }}</strong></div><div><span>计划周期</span><strong>{{ detail.plannedStartDate||'-' }} ~ {{ detail.plannedEndDate }}</strong></div><div><span>当前阶段</span><strong>{{ detail.currentPhase }}</strong></div></div>
        <section><h3>研究目标</h3><p>{{ detail.objectives||'-' }}</p></section><section><h3>研究内容</h3><p>{{ detail.researchContent||'-' }}</p></section>
        <section><div class="section-head"><strong>团队</strong><span>{{ detail.members?.length||0 }} 人</span></div><div class="mini-list"><div v-for="m in detail.members" :key="m.memberId"><span>{{ m.userName }}</span><b>{{ m.memberRole }}</b></div></div></section>
        <section><div class="section-head"><strong>预算明细</strong><span>¥{{ money(detail.budgetLines?.reduce((a,b)=>a+Number(b.amount||0),0)) }}</span></div><div class="mini-list"><div v-for="b in detail.budgetLines" :key="b.budgetLineId"><span>{{ b.category }}</span><b>¥{{ money(b.amount) }}</b></div></div></section>
        <section><div class="section-head"><strong>预期成果</strong><span>{{ detail.expectedOutputs?.length||0 }} 项</span></div><div class="mini-list"><div v-for="o in detail.expectedOutputs" :key="o.expectedOutputId"><span>{{ o.name||o.outputType }}</span><b>{{ o.targetQuantity }} 项</b><small>{{ o.targetDescription||'-' }}</small></div></div></section>
        <section><div class="section-head"><strong>申报材料</strong><span>{{ detail.documents?.length||0 }} 个</span></div><div class="mini-list"><a v-for="d in detail.documents" :key="d.documentId" :href="baseUrl+d.storageKey" target="_blank">{{ d.fileName }}</a></div></section>
        <div v-if="validation" class="validation"><strong>{{ validation.valid?'已满足提交条件':'提交前需补充' }}</strong><p v-for="m in validation.missing" :key="m.code">{{ m.message }}</p><p v-for="w in validation.warnings" :key="w.code" class="warn">{{ w.message }}</p></div>
        <div class="drawer-actions"><el-button v-if="editable" @click="openEdit">编辑草稿</el-button><el-button v-if="editable" @click="runValidate">完整性检查</el-button><el-button v-if="editable" type="primary" @click="submit">提交评审</el-button></div>
      </template>
    </el-drawer>
  </div>
</template>
<script setup>
import { Search } from '@element-plus/icons-vue'
import ResearchDocumentUpload from '@/components/ResearchDocumentUpload/index.vue'
import { listProposals,getProposal,createProposal,updateProposal,attachProposalDocument,validateProposal,submitProposal } from '@/api/research'
import useUserStore from '@/store/modules/user'
const {proxy}=getCurrentInstance(),userStore=useUserStore(),baseUrl=import.meta.env.VITE_APP_BASE_API
const loading=ref(false),saving=ref(false),formVisible=ref(false),detailVisible=ref(false),rows=ref([]),detail=reactive({}),validation=ref(null),dateRange=ref([])
const query=reactive({keyword:'',status:''})
const statuses=[['DRAFT','草稿'],['UNDER_REVIEW','评审中'],['APPROVED','已批准'],['REJECTED','已拒绝']].map(([value,label])=>({value,label}))
const canCreate=computed(()=>userStore.roles.some(r=>['admin','research_admin','research_owner'].includes(r)))
const editable=computed(()=>['DRAFT','REVISION_REQUIRED'].includes(detail.status))
const outputTypes=['PAPER','PATENT','SOFTWARE','DATASET','STANDARD','REPORT','PROTOTYPE','OTHER']
const blank=()=>({proposalId:null,title:'',background:'',objectives:'',projectScope:'',outOfScope:'',researchContent:'',successCriteria:'',plannedStartDate:'',plannedEndDate:'',requestedBudget:0,budgetLines:[{category:'设备费',amount:0,description:'',sortOrder:1}],expectedOutputs:[{outputType:'REPORT',name:'研究报告',targetQuantity:1,targetDescription:''}],documents:[]})
const form=reactive(blank())
async function load(){loading.value=true;try{const r=await listProposals(query);rows.value=r.data||[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,blank());dateRange.value=[];formVisible.value=true}
function addBudgetLine(){form.budgetLines.push({category:'其他',amount:0,description:'',sortOrder:form.budgetLines.length+1})}
function addOutputLine(){form.expectedOutputs.push({outputType:'REPORT',name:'',targetQuantity:1,targetDescription:''})}
function openEdit(){Object.assign(form,{...blank(),...detail,budgetLines:(detail.budgetLines||[]).map((x,i)=>({...x,sortOrder:x.sortOrder||i+1})),documents:(detail.documents||[]).map(x=>({...x}))});dateRange.value=[detail.plannedStartDate,detail.plannedEndDate];detailVisible.value=false;formVisible.value=true}
async function save(){if(!form.title||!dateRange.value?.[1])return proxy.$modal.msgWarning('请填写项目名称和计划周期');form.plannedStartDate=dateRange.value[0];form.plannedEndDate=dateRange.value[1];saving.value=true;try{if(form.proposalId){const existing=new Set((detail.documents||[]).map(x=>x.storageKey));await updateProposal(form.proposalId,form);for(const d of form.documents.filter(x=>!existing.has(x.storageKey)))await attachProposalDocument(form.proposalId,d);proxy.$modal.msgSuccess('申请草稿已更新')}else{await createProposal(form);proxy.$modal.msgSuccess('申请草稿已创建')}formVisible.value=false;load()}finally{saving.value=false}}
async function openDetail(row){detailVisible.value=true;validation.value=null;const r=await getProposal(row.proposalId);Object.assign(detail,r.data||{})}
async function runValidate(){const r=await validateProposal(detail.proposalId);validation.value=r.data}
async function submit(){await runValidate();if(!validation.value?.valid)return;await proxy.$modal.confirm('提交后申请将进入评审，确认继续？');await submitProposal(detail.proposalId);proxy.$modal.msgSuccess('已提交评审');detailVisible.value=false;load()}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
function statusText(s){return ({DRAFT:'草稿',UNDER_REVIEW:'评审中',REVISION_REQUIRED:'退回修改',APPROVED:'已批准',REJECTED:'已拒绝',WITHDRAWN:'已撤回'})[s]||s}
function statusTone(s){return s==='APPROVED'?'success':s==='UNDER_REVIEW'?'warning':s==='REJECTED'?'danger':'neutral'}
onMounted(load)
</script>
<style scoped lang="scss">
.page{display:flex;flex-direction:column;gap:10px}.toolbar{padding:10px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;justify-content:space-between;gap:12px}.filters{display:flex;gap:8px}.filters .el-input{width:300px}.filters .el-select{width:150px}.summary{padding:0 2px;display:flex;justify-content:space-between;color:var(--rf-text-muted);font-size:11px}.summary strong{color:var(--rf-text-secondary)}.panel{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.title{font-size:13px;color:var(--rf-text)}.sub{margin-top:2px;font-size:11px;color:var(--rf-text-muted)}.status{display:inline-flex;padding:3px 7px;border-radius:999px;font-size:11px;background:var(--rf-surface-subtle);color:var(--rf-text-secondary)}.status.success{background:var(--rf-success-soft);color:var(--rf-success)}.status.warning{background:var(--rf-warning-soft);color:var(--rf-warning)}.status.danger{background:var(--rf-danger-soft);color:var(--rf-danger)}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:12px}.section-head{min-height:32px;display:flex;align-items:center;justify-content:space-between;color:var(--rf-text-secondary);font-size:12px}.line-row{display:grid;grid-template-columns:130px 150px 1fr auto;gap:6px;margin-bottom:6px}.output-row{display:grid;grid-template-columns:120px 1fr 90px 1fr auto;gap:6px;margin-bottom:6px}.upload-field{margin-top:12px}.drawer-head{display:flex;justify-content:space-between;gap:12px}.drawer-head span{color:var(--rf-text-muted);font-size:11px}.drawer-head h2{margin:4px 0 0;font-size:18px}.info-grid{margin:16px 0;display:grid;grid-template-columns:1fr 1fr;border:1px solid var(--rf-border);border-radius:10px}.info-grid>div{padding:10px 12px;display:flex;flex-direction:column;gap:4px;border-right:1px solid var(--rf-border);border-bottom:1px solid var(--rf-border)}.info-grid>div:nth-child(2n){border-right:0}.info-grid>div:nth-last-child(-n+2){border-bottom:0}.info-grid span{font-size:10px;color:var(--rf-text-muted)}.info-grid strong{font-size:12px}section{margin-top:14px}section h3{margin:0 0 5px;font-size:12px}section p{margin:0;color:var(--rf-text-secondary);font-size:12px;line-height:1.65;white-space:pre-wrap}.mini-list{border:1px solid var(--rf-border);border-radius:8px;overflow:hidden}.mini-list>div,.mini-list>a{min-height:34px;padding:6px 9px;border-top:1px solid var(--rf-border);display:flex;justify-content:space-between;gap:10px;align-items:center;font-size:11px;color:var(--rf-text-secondary);text-decoration:none}.mini-list>*:first-child{border-top:0}.validation{margin-top:14px;padding:10px;border:1px solid var(--rf-border);border-radius:8px;background:var(--rf-surface-subtle);font-size:11px}.validation p{margin:4px 0;color:var(--rf-danger)}.validation .warn{color:var(--rf-warning)}.drawer-actions{position:sticky;bottom:0;margin-top:16px;padding:10px 0;background:var(--rf-surface);display:flex;justify-content:flex-end;gap:8px}
@media(max-width:700px){.toolbar,.filters{flex-direction:column}.filters .el-input,.filters .el-select{width:100%}.grid2{grid-template-columns:1fr}.line-row,.output-row{grid-template-columns:1fr 1fr}.line-row>*:nth-child(3),.output-row>*:nth-child(4){grid-column:1/-1}}
</style>
