<template>
  <div class="dense-page" v-loading="loading">
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="名称 / 申报编号" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <span class="count">{{ proposals.length }} 条</span>
        <el-button v-if="canCreate" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建申请</el-button>
      </div>
    </div>

    <div class="table-shell">
      <el-table :data="proposals" size="small" stripe @row-dblclick="openDetail">
        <el-table-column prop="proposalNo" label="申报编号" width="155" />
        <el-table-column prop="title" label="项目名称" min-width="250" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="150" show-overflow-tooltip />
        <el-table-column label="申请预算" width="120" align="right">
          <template #default="{row}">¥{{ money(row.requestedBudget) }}</template>
        </el-table-column>
        <el-table-column label="计划周期" width="190">
          <template #default="{row}">{{ row.plannedStartDate || '-' }} — {{ row.plannedEndDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{row}"><span :class="['status',row.status.toLowerCase()]">{{ statusText(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="" width="90" fixed="right">
          <template #default="{row}"><el-button text type="primary" @click.stop="openDetail(row)">查看</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.proposalId ? '项目申请' : '新建项目申请'" width="860px" class="proposal-dialog" destroy-on-close>
      <div v-if="form.proposalId" class="dialog-meta">
        <span>{{ form.proposalNo }}</span><span :class="['status',form.status?.toLowerCase()]">{{ statusText(form.status) }}</span>
      </div>

      <section v-if="editable" class="ai-strip">
        <el-input v-model="aiDescription" placeholder="输入项目想法，让 AI 先整理申报草稿" @keyup.enter="generateAi" />
        <el-button :loading="aiLoading" @click="generateAi"><el-icon><MagicStick /></el-icon>AI整理</el-button>
      </section>

      <el-form :model="form" label-position="top" class="compact-form">
        <div class="form-grid two">
          <el-form-item label="项目名称"><el-input v-model="form.title" :disabled="!editable" /></el-form-item>
          <el-form-item label="申请预算（元）"><el-input-number v-model="form.requestedBudget" :disabled="!editable" :min="0" :step="10000" style="width:100%" /></el-form-item>
        </div>
        <div class="form-grid two">
          <el-form-item label="计划开始"><el-date-picker v-model="form.plannedStartDate" :disabled="!editable" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="计划结束"><el-date-picker v-model="form.plannedEndDate" :disabled="!editable" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </div>

        <div class="section-title">立项依据</div>
        <el-form-item label="项目背景"><el-input v-model="form.background" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="研究目标"><el-input v-model="form.objectives" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
        <div class="form-grid two">
          <el-form-item label="项目范围"><el-input v-model="form.projectScope" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="非项目范围"><el-input v-model="form.outOfScope" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
        </div>

        <div class="section-title">研究方案</div>
        <el-form-item label="研究内容"><el-input v-model="form.researchContent" :disabled="!editable" type="textarea" :rows="3" /></el-form-item>
        <div class="form-grid two">
          <el-form-item label="技术路线"><el-input v-model="form.methodology" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="创新点"><el-input v-model="form.innovationPoints" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>
        </div>
        <el-form-item label="成功标准"><el-input v-model="form.successCriteria" :disabled="!editable" type="textarea" :rows="2" /></el-form-item>

        <div class="section-title inline-title"><span>预算明细</span><el-button v-if="editable" text type="primary" @click="addBudgetLine">+ 科目</el-button></div>
        <div class="mini-table">
          <div v-for="(line,i) in form.budgetLines" :key="i" class="mini-row">
            <el-input v-model="line.category" :disabled="!editable" placeholder="科目" />
            <el-input-number v-model="line.amount" :disabled="!editable" :min="0" :step="10000" controls-position="right" />
            <el-input v-model="line.description" :disabled="!editable" placeholder="说明" />
            <el-button v-if="editable" text type="danger" @click="form.budgetLines.splice(i,1)">删除</el-button>
          </div>
        </div>

        <div class="section-title inline-title"><span>预期成果</span><el-button v-if="editable" text type="primary" @click="addOutput">+ 成果</el-button></div>
        <div class="mini-table output-table">
          <div v-for="(o,i) in form.expectedOutputs" :key="i" class="mini-row">
            <el-select v-model="o.outputType" :disabled="!editable"><el-option v-for="t in outputTypes" :key="t.value" :label="t.label" :value="t.value" /></el-select>
            <el-input v-model="o.name" :disabled="!editable" placeholder="成果名称" />
            <el-input-number v-model="o.targetQuantity" :disabled="!editable" :min="1" controls-position="right" />
            <el-button v-if="editable" text type="danger" @click="form.expectedOutputs.splice(i,1)">删除</el-button>
          </div>
        </div>

        <div class="section-title">申报材料</div>
        <ResearchAttachmentUpload v-model="form.attachments" :disabled="!editable" />
      </el-form>

      <div v-if="validation" class="validation-box" :class="{ok:validation.valid}">
        <strong>{{ validation.valid ? '材料完整，可以提交' : '提交前还需要完善' }}</strong>
        <span v-for="item in validation.missing || []" :key="item">• {{ item }}</span>
        <span v-for="item in validation.warnings || []" :key="item" class="warning">• {{ item }}</span>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div>
            <el-button v-if="form.proposalId" @click="runValidation">完整性检查</el-button>
          </div>
          <div>
            <el-button @click="dialogVisible=false">关闭</el-button>
            <el-button v-if="editable" :loading="saving" @click="save">保存</el-button>
            <el-button v-if="canSubmit" type="primary" @click="submit">提交评审</el-button>
            <el-button v-if="canAward" type="success" @click="issueAward">立项并创建项目</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import ResearchAttachmentUpload from '@/components/ResearchAttachmentUpload/index.vue'
import useUserStore from '@/store/modules/user'
import {
  listResearchProposals,getResearchProposal,createResearchProposal,updateResearchProposal,
  validateResearchProposal,submitResearchProposal,issueResearchAward,generateResearchProposal
} from '@/api/research'

const { proxy }=getCurrentInstance()
const userStore=useUserStore()
const loading=ref(false),saving=ref(false),dialogVisible=ref(false),aiLoading=ref(false)
const proposals=ref([]),validation=ref(null),aiDescription=ref('')
const query=reactive({keyword:'',status:''})
const statuses=[
  {value:'DRAFT',label:'草稿'},{value:'UNDER_REVIEW',label:'评审中'},{value:'REVISION_REQUIRED',label:'退回修改'},
  {value:'APPROVED',label:'已批准'},{value:'REJECTED',label:'未通过'},{value:'WITHDRAWN',label:'已撤回'}
]
const outputTypes=[
  {value:'PAPER',label:'论文'},{value:'PATENT',label:'专利'},{value:'SOFTWARE',label:'软件'},
  {value:'DATASET',label:'数据集'},{value:'STANDARD',label:'标准'},{value:'REPORT',label:'报告'},
  {value:'PROTOTYPE',label:'原型'},{value:'OTHER',label:'其他'}
]
const blank=()=>({
  proposalId:null,proposalNo:'',status:'DRAFT',title:'',background:'',objectives:'',projectScope:'',outOfScope:'',
  researchContent:'',methodology:'',innovationPoints:'',successCriteria:'',plannedStartDate:'',plannedEndDate:'',
  requestedBudget:0,members:[],budgetLines:[
    {category:'设备费',amount:0,description:''},{category:'材料费',amount:0,description:''},
    {category:'测试费',amount:0,description:''},{category:'人员费',amount:0,description:''}
  ],expectedOutputs:[{outputType:'REPORT',name:'项目技术报告',targetQuantity:1,targetDescription:''}],attachments:''
})
const form=reactive(blank())
const isAdmin=computed(()=>userStore.roles.includes('admin')||userStore.roles.includes('research_admin'))
const canCreate=computed(()=>isAdmin.value||userStore.roles.includes('research_owner'))
const editable=computed(()=>!form.proposalId||['DRAFT','REVISION_REQUIRED'].includes(form.status))
const canSubmit=computed(()=>form.proposalId&&editable.value)
const canAward=computed(()=>isAdmin.value&&form.status==='APPROVED'&&!form.award)

async function load(){loading.value=true;try{const r=await listResearchProposals(query);proposals.value=r.data||[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,blank());validation.value=null;aiDescription.value='';dialogVisible.value=true}
async function openDetail(row){const r=await getResearchProposal(row.proposalId);const d=r.data||{};Object.assign(form,blank(),d.proposal||{},{
  members:d.members||[],budgetLines:d.budgetLines||[],expectedOutputs:d.expectedOutputs||[],award:d.award,
  attachments:JSON.stringify((d.documents||[]).map(x=>({name:x.fileName,url:x.storageKey})))
});validation.value=null;dialogVisible.value=true}
function addBudgetLine(){form.budgetLines.push({category:'其他',amount:0,description:''})}
function addOutput(){form.expectedOutputs.push({outputType:'REPORT',name:'',targetQuantity:1,targetDescription:''})}
async function save(){
  if(!form.title)return proxy.$modal.msgWarning('请输入项目名称')
  if(!form.plannedEndDate)return proxy.$modal.msgWarning('请选择计划结束日期')
  saving.value=true
  try{
    if(form.proposalId)await updateResearchProposal(form.proposalId,form)
    else {const r=await createResearchProposal(form);const id=r.data?.proposal?.proposalId;if(id)form.proposalId=id}
    proxy.$modal.msgSuccess('申报已保存');await load();if(form.proposalId)await openDetail({proposalId:form.proposalId})
  }finally{saving.value=false}
}
async function runValidation(){const r=await validateResearchProposal(form.proposalId);validation.value=r.data||{}}
async function submit(){await runValidation();if(!validation.value?.valid)return;await proxy.$modal.confirm('提交后进入正式评审，确认继续？');await submitResearchProposal(form.proposalId);proxy.$modal.msgSuccess('已提交评审');dialogVisible.value=false;load()}
async function issueAward(){
  await proxy.$modal.confirm('将按批准后的申报内容生成立项批复和规划中项目，确认继续？')
  await issueResearchAward(form.proposalId,{})
  proxy.$modal.msgSuccess('立项完成，已创建正式项目');dialogVisible.value=false;load()
}
async function generateAi(){
  if(!aiDescription.value.trim())return proxy.$modal.msgWarning('请先描述项目想法')
  aiLoading.value=true
  try{const r=await generateResearchProposal(aiDescription.value.trim());const text=r.data?.content||'';if(text){form.researchContent=text;proxy.$modal.msgSuccess('AI 草稿已填入研究内容，请人工整理后保存')}}finally{aiLoading.value=false}
}
function statusText(s){return ({DRAFT:'草稿',SUBMITTED:'已提交',UNDER_REVIEW:'评审中',REVISION_REQUIRED:'退回修改',APPROVED:'已批准',REJECTED:'未通过',WITHDRAWN:'已撤回'})[s]||s}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
onMounted(load)
</script>

<style scoped lang="scss">
.dense-page{display:flex;flex-direction:column;gap:10px}.toolbar{min-height:52px;padding:8px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;justify-content:space-between;gap:12px}.filters{display:flex;gap:8px}.filters .el-input{width:280px}.filters .el-select{width:150px}.toolbar-right{display:flex;align-items:center;gap:10px}.count{color:var(--rf-text-muted);font-size:12px}.table-shell{border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.status{display:inline-flex;align-items:center;min-height:24px;padding:0 7px;border-radius:999px;background:var(--rf-surface-subtle);color:var(--rf-text-secondary);font-size:11px;font-weight:650}.status.under_review{background:var(--rf-warning-soft);color:var(--rf-warning)}.status.approved{background:var(--rf-success-soft);color:var(--rf-success)}.status.rejected{background:var(--rf-danger-soft);color:var(--rf-danger)}.status.draft,.status.revision_required{background:var(--rf-primary-soft);color:var(--rf-primary)}.dialog-meta{margin:-4px 0 10px;display:flex;gap:8px;align-items:center;color:var(--rf-text-muted);font-size:12px}.ai-strip{margin-bottom:12px;padding:8px;border:1px solid var(--rf-primary-border);border-radius:9px;background:var(--rf-primary-soft);display:flex;gap:8px}.compact-form :deep(.el-form-item){margin-bottom:12px}.compact-form :deep(.el-form-item__label){padding-bottom:5px;font-size:12px}.form-grid.two{display:grid;grid-template-columns:1fr 1fr;gap:12px}.section-title{margin:8px 0 10px;padding-top:10px;border-top:1px solid var(--rf-border);color:var(--rf-text);font-size:12px;font-weight:750}.inline-title{display:flex;align-items:center;justify-content:space-between}.mini-table{display:flex;flex-direction:column;gap:6px}.mini-row{display:grid;grid-template-columns:140px 150px minmax(0,1fr) 52px;gap:6px;align-items:center}.output-table .mini-row{grid-template-columns:130px minmax(0,1fr) 120px 52px}.validation-box{margin-top:12px;padding:10px 12px;border-radius:8px;background:var(--rf-danger-soft);color:var(--rf-danger);display:flex;flex-direction:column;gap:3px;font-size:12px}.validation-box.ok{background:var(--rf-success-soft);color:var(--rf-success)}.validation-box .warning{color:var(--rf-warning)}.dialog-footer{display:flex;align-items:center;justify-content:space-between;gap:12px}.dialog-footer>div{display:flex;gap:8px}@media(max-width:760px){.toolbar,.filters{align-items:stretch;flex-direction:column}.filters .el-input,.filters .el-select{width:100%}.toolbar-right{justify-content:space-between}.form-grid.two{grid-template-columns:1fr}.mini-row,.output-table .mini-row{grid-template-columns:1fr 1fr}.mini-row>:nth-child(3){grid-column:1/-1}}
</style>
