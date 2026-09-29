<template>
  <div v-loading="loading" class="workspace" v-if="project.projectId">
    <section class="project-hero">
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
          <h1>{{ project.projectName }}</h1>
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

    <div v-if="project.riskLevel !== 'NONE'" class="risk-banner" :class="project.riskLevel.toLowerCase()">
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
      <el-form :model="editForm" label-position="top"><el-form-item label="项目名称"><el-input v-model="editForm.projectName" /></el-form-item><el-form-item label="项目简介"><el-input v-model="editForm.summary" type="textarea" :rows="2" /></el-form-item><div class="dialog-grid"><el-form-item label="开始日期"><el-date-picker v-model="editForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="计划结束日期"><el-date-picker v-model="editForm.plannedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></div><el-form-item label="项目预算"><el-input-number v-model="editForm.totalBudget" :min="0" style="width:100%" /></el-form-item><el-form-item label="研究目标"><el-input v-model="editForm.researchObjectives" type="textarea" :rows="3" /></el-form-item><el-form-item label="研究内容"><el-input v-model="editForm.researchContent" type="textarea" :rows="3" /></el-form-item><el-form-item label="预期成果"><el-input v-model="editForm.expectedDeliverables" type="textarea" :rows="2" /></el-form-item></el-form>
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
.workspace{display:flex;flex-direction:column;gap:16px}.project-hero{background:linear-gradient(135deg,#102a43,#173f68);color:#fff;border-radius:18px;overflow:hidden;box-shadow:0 18px 36px rgba(15,44,74,.12)}.hero-top{padding:14px 20px;border-bottom:1px solid rgba(255,255,255,.08);display:flex;justify-content:space-between;align-items:center}.hero-top :deep(.el-button.is-text){color:#b9cee3}.hero-actions{display:flex;gap:8px}.hero-body{padding:24px 28px;display:flex;justify-content:space-between;gap:30px;align-items:center}.hero-kicker{display:flex;gap:10px;align-items:center;color:#9db6cf;font-size:11px}.status-pill{padding:4px 8px;border-radius:99px;background:rgba(255,255,255,.1);color:#d5e7f8}.hero-body h1{font-size:24px;margin:9px 0 7px}.hero-body p{margin:0;color:#b7c9db;font-size:12px;max-width:760px}.hero-progress{text-align:center;min-width:100px}.progress-number strong{font-size:38px}.progress-number span{font-size:15px;color:#8fafd0}.hero-progress>span{font-size:10px;color:#94abc2}.hero-meta{display:grid;grid-template-columns:repeat(4,1fr);background:rgba(0,0,0,.12)}.hero-meta>div{padding:14px 24px;border-right:1px solid rgba(255,255,255,.07);display:flex;flex-direction:column;gap:5px}.hero-meta span{color:#8fa9c1;font-size:10px}.hero-meta strong{font-size:12px}.risk-banner{display:flex;align-items:center;gap:12px;padding:14px 18px;background:#fff7ee;border:1px solid #ffe0bd;border-radius:12px;color:#b96b14}.risk-banner.high{background:#fff1f1;border-color:#ffd6d6;color:#b43b3b}.risk-banner>.el-icon{font-size:22px}.risk-banner div{flex:1}.risk-banner p{margin:4px 0 0;font-size:11px}.risk-banner>span{font-size:10px;border:1px solid currentColor;border-radius:99px;padding:4px 8px;opacity:.7}.workspace-panel{background:#fff;border:1px solid #e7ecf2;border-radius:16px;padding:0 22px 24px}.workspace-tabs :deep(.el-tabs__header){margin-bottom:24px}.workspace-tabs :deep(.el-tabs__item){height:52px}.overview-grid{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:24px}.overview-main{display:flex;flex-direction:column;gap:14px}.section-block,.health-card,.next-card{border:1px solid #e9edf2;border-radius:12px;padding:18px}.section-block h3,.block-head h3{margin:0;font-size:13px}.long-copy{margin:10px 0 0;color:#687588;font-size:12px;line-height:1.8;white-space:pre-wrap}.block-head{display:flex;justify-content:space-between;align-items:center}.latest-progress{margin-top:14px;padding:14px;background:#f7f9fc;border-radius:10px}.progress-date{color:#9ba5b2;font-size:10px;margin-bottom:5px}.latest-progress>strong{font-size:12px}.latest-progress p{color:#66758a;font-size:11px;line-height:1.65}.issue-line{display:flex;gap:6px;color:#b3702a;font-size:11px;background:#fff7eb;padding:9px;border-radius:7px}.overview-side{display:flex;flex-direction:column;gap:14px}.health-tag{font-size:10px;color:#348d5a;background:#eaf8f0;padding:4px 7px;border-radius:99px}.health-tag.medium{color:#b16a15;background:#fff3e3}.health-tag.high{color:#bd3f3f;background:#ffeaea}.metric-line{margin-top:18px}.metric-line>div:first-child{display:flex;justify-content:space-between;font-size:11px;color:#8290a2}.metric-line strong{color:#34465c}.metric-track{height:7px;background:#edf1f5;border-radius:99px;margin-top:7px;overflow:hidden}.metric-fill{height:100%;border-radius:99px;background:linear-gradient(90deg,#2f80ed,#56ccf2)}.mini-stat{display:flex;justify-content:space-between;padding-top:13px;margin-top:13px;border-top:1px solid #edf1f4;font-size:11px}.mini-stat span{color:#8995a5}.next-card>span{color:#98a3b1;font-size:10px}.next-card strong{display:block;margin-top:8px;font-size:13px}.next-card p{margin:5px 0 0;color:#8894a4;font-size:11px}.tab-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.tab-head h3{margin:0;font-size:15px}.tab-head p{margin:5px 0 0;color:#929dab;font-size:11px}.milestone-list{display:flex;flex-direction:column}.milestone-item{display:grid;grid-template-columns:46px minmax(0,1fr) 130px auto;gap:16px;align-items:center;padding:16px 4px;border-top:1px solid #edf1f4}.milestone-item:first-child{border-top:0}.milestone-index{width:38px;height:38px;border-radius:10px;background:#f0f5fb;color:#4475a4;display:grid;place-items:center;font-size:11px;font-weight:700}.milestone-main>div{display:flex;gap:9px;align-items:center}.milestone-main strong{font-size:13px}.milestone-main p{margin:5px 0 0;color:#8c97a5;font-size:11px}.milestone-status{font-size:9px;padding:3px 7px;border-radius:99px;background:#fff3df;color:#aa6c18}.milestone-status.completed{background:#e9f8ef;color:#298757}.milestone-date{display:flex;flex-direction:column;gap:4px}.milestone-date span{color:#9ca6b3;font-size:9px}.milestone-date strong{font-size:11px}.progress-timeline,.workflow-timeline{padding:6px 8px}.timeline-card{border:1px solid #e7ecf2;border-radius:12px;padding:16px}.timeline-head{display:flex;justify-content:space-between}.timeline-head strong{font-size:13px}.timeline-head span{color:#939eac;font-size:10px}.timeline-card h5{font-size:10px;color:#919caa;margin:14px 0 4px}.timeline-card p{margin:0;color:#5f6d80;font-size:11px;line-height:1.6}.timeline-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}.budget-overview{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.budget-overview>div{padding:15px;background:#f8fafc;border-radius:10px}.budget-overview span{display:block;color:#929dab;font-size:10px}.budget-overview strong{display:block;margin-top:6px;font-size:16px}.budget-progress{margin:18px 0}.record-list{border-top:1px solid #edf1f4}.record-row{display:grid;grid-template-columns:38px minmax(0,1fr) 100px 120px;gap:12px;align-items:center;padding:14px 2px;border-bottom:1px solid #edf1f4}.record-icon{width:34px;height:34px;border-radius:9px;background:#edf6ff;color:#2f80ed;display:grid;place-items:center}.record-main{display:flex;flex-direction:column;gap:4px}.record-main strong{font-size:12px}.record-main span,.record-date{font-size:10px;color:#919caa}.record-amount{text-align:right;font-size:12px}.deliverable-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.deliverable-card{border:1px solid #e7ecf2;border-radius:12px;padding:16px;display:flex;gap:12px}.deliverable-icon{width:40px;height:40px;border-radius:10px;background:#eef7f2;color:#349662;display:grid;place-items:center;font-size:18px}.deliverable-card>div:last-child{min-width:0}.deliverable-card span{font-size:9px;color:#6b9b7e}.deliverable-card h4{margin:4px 0;font-size:13px}.deliverable-card p{margin:0;color:#818e9f;font-size:10px;line-height:1.55}.deliverable-card small{display:block;margin-top:8px;color:#abb3bd}.workflow-row{display:flex;justify-content:space-between;border:1px solid #e9edf2;border-radius:10px;padding:13px 15px}.workflow-row strong{font-size:12px}.workflow-row p{margin:5px 0 0;color:#8c97a5;font-size:10px}.workflow-row>span{font-size:10px;color:#919caa}.acceptance-card{margin-top:18px;padding:18px;border-radius:12px;background:#f8fafc}.acceptance-card>p{font-size:11px;color:#667589;line-height:1.65}.acceptance-status{font-size:10px;padding:4px 8px;border-radius:99px;background:#fff1d9;color:#ac6d1a}.acceptance-status.approved{background:#e9f8ef;color:#2f8a59}.acceptance-status.rejected{background:#ffeaea;color:#bd4444}.dialog-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}@media(max-width:1000px){.overview-grid{grid-template-columns:1fr}.hero-meta{grid-template-columns:repeat(2,1fr)}.milestone-item{grid-template-columns:46px 1fr}.milestone-date,.milestone-item>.el-button{grid-column:2}.budget-overview{grid-template-columns:repeat(2,1fr)}}@media(max-width:700px){.hero-top,.hero-body{flex-direction:column;align-items:flex-start}.hero-actions{flex-wrap:wrap}.hero-meta{grid-template-columns:1fr}.budget-overview,.deliverable-grid,.timeline-grid{grid-template-columns:1fr}.record-row{grid-template-columns:38px 1fr}.record-date,.record-amount{grid-column:2;text-align:left}.dialog-grid{grid-template-columns:1fr}}
</style>
