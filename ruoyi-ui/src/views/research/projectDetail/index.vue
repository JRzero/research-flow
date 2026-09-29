<template>
  <div v-loading="loading" class="workspace" v-if="project.projectId">
    <section class="project-hero" aria-labelledby="project-title">
      <div class="hero-top">
        <el-button text @click="router.push('/research/projects')"><el-icon><ArrowLeft /></el-icon> 返回项目</el-button>
        <div class="hero-actions">
          <el-button v-if="canEditDraft" @click="openEdit">编辑申报</el-button>
          <el-button v-if="canSubmit" type="primary" @click="submitProject">提交审批</el-button>
          <template v-if="canApprove && project.status==='PENDING_APPROVAL'">
            <el-button @click="rejectProject">驳回</el-button><el-button type="primary" @click="approveProject">审批通过</el-button>
          </template>
          <el-button v-if="canApprove && project.status==='APPROVED'" type="primary" @click="startProject">启动项目</el-button>
          <el-button v-if="canSubmitAcceptance" type="success" plain @click="acceptanceVisible=true">提交验收</el-button>
          <template v-if="canApprove && project.status==='PENDING_ACCEPTANCE'">
            <el-button @click="reviewAcceptance(false)">退回整改</el-button><el-button type="success" @click="reviewAcceptance(true)">验收通过</el-button>
          </template>
        </div>
      </div>
      <div class="hero-body">
        <div>
          <div class="hero-kicker"><span>{{ project.projectNo }}</span><span class="status-pill" :class="project.status.toLowerCase()">{{ statusText(project.status) }}</span></div>
          <h1 id="project-title">{{ project.projectName }}</h1>
          <p>{{ project.summary || '暂无项目简介' }}</p>
        </div>
        <div class="hero-progress">
          <div class="progress-number"><strong>{{ project.progress || 0 }}</strong><span>%</span></div>
          <span>当前完成度</span>
        </div>
      </div>
      <div class="hero-meta">
        <div><span>负责人</span><strong>{{ project.ownerName }}</strong></div>
        <div><span>所属部门</span><strong>{{ project.deptName || '-' }}</strong></div>
        <div><span>项目周期</span><strong>{{ project.startDate || '-' }} 至 {{ project.plannedEndDate }}</strong></div>
        <div><span>项目预算</span><strong>¥{{ money(project.totalBudget) }}</strong></div>
      </div>
    </section>

    <div v-if="project.riskLevel !== 'NONE'" class="risk-banner" :class="project.riskLevel.toLowerCase()" role="status">
      <el-icon><WarningFilled /></el-icon>
      <div><strong>{{ project.riskLevel === 'HIGH' ? '高风险项目' : '项目需要关注' }}</strong><p>{{ project.riskReason }}</p></div>
      <span>规则识别</span>
    </div>

    <section class="workspace-panel">
      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane label="概览" name="overview">
          <div class="overview-grid">
            <div class="overview-main">
              <InfoBlock title="研究目标" :content="project.researchObjectives" />
              <InfoBlock title="研究内容" :content="project.researchContent" />
              <InfoBlock title="预期成果" :content="project.expectedDeliverables" />
              <div class="section-block">
                <div class="block-head">
                  <h3>申报附件</h3>
                  <span class="attachment-hint">项目申报阶段提交的任务书、预算说明和论证材料</span>
                </div>
                <ResearchAttachmentUpload :model-value="project.applicationAttachments || ''" disabled />
              </div>
              <div class="section-block">
                <div class="block-head"><h3>最近进展</h3><el-button v-if="canExecute" text type="primary" @click="progressVisible=true">更新进展</el-button></div>
                <div v-if="progressRecords.length" class="latest-progress">
                  <div class="progress-date">{{ progressRecords[0].recordDate }}</div>
                  <strong>完成度更新至 {{ progressRecords[0].progressPercent }}%</strong>
                  <p>{{ progressRecords[0].completedWork }}</p>
                  <div v-if="progressRecords[0].issues" class="issue-line"><el-icon><Warning /></el-icon>{{ progressRecords[0].issues }}</div>
                </div>
                <el-empty v-else description="尚未记录项目进展" :image-size="70" />
              </div>
            </div>
            <aside class="overview-side">
              <div class="health-card">
                <div class="block-head"><h3>项目健康度</h3><span :class="['health-tag',project.riskLevel?.toLowerCase()]">{{ project.riskLevel==='NONE'?'正常':project.riskLevel==='HIGH'?'高风险':'需关注' }}</span></div>
                <MetricLine label="任务进度" :value="project.progress || 0" />
                <MetricLine label="预算执行" :value="budgetRate" />
                <div class="mini-stat"><span>里程碑</span><strong>{{ completedMilestones }}/{{ milestones.length }}</strong></div>
                <div class="mini-stat"><span>成果登记</span><strong>{{ deliverables.length }}</strong></div>
              </div>
              <div class="next-card">
                <span>下一个里程碑</span>
                <template v-if="nextMilestone"><strong>{{ nextMilestone.title }}</strong><p>计划 {{ nextMilestone.dueDate }} 完成</p></template>
                <p v-else>暂无待完成里程碑</p>
              </div>
            </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`里程碑 ${milestones.length}`" name="milestones">
          <div class="tab-head"><div><h3>项目里程碑</h3><p>用可验证节点表达项目进度，而不是只依赖一个百分比。</p></div><el-button v-if="canExecute" type="primary" plain @click="milestoneVisible=true"><el-icon><Plus /></el-icon> 添加里程碑</el-button></div>
          <div v-if="milestones.length" class="milestone-list">
            <div v-for="(m,index) in milestones" :key="m.milestoneId" class="milestone-item">
              <div class="milestone-index">{{ String(index+1).padStart(2,'0') }}</div>
              <div class="milestone-main"><div><strong>{{ m.title }}</strong><span :class="['milestone-status',m.status.toLowerCase()]">{{ milestoneStatus(m) }}</span></div><p>{{ m.description || '暂无说明' }}</p></div>
              <div class="milestone-date"><span>计划完成</span><strong>{{ m.dueDate || '-' }}</strong></div>
              <el-button v-if="canExecute && m.status!=='COMPLETED'" text type="primary" @click="completeMilestone(m)">标记完成</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无里程碑" />
        </el-tab-pane>

        <el-tab-pane :label="`项目进展 ${progressRecords.length}`" name="progress">
          <div class="tab-head"><div><h3>项目进展时间线</h3><p>持续记录已完成工作、问题和下一步计划。</p></div><el-button v-if="canExecute" type="primary" @click="progressVisible=true"><el-icon><Plus /></el-icon> 更新进展</el-button></div>
          <el-timeline v-if="progressRecords.length" class="progress-timeline">
            <el-timeline-item v-for="r in progressRecords" :key="r.progressId" :timestamp="r.recordDate" placement="top" type="primary">
              <div class="timeline-card"><div class="timeline-head"><strong>项目进度 {{ r.progressPercent }}%</strong><span>{{ r.recorderName }}</span></div><h5>本阶段完成</h5><p>{{ r.completedWork || '-' }}</p><div class="timeline-grid"><div><h5>当前问题</h5><p>{{ r.issues || '暂无' }}</p></div><div><h5>下一阶段</h5><p>{{ r.nextPlan || '-' }}</p></div></div></div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="尚未记录进展" />
        </el-tab-pane>

        <el-tab-pane label="经费" name="budget">
          <div class="tab-head"><div><h3>项目经费执行</h3><p>只跟踪项目预算使用，不替代财务报销系统。</p></div><el-button v-if="canExecute" type="primary" plain @click="expenseVisible=true"><el-icon><Plus /></el-icon> 记录支出</el-button></div>
          <div class="budget-overview">
            <div><span>项目总预算</span><strong>¥{{ money(project.totalBudget) }}</strong></div><div><span>已使用</span><strong>¥{{ money(project.usedBudget) }}</strong></div><div><span>剩余预算</span><strong>¥{{ money(project.remainingBudget) }}</strong></div><div><span>执行率</span><strong>{{ budgetRate }}%</strong></div>
          </div>
          <el-progress :percentage="budgetRate" :stroke-width="12" class="budget-progress" />
          <div v-if="expenses.length" class="record-list">
            <div v-for="e in expenses" :key="e.expenseId" class="record-row"><div class="record-icon"><el-icon><Wallet /></el-icon></div><div class="record-main"><strong>{{ e.expenseType }}</strong><span>{{ e.description || '无备注' }}</span></div><span class="record-date">{{ e.expenseDate }}</span><strong class="record-amount">- ¥{{ money(e.amount) }}</strong></div>
          </div>
          <el-empty v-else description="暂无经费记录" />
        </el-tab-pane>

        <el-tab-pane :label="`成果 ${deliverables.length}`" name="deliverables">
          <div class="tab-head"><div><h3>项目成果</h3><p>登记论文、专利、软件、数据集和技术报告等成果。</p></div><el-button v-if="canExecute" type="primary" plain @click="deliverableVisible=true"><el-icon><Plus /></el-icon> 登记成果</el-button></div>
          <div v-if="deliverables.length" class="deliverable-grid"><div v-for="d in deliverables" :key="d.deliverableId" class="deliverable-card"><div class="deliverable-icon"><el-icon><DocumentChecked /></el-icon></div><div><span>{{ d.type }}</span><h4>{{ d.name }}</h4><p>{{ d.description || '暂无成果说明' }}</p><small>{{ d.completedDate }}</small></div></div></div>
          <el-empty v-else description="暂无项目成果" />
        </el-tab-pane>

        <el-tab-pane label="流程记录" name="workflow">
          <div class="tab-head"><div><h3>项目生命周期</h3><p>所有关键业务状态变化均保留操作记录。</p></div></div>
          <el-timeline v-if="approvals.length" class="workflow-timeline">
            <el-timeline-item v-for="a in approvals" :key="a.approvalId" :timestamp="a.createdAt" placement="top" :type="actionType(a.action)">
              <div class="workflow-row"><div><strong>{{ actionText(a) }}</strong><p>{{ a.comment || '无补充意见' }}</p></div><span>{{ a.operatorName || '-' }}</span></div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流程记录" />
          <div v-if="acceptance" class="acceptance-card"><div class="block-head"><h3>验收申请</h3><span :class="['acceptance-status',acceptance.status?.toLowerCase()]">{{ acceptanceStatus }}</span></div><p><strong>项目总结：</strong>{{ acceptance.projectSummary }}</p><p><strong>完成情况：</strong>{{ acceptance.completionStatement || '-' }}</p><p v-if="acceptance.unfinishedItems"><strong>未完成事项：</strong>{{ acceptance.unfinishedItems }}</p><p v-if="acceptance.reviewComment"><strong>验收意见：</strong>{{ acceptance.reviewComment }}</p></div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="editVisible" title="编辑项目申报" width="720px">
      <el-form :model="editForm" label-position="top"><el-form-item label="项目名称"><el-input v-model="editForm.projectName" /></el-form-item><el-form-item label="项目简介"><el-input v-model="editForm.summary" type="textarea" :rows="2" /></el-form-item><div class="dialog-grid"><el-form-item label="开始日期"><el-date-picker v-model="editForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="计划结束日期"><el-date-picker v-model="editForm.plannedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></div><el-form-item label="项目预算"><el-input-number v-model="editForm.totalBudget" :min="0" style="width:100%" /></el-form-item><el-form-item label="研究目标"><el-input v-model="editForm.researchObjectives" type="textarea" :rows="3" /></el-form-item><el-form-item label="研究内容"><el-input v-model="editForm.researchContent" type="textarea" :rows="3" /></el-form-item><el-form-item label="预期成果"><el-input v-model="editForm.expectedDeliverables" type="textarea" :rows="2" /></el-form-item><el-form-item label="申报附件"><ResearchAttachmentUpload v-model="editForm.applicationAttachments" /></el-form-item></el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" @click="saveEdit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="milestoneVisible" title="添加里程碑" width="520px"><el-form :model="milestoneForm" label-position="top"><el-form-item label="里程碑名称"><el-input v-model="milestoneForm.title" /></el-form-item><el-form-item label="计划完成日期"><el-date-picker v-model="milestoneForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="说明"><el-input v-model="milestoneForm.description" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="milestoneVisible=false">取消</el-button><el-button type="primary" @click="saveMilestone">保存</el-button></template></el-dialog>

    <el-dialog v-model="progressVisible" title="更新项目进展" width="620px"><el-form :model="progressForm" label-position="top"><el-form-item label="当前完成度"><el-slider v-model="progressForm.progressPercent" show-input /></el-form-item><el-form-item label="本阶段完成工作"><el-input v-model="progressForm.completedWork" type="textarea" :rows="3" /></el-form-item><el-form-item label="当前问题"><el-input v-model="progressForm.issues" type="textarea" :rows="2" /></el-form-item><el-form-item label="下一阶段计划"><el-input v-model="progressForm.nextPlan" type="textarea" :rows="2" /></el-form-item></el-form><template #footer><el-button @click="progressVisible=false">取消</el-button><el-button type="primary" @click="saveProgress">保存进展</el-button></template></el-dialog>

    <el-dialog v-model="expenseVisible" title="记录项目支出" width="520px"><el-form :model="expenseForm" label-position="top"><el-form-item label="支出类型"><el-select v-model="expenseForm.expenseType" style="width:100%"><el-option v-for="t in expenseTypes" :key="t" :label="t" :value="t" /></el-select></el-form-item><el-form-item label="支出金额（元）"><el-input-number v-model="expenseForm.amount" :min="0" :step="1000" style="width:100%" /></el-form-item><el-form-item label="支出日期"><el-date-picker v-model="expenseForm.expenseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="说明"><el-input v-model="expenseForm.description" type="textarea" :rows="2" /></el-form-item></el-form><template #footer><el-button @click="expenseVisible=false">取消</el-button><el-button type="primary" @click="saveExpense">保存</el-button></template></el-dialog>

    <el-dialog v-model="deliverableVisible" title="登记项目成果" width="540px"><el-form :model="deliverableForm" label-position="top"><el-form-item label="成果名称"><el-input v-model="deliverableForm.name" /></el-form-item><el-form-item label="成果类型"><el-select v-model="deliverableForm.type" style="width:100%"><el-option v-for="t in deliverableTypes" :key="t" :label="t" :value="t" /></el-select></el-form-item><el-form-item label="完成日期"><el-date-picker v-model="deliverableForm.completedDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="成果说明"><el-input v-model="deliverableForm.description" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="deliverableVisible=false">取消</el-button><el-button type="primary" @click="saveDeliverable">保存</el-button></template></el-dialog>

    <el-dialog v-model="acceptanceVisible" title="提交项目验收" width="650px"><el-form :model="acceptanceForm" label-position="top"><el-form-item label="项目总结"><el-input v-model="acceptanceForm.projectSummary" type="textarea" :rows="4" /></el-form-item><el-form-item label="任务完成情况"><el-input v-model="acceptanceForm.completionStatement" type="textarea" :rows="3" /></el-form-item><el-form-item label="未完成事项"><el-input v-model="acceptanceForm.unfinishedItems" type="textarea" :rows="2" /></el-form-item><el-form-item label="验收说明"><el-input v-model="acceptanceForm.acceptanceNote" type="textarea" :rows="2" /></el-form-item></el-form><template #footer><el-button @click="acceptanceVisible=false">取消</el-button><el-button type="success" @click="saveAcceptance">提交验收</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { h } from 'vue'
import ResearchAttachmentUpload from '@/components/ResearchAttachmentUpload/index.vue'
import useUserStore from '@/store/modules/user'
import { getResearchProject, updateResearchProject, submitResearchProject, approveResearchProject, rejectResearchProject, startResearchProject as startProjectApi, addResearchMilestone, completeResearchMilestone, addResearchProgress, addResearchExpense, addResearchDeliverable, submitResearchAcceptance, approveResearchAcceptance, rejectResearchAcceptance } from '@/api/research'

const InfoBlock={props:['title','content'],setup(props){return()=>h('div',{class:'section-block'},[h('h3',props.title),h('p',{class:'long-copy'},props.content||'暂无内容')])}}
const MetricLine={props:['label','value'],setup(props){return()=>h('div',{class:'metric-line'},[h('div',[h('span',props.label),h('strong',`${props.value||0}%`)]),h('div',{class:'metric-track'},h('div',{class:'metric-fill',style:{width:`${Math.min(100,props.value||0)}%`}}))])}}

const route=useRoute(); const router=useRouter(); const userStore=useUserStore(); const {proxy}=getCurrentInstance(); const loading=ref(false); const activeTab=ref('overview')
const detail=reactive({project:{},milestones:[],progressRecords:[],expenses:[],deliverables:[],approvals:[],acceptance:null})
const project=computed(()=>detail.project||{}); const milestones=computed(()=>detail.milestones||[]); const progressRecords=computed(()=>detail.progressRecords||[]); const expenses=computed(()=>detail.expenses||[]); const deliverables=computed(()=>detail.deliverables||[]); const approvals=computed(()=>detail.approvals||[]); const acceptance=computed(()=>detail.acceptance)
const isAdmin=computed(()=>userStore.roles.includes('admin')||userStore.roles.includes('research_admin')); const isResearchOwner=computed(()=>userStore.roles.includes('research_owner')); const isOwner=computed(()=>Number(userStore.id)===Number(project.value.ownerUserId)); const canApprove=isAdmin; const canOperate=computed(()=>isAdmin.value||(isResearchOwner.value&&isOwner.value)); const canEditDraft=computed(()=>canOperate.value&&['DRAFT','REJECTED'].includes(project.value.status)); const canSubmit=canEditDraft; const canExecute=computed(()=>canOperate.value&&project.value.status==='IN_PROGRESS'); const canSubmitAcceptance=canExecute
const budgetRate=computed(()=>{const t=Number(project.value.totalBudget||0);return t?Math.min(100,Math.round(Number(project.value.usedBudget||0)*100/t)):0}); const completedMilestones=computed(()=>milestones.value.filter(m=>m.status==='COMPLETED').length); const nextMilestone=computed(()=>milestones.value.find(m=>m.status!=='COMPLETED'))
const acceptanceStatus=computed(()=>({PENDING:'待验收',APPROVED:'已通过',REJECTED:'已退回'})[acceptance.value?.status]||acceptance.value?.status)

const editVisible=ref(false), milestoneVisible=ref(false),progressVisible=ref(false),expenseVisible=ref(false),deliverableVisible=ref(false),acceptanceVisible=ref(false)
const editForm=reactive({}); const milestoneForm=reactive({title:'',dueDate:'',description:''}); const progressForm=reactive({progressPercent:0,completedWork:'',issues:'',nextPlan:''}); const expenseForm=reactive({expenseType:'材料费',amount:0,expenseDate:'',description:''}); const deliverableForm=reactive({name:'',type:'技术报告',completedDate:'',description:''}); const acceptanceForm=reactive({projectSummary:'',completionStatement:'',unfinishedItems:'',acceptanceNote:''})
const expenseTypes=['设备费','材料费','人员费','测试费','差旅费','其他']; const deliverableTypes=['论文','专利','软件','数据集','技术报告','标准','其他']
async function load(){loading.value=true;try{const res=await getResearchProject(route.params.projectId);Object.assign(detail,res.data||{})}finally{loading.value=false}}
function openEdit(){Object.assign(editForm,JSON.parse(JSON.stringify(project.value)));editVisible.value=true}
async function saveEdit(){await updateResearchProject(project.value.projectId,editForm);proxy.$modal.msgSuccess('申报信息已更新');editVisible.value=false;load()}
async function submitProject(){await proxy.$modal.confirm('确认提交项目申报吗？提交后将进入审批流程。');await submitResearchProject(project.value.projectId);proxy.$modal.msgSuccess('已提交审批');load()}
async function ask(title,msg){try{const {value}=await ElMessageBox.prompt(msg,title,{confirmButtonText:'确认',cancelButtonText:'取消',inputType:'textarea'});return value||''}catch{return null}}
async function approveProject(){const c=await ask('审批通过','填写审批意见（可选）');if(c===null)return;await approveResearchProject(project.value.projectId,c);proxy.$modal.msgSuccess('审批通过');load()}
async function rejectProject(){const c=await ask('驳回项目','请输入驳回原因');if(c===null)return;await rejectResearchProject(project.value.projectId,c);proxy.$modal.msgSuccess('项目已驳回');load()}
async function startProject(){await proxy.$modal.confirm('确认启动该项目并进入执行阶段吗？');await startProjectApi(project.value.projectId);proxy.$modal.msgSuccess('项目已启动');load()}
async function saveMilestone(){if(!milestoneForm.title)return proxy.$modal.msgWarning('请输入里程碑名称');await addResearchMilestone(project.value.projectId,milestoneForm);milestoneVisible.value=false;Object.assign(milestoneForm,{title:'',dueDate:'',description:''});proxy.$modal.msgSuccess('里程碑已添加');load()}
async function completeMilestone(m){await completeResearchMilestone(project.value.projectId,m.milestoneId);proxy.$modal.msgSuccess('里程碑已完成');load()}
async function saveProgress(){await addResearchProgress(project.value.projectId,progressForm);progressVisible.value=false;Object.assign(progressForm,{progressPercent:project.value.progress||0,completedWork:'',issues:'',nextPlan:''});proxy.$modal.msgSuccess('进展已更新');load()}
async function saveExpense(){await addResearchExpense(project.value.projectId,expenseForm);expenseVisible.value=false;Object.assign(expenseForm,{expenseType:'材料费',amount:0,expenseDate:'',description:''});proxy.$modal.msgSuccess('支出已记录');load()}
async function saveDeliverable(){if(!deliverableForm.name)return proxy.$modal.msgWarning('请输入成果名称');await addResearchDeliverable(project.value.projectId,deliverableForm);deliverableVisible.value=false;Object.assign(deliverableForm,{name:'',type:'技术报告',completedDate:'',description:''});proxy.$modal.msgSuccess('成果已登记');load()}
async function saveAcceptance(){if(!acceptanceForm.projectSummary)return proxy.$modal.msgWarning('请填写项目总结');await submitResearchAcceptance(project.value.projectId,acceptanceForm);acceptanceVisible.value=false;proxy.$modal.msgSuccess('验收申请已提交');load()}
async function reviewAcceptance(approved){const c=await ask(approved?'验收通过':'退回整改',approved?'填写验收意见':'填写整改要求');if(c===null)return;if(approved)await approveResearchAcceptance(project.value.projectId,c);else await rejectResearchAcceptance(project.value.projectId,c);proxy.$modal.msgSuccess(approved?'项目已结项':'已退回整改');load()}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:2})}
function statusText(s){return ({DRAFT:'草稿',PENDING_APPROVAL:'待审批',APPROVED:'已立项',IN_PROGRESS:'执行中',PENDING_ACCEPTANCE:'待验收',COMPLETED:'已结项',REJECTED:'已驳回',TERMINATED:'已终止'})[s]||s}
function milestoneStatus(m){if(m.status==='COMPLETED')return '已完成';if(m.dueDate&&new Date(m.dueDate)<new Date())return '已延期';return '待完成'}
function actionText(a){const map={SUBMIT:'提交',APPROVE:'审批通过',REJECT:'退回',START:'项目启动'};const prefix=a.businessType==='PROJECT_ACCEPTANCE'?'项目验收':a.businessType==='PROJECT_EXECUTION'?'项目执行':'项目申报';return `${prefix} · ${map[a.action]||a.action}`}
function actionType(a){return a.action==='APPROVE'||a.action==='START'?'success':a.action==='REJECT'?'danger':'primary'}
onMounted(()=>{load();progressForm.progressPercent=0})
</script>

<style scoped lang="scss">
.workspace { display: flex; flex-direction: column; gap: 16px; min-width: 0; }

.project-hero {
  overflow: hidden;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  background: var(--rf-surface);
  color: var(--rf-text);
  box-shadow: var(--rf-shadow-sm);
}
.hero-top {
  min-height: 62px;
  padding: 9px 20px;
  border-bottom: 1px solid var(--rf-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.hero-top :deep(.el-button.is-text) { color: var(--rf-text-secondary); }
.hero-top :deep(.el-button.is-text:hover) { color: var(--rf-primary); background: var(--rf-primary-soft); }
.hero-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }

.hero-body {
  padding: 24px 26px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 28px;
  align-items: center;
}
.hero-body > div:first-child { min-width: 0; }
.hero-kicker { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; color: var(--rf-text-muted); font-size: 12px; font-weight: 550; }
.status-pill {
  min-height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  font-size: 12px;
  font-weight: 650;
}
.status-pill::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: var(--rf-text-muted); }
.status-pill.in_progress { background: var(--rf-primary-soft); color: var(--rf-primary-hover); }
.status-pill.in_progress::before { background: var(--rf-primary); }
.status-pill.pending_approval,
.status-pill.pending_acceptance { background: var(--rf-warning-soft); color: var(--rf-warning); }
.status-pill.pending_approval::before,
.status-pill.pending_acceptance::before { background: var(--rf-warning); }
.status-pill.completed { background: var(--rf-success-soft); color: var(--rf-success); }
.status-pill.completed::before { background: var(--rf-success); }
.status-pill.rejected,
.status-pill.terminated { background: var(--rf-danger-soft); color: var(--rf-danger); }
.status-pill.rejected::before,
.status-pill.terminated::before { background: var(--rf-danger); }

.hero-body h1 {
  margin: 9px 0 7px;
  color: var(--rf-text);
  font-size: clamp(22px, 2.1vw, 28px);
  font-weight: 730;
  line-height: 1.35;
  letter-spacing: -.5px;
  overflow-wrap: anywhere;
}
.hero-body p { max-width: 820px; margin: 0; color: var(--rf-text-secondary); font-size: 13px; line-height: 1.65; }

.hero-progress {
  min-height: 100px;
  padding: 14px;
  border: 1px solid var(--rf-primary-border);
  border-radius: 14px;
  display: grid;
  place-items: center;
  align-content: center;
  background: var(--rf-primary-soft);
  text-align: center;
}
.progress-number { line-height: 1; }
.progress-number strong { color: var(--rf-primary-hover); font-size: 34px; font-weight: 760; font-variant-numeric: tabular-nums; }
.progress-number span { color: var(--rf-primary); font-size: 14px; }
.hero-progress > span { margin-top: 7px; color: var(--rf-text-muted); font-size: 12px; }

.hero-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--rf-border);
  background: var(--rf-surface-subtle);
}
.hero-meta > div {
  min-width: 0;
  padding: 14px 20px;
  border-right: 1px solid var(--rf-border);
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.hero-meta > div:last-child { border-right: 0; }
.hero-meta span { color: var(--rf-text-muted); font-size: 12px; }
.hero-meta strong { overflow-wrap: anywhere; color: var(--rf-text-secondary); font-size: 13px; font-weight: 650; font-variant-numeric: tabular-nums; }

.risk-banner {
  min-height: 66px;
  padding: 13px 16px;
  border: 1px solid color-mix(in srgb, var(--rf-warning) 24%, var(--rf-border));
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--rf-warning-soft);
  color: var(--rf-warning);
}
.risk-banner.high { border-color: color-mix(in srgb, var(--rf-danger) 24%, var(--rf-border)); background: var(--rf-danger-soft); color: var(--rf-danger); }
.risk-banner > .el-icon { flex: 0 0 auto; font-size: 22px; }
.risk-banner div { min-width: 0; flex: 1; }
.risk-banner strong { color: var(--rf-text); font-size: 13px; }
.risk-banner p { margin: 4px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.5; }
.risk-banner > span { min-height: 26px; padding: 0 8px; border: 1px solid currentColor; border-radius: 999px; display: inline-flex; align-items: center; font-size: 11px; font-weight: 650; white-space: nowrap; }

.workspace-panel {
  min-width: 0;
  padding: 0 22px 24px;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  background: var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
  background: var(--rf-surface);
}
.workspace-tabs :deep(.el-tabs__item) {
  min-width: 72px;
  height: 54px;
  padding: 0 14px;
  color: var(--rf-text-muted);
  font-size: 13px;
}
.workspace-tabs :deep(.el-tabs__item.is-active) { color: var(--rf-primary); font-weight: 650; }
.workspace-tabs :deep(.el-tabs__active-bar) { height: 3px; border-radius: 3px 3px 0 0; }
.workspace-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; background: var(--rf-border); }

.overview-grid { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; }
.overview-main { min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.overview-side { display: flex; flex-direction: column; gap: 12px; }
.section-block, .health-card, .next-card {
  padding: 17px;
  border: 1px solid var(--rf-border);
  border-radius: 12px;
  background: var(--rf-surface);
}
.section-block h3, .block-head h3 { margin: 0; color: var(--rf-text); font-size: 14px; font-weight: 680; }
.long-copy {
  margin: 9px 0 0;
  color: var(--rf-text-secondary);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.block-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.attachment-hint { color: var(--rf-text-muted); font-size: 11px; line-height: 1.5; text-align: right; }

.latest-progress { margin-top: 12px; padding: 14px; border-radius: 10px; background: var(--rf-surface-subtle); }
.progress-date { margin-bottom: 5px; color: var(--rf-text-muted); font-size: 12px; }
.latest-progress > strong { color: var(--rf-text); font-size: 13px; }
.latest-progress p { margin: 7px 0 0; color: var(--rf-text-secondary); font-size: 13px; line-height: 1.65; }
.issue-line { margin-top: 10px; padding: 9px 10px; border-radius: 8px; display: flex; gap: 7px; align-items: flex-start; background: var(--rf-warning-soft); color: var(--rf-warning); font-size: 12px; line-height: 1.5; }

.health-tag, .acceptance-status, .milestone-status {
  min-height: 25px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 650;
}
.health-tag { background: var(--rf-success-soft); color: var(--rf-success); }
.health-tag.medium { background: var(--rf-warning-soft); color: var(--rf-warning); }
.health-tag.high { background: var(--rf-danger-soft); color: var(--rf-danger); }

.metric-line { margin-top: 17px; }
.metric-line > div:first-child { display: flex; justify-content: space-between; gap: 12px; color: var(--rf-text-muted); font-size: 12px; }
.metric-line strong { color: var(--rf-text-secondary); font-variant-numeric: tabular-nums; }
.metric-track { height: 7px; margin-top: 7px; overflow: hidden; border-radius: 999px; background: var(--rf-border); }
.metric-fill { height: 100%; border-radius: 999px; background: var(--rf-primary); }
.mini-stat { margin-top: 13px; padding-top: 13px; border-top: 1px solid var(--rf-border); display: flex; justify-content: space-between; color: var(--rf-text-secondary); font-size: 12px; }
.mini-stat span { color: var(--rf-text-muted); }

.next-card { background: var(--rf-surface-subtle); }
.next-card > span { color: var(--rf-text-muted); font-size: 12px; }
.next-card strong { display: block; margin-top: 7px; color: var(--rf-text); font-size: 13px; line-height: 1.5; }
.next-card p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }

.tab-head { margin-bottom: 18px; display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.tab-head h3 { margin: 0; color: var(--rf-text); font-size: 15px; font-weight: 700; }
.tab-head p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }

.milestone-list { display: flex; flex-direction: column; }
.milestone-item {
  min-height: 78px;
  padding: 14px 4px;
  border-top: 1px solid var(--rf-border);
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) 130px auto;
  gap: 14px;
  align-items: center;
}
.milestone-item:first-child { border-top: 0; }
.milestone-index { width: 38px; height: 38px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-primary-soft); color: var(--rf-primary); font-size: 12px; font-weight: 700; font-variant-numeric: tabular-nums; }
.milestone-main { min-width: 0; }
.milestone-main > div { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.milestone-main strong { color: var(--rf-text); font-size: 13px; }
.milestone-main p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.milestone-status { background: var(--rf-warning-soft); color: var(--rf-warning); }
.milestone-status.completed { background: var(--rf-success-soft); color: var(--rf-success); }
.milestone-date { display: flex; flex-direction: column; gap: 4px; }
.milestone-date span { color: var(--rf-text-muted); font-size: 12px; }
.milestone-date strong { color: var(--rf-text-secondary); font-size: 12px; font-variant-numeric: tabular-nums; }

.progress-timeline, .workflow-timeline { padding: 6px 4px; }
.timeline-card {
  padding: 15px;
  border: 1px solid var(--rf-border);
  border-radius: 12px;
  background: var(--rf-surface);
}
.timeline-head { display: flex; justify-content: space-between; gap: 12px; }
.timeline-head strong { color: var(--rf-text); font-size: 13px; }
.timeline-head span { color: var(--rf-text-muted); font-size: 12px; }
.timeline-card h5 { margin: 13px 0 5px; color: var(--rf-text-muted); font-size: 12px; font-weight: 650; }
.timeline-card p { margin: 0; color: var(--rf-text-secondary); font-size: 13px; line-height: 1.6; }
.timeline-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }

.budget-overview { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.budget-overview > div { padding: 14px; border: 1px solid var(--rf-border); border-radius: 10px; background: var(--rf-surface-subtle); }
.budget-overview span { display: block; color: var(--rf-text-muted); font-size: 12px; }
.budget-overview strong { display: block; margin-top: 6px; color: var(--rf-text); font-size: 17px; font-variant-numeric: tabular-nums; }
.budget-progress { margin: 18px 0; }

.record-list { border-top: 1px solid var(--rf-border); }
.record-row { min-height: 66px; padding: 12px 2px; border-bottom: 1px solid var(--rf-border); display: grid; grid-template-columns: 40px minmax(0, 1fr) 110px 130px; gap: 12px; align-items: center; }
.record-icon { width: 38px; height: 38px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-primary-soft); color: var(--rf-primary); }
.record-main { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.record-main strong { color: var(--rf-text); font-size: 13px; }
.record-main span, .record-date { color: var(--rf-text-muted); font-size: 12px; line-height: 1.45; }
.record-amount { color: var(--rf-text-secondary); font-size: 13px; text-align: right; font-variant-numeric: tabular-nums; }

.deliverable-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.deliverable-card { min-width: 0; padding: 15px; border: 1px solid var(--rf-border); border-radius: 12px; display: flex; gap: 12px; background: var(--rf-surface); }
.deliverable-icon { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-success-soft); color: var(--rf-success); font-size: 18px; }
.deliverable-card > div:last-child { min-width: 0; }
.deliverable-card span { color: var(--rf-success); font-size: 12px; font-weight: 650; }
.deliverable-card h4 { margin: 4px 0; color: var(--rf-text); font-size: 13px; overflow-wrap: anywhere; }
.deliverable-card p { margin: 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.55; }
.deliverable-card small { display: block; margin-top: 8px; color: var(--rf-text-muted); font-size: 11px; }

.workflow-row { padding: 13px 14px; border: 1px solid var(--rf-border); border-radius: 10px; display: flex; justify-content: space-between; gap: 16px; background: var(--rf-surface); }
.workflow-row strong { color: var(--rf-text); font-size: 13px; }
.workflow-row p { margin: 5px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.5; }
.workflow-row > span { color: var(--rf-text-muted); font-size: 12px; white-space: nowrap; }

.acceptance-card { margin-top: 18px; padding: 17px; border: 1px solid var(--rf-border); border-radius: 12px; background: var(--rf-surface-subtle); }
.acceptance-card > p { margin: 9px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.65; }
.acceptance-status { background: var(--rf-warning-soft); color: var(--rf-warning); }
.acceptance-status.approved { background: var(--rf-success-soft); color: var(--rf-success); }
.acceptance-status.rejected { background: var(--rf-danger-soft); color: var(--rf-danger); }

.dialog-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
:deep(.el-dialog) { max-width: calc(100vw - 32px); }

@media (max-width: 1050px) {
  .overview-grid { grid-template-columns: 1fr; }
  .overview-side { display: grid; grid-template-columns: 1fr 1fr; }
  .hero-meta { grid-template-columns: repeat(2, 1fr); }
  .hero-meta > div:nth-child(2) { border-right: 0; }
  .hero-meta > div:nth-child(-n+2) { border-bottom: 1px solid var(--rf-border); }
  .milestone-item { grid-template-columns: 46px minmax(0, 1fr); }
  .milestone-date, .milestone-item > .el-button { grid-column: 2; }
  .budget-overview { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 720px) {
  .hero-top { padding: 10px 14px; align-items: flex-start; flex-direction: column; }
  .hero-actions { width: 100%; justify-content: flex-start; }
  .hero-body { padding: 18px; grid-template-columns: 1fr; }
  .hero-progress { width: 100%; min-height: 82px; grid-template-columns: auto auto; gap: 8px; justify-content: start; place-items: center start; }
  .hero-progress > span { margin-top: 0; }
  .hero-meta { grid-template-columns: 1fr; }
  .hero-meta > div { border-right: 0; border-bottom: 1px solid var(--rf-border); }
  .hero-meta > div:last-child { border-bottom: 0; }
  .risk-banner { align-items: flex-start; }
  .risk-banner > span { display: none; }
  .workspace-panel { padding: 0 14px 18px; }
  .workspace-tabs :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
  .workspace-tabs :deep(.el-tabs__nav-scroll) { overflow: visible; }
  .overview-side { display: flex; }
  .tab-head { align-items: stretch; flex-direction: column; }
  .tab-head .el-button { align-self: flex-start; }
  .budget-overview, .deliverable-grid, .timeline-grid { grid-template-columns: 1fr; }
  .record-row { grid-template-columns: 40px minmax(0, 1fr); }
  .record-date, .record-amount { grid-column: 2; text-align: left; }
  .workflow-row { flex-direction: column; gap: 6px; }
  .dialog-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>