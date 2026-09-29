<template>
  <div class="projects-page" v-loading="loading">
    <div class="toolbar-card">
      <div class="filters">
        <el-input v-model="query.keyword" placeholder="搜索项目名称或编号" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
      <el-button v-if="canCreate" type="primary" size="large" @click="openCreate"><el-icon><Plus /></el-icon> 新建项目</el-button>
    </div>

    <div class="project-summary"><strong>{{ projects.length }}</strong> 个项目 <span>·</span> 点击项目进入完整项目工作空间</div>

    <div v-if="projects.length" class="project-grid">
      <article v-for="project in projects" :key="project.projectId" class="project-card" @click="openProject(project.projectId)">
        <div class="card-top">
          <span class="project-no">{{ project.projectNo }}</span>
          <span class="status-pill" :class="project.status.toLowerCase()">{{ statusText(project.status) }}</span>
        </div>
        <h3>{{ project.projectName }}</h3>
        <p class="summary">{{ project.summary || '暂无项目简介' }}</p>
        <div class="owner-line"><el-avatar :size="28">{{ (project.ownerName || '项').slice(0,1) }}</el-avatar><span>{{ project.ownerName || '未指定' }}</span><i></i><span>{{ project.deptName || '未指定部门' }}</span></div>
        <div class="progress-box">
          <div><span>项目进度</span><strong>{{ project.progress || 0 }}%</strong></div>
          <el-progress :percentage="project.progress || 0" :stroke-width="8" :show-text="false" />
        </div>
        <div class="card-foot">
          <div><span>预算</span><strong>¥{{ compactMoney(project.totalBudget) }}</strong></div>
          <div><span>已使用</span><strong>¥{{ compactMoney(project.usedBudget) }}</strong></div>
          <div v-if="project.riskLevel !== 'NONE'" class="risk"><el-icon><WarningFilled /></el-icon>{{ project.riskLevel === 'HIGH' ? '高风险' : '需关注' }}</div>
          <div v-else class="healthy"><el-icon><CircleCheck /></el-icon>正常</div>
        </div>
      </article>
    </div>
    <el-empty v-else description="没有找到符合条件的项目" />

    <el-dialog v-model="formVisible" :title="form.projectId ? '编辑项目申报' : '创建科研项目'" width="720px" destroy-on-close>
      <div class="ai-assist">
        <div class="ai-assist-head"><div><strong>AI 申报助手</strong><span>把模糊想法整理成可编辑草稿，不会自动提交</span></div><el-tag effect="plain" size="small">Copilot</el-tag></div>
        <div class="ai-input-row"><el-input v-model="aiDescription" placeholder="例如：想研究大模型在工业缺陷检测中的应用，希望一年内做出可验证原型" /><el-button :loading="aiLoading" @click="generateAiDraft"><el-icon><MagicStick /></el-icon> 帮我整理</el-button></div>
        <div v-if="aiContent" class="ai-result"><pre>{{ aiContent }}</pre><el-button size="small" type="primary" plain @click="applyAiDraft">应用到申报表单</el-button></div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid two">
          <el-form-item label="项目名称" prop="projectName"><el-input v-model="form.projectName" placeholder="例如：AI驱动工业质量检测关键技术研究" /></el-form-item>
          <el-form-item label="项目编号"><el-input v-model="form.projectNo" placeholder="留空自动生成" /></el-form-item>
        </div>
        <el-form-item label="项目简介"><el-input v-model="form.summary" type="textarea" :rows="2" placeholder="用一句话说明项目要解决的问题" /></el-form-item>
        <div class="form-grid two">
          <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="计划结束日期" prop="plannedEndDate"><el-date-picker v-model="form.plannedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </div>
        <el-form-item label="项目总预算（元）"><el-input-number v-model="form.totalBudget" :min="0" :step="10000" controls-position="right" style="width:100%" /></el-form-item>
        <el-form-item label="研究目标"><el-input v-model="form.researchObjectives" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="研究内容"><el-input v-model="form.researchContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="预期成果"><el-input v-model="form.expectedDeliverables" type="textarea" :rows="2" placeholder="论文、专利、软件、技术报告等" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProject">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { createResearchProject, listResearchProjects, updateResearchProject, generateResearchProposal } from '@/api/research'
import useUserStore from '@/store/modules/user'

const router = useRouter(); const userStore = useUserStore(); const { proxy } = getCurrentInstance()
const loading = ref(false); const saving = ref(false); const formVisible = ref(false); const projects = ref([]); const aiDescription = ref(''); const aiContent = ref(''); const aiLoading = ref(false)
const query = reactive({ keyword:'', status:'' })
const canCreate = computed(() => !userStore.roles.includes('research_manager'))
const statusOptions = [
  {value:'DRAFT',label:'草稿'},{value:'PENDING_APPROVAL',label:'待审批'},{value:'APPROVED',label:'已立项'},
  {value:'IN_PROGRESS',label:'执行中'},{value:'PENDING_ACCEPTANCE',label:'待验收'},{value:'COMPLETED',label:'已结项'},{value:'REJECTED',label:'已驳回'}
]
const blankForm = () => ({ projectId:null, projectNo:'', projectName:'', summary:'', startDate:'', plannedEndDate:'', totalBudget:0, researchObjectives:'', researchContent:'', expectedDeliverables:'' })
const form = reactive(blankForm())
const rules = { projectName:[{required:true,message:'请输入项目名称',trigger:'blur'}], plannedEndDate:[{required:true,message:'请选择计划结束时间',trigger:'change'}] }
async function load(){ loading.value=true; try{ const res=await listResearchProjects(query); projects.value=res.data || res || [] } finally{loading.value=false} }
function openCreate(){ Object.assign(form,blankForm()); aiDescription.value=''; aiContent.value=''; formVisible.value=true }
function openProject(id){ router.push(`/research/projects/${id}`) }

async function generateAiDraft(){
  if(!aiDescription.value.trim()) return proxy.$modal.msgWarning('请先描述项目想法')
  aiLoading.value=true
  try{ const res=await generateResearchProposal(aiDescription.value.trim()); const payload=res.data||{}; aiContent.value=payload.content||''; if(!payload.enabled) proxy.$modal.msgWarning('AI API 未配置，已保留核心功能可用性') }finally{aiLoading.value=false}
}
function section(text, title, nextTitles){
  const escaped=title.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')
  const next=nextTitles.length?`(?=${nextTitles.map(t=>`(?:#+\\s*)?${t}[：:]?`).join('|')}|$)`:'$'
  const re=new RegExp(`(?:#+\\s*)?${escaped}[：:]?\\s*([\\s\\S]*?)${next}`,'i')
  const m=text.match(re); return m?m[1].trim().replace(/^[-*]\s*/gm,''):''
}
function applyAiDraft(){
  const t=aiContent.value
  const summary=section(t,'项目简介',['研究目标','研究内容','预期成果'])
  const objectives=section(t,'研究目标',['研究内容','预期成果'])
  const content=section(t,'研究内容',['预期成果'])
  const outputs=section(t,'预期成果',[])
  if(summary) form.summary=summary; if(objectives) form.researchObjectives=objectives; if(content) form.researchContent=content; if(outputs) form.expectedDeliverables=outputs
  proxy.$modal.msgSuccess('AI 草稿已填入表单，请人工确认后保存')
}

async function saveProject(){ await proxy.$refs.formRef.validate(); saving.value=true; try{ if(form.projectId) await updateResearchProject(form.projectId,form); else await createResearchProject(form); proxy.$modal.msgSuccess('项目草稿已保存'); formVisible.value=false; load() } finally{saving.value=false} }
function statusText(status){return ({DRAFT:'草稿',PENDING_APPROVAL:'待审批',APPROVED:'已立项',IN_PROGRESS:'执行中',PENDING_ACCEPTANCE:'待验收',COMPLETED:'已结项',REJECTED:'已驳回',TERMINATED:'已终止'})[status]||status}
function compactMoney(v){const n=Number(v||0);return n>=10000?(n/10000).toFixed(n%10000===0?0:1)+'万':n.toLocaleString()}
onMounted(load)
</script>

<style scoped lang="scss">
.projects-page{display:flex;flex-direction:column;gap:18px}.toolbar-card{background:#fff;border:1px solid #e8edf3;border-radius:14px;padding:14px 16px;display:flex;justify-content:space-between;gap:16px}.filters{display:flex;gap:10px;flex:1}.filters .el-input{max-width:360px}.filters .el-select{width:160px}.project-summary{font-size:12px;color:#8793a4}.project-summary strong{color:#26364a;font-size:14px}.project-summary span{margin:0 6px;color:#c2c9d2}.project-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}.project-card{background:#fff;border:1px solid #e7ecf2;border-radius:16px;padding:20px;cursor:pointer;transition:.22s;box-shadow:0 3px 14px rgba(20,37,63,.025)}.project-card:hover{transform:translateY(-3px);border-color:#bed7f5;box-shadow:0 12px 30px rgba(38,99,170,.09)}.card-top{display:flex;justify-content:space-between;align-items:center}.project-no{font-size:11px;color:#8d98a8;letter-spacing:.3px}.status-pill{font-size:10px;padding:4px 8px;border-radius:99px;background:#f1f4f8;color:#677386}.status-pill.in_progress{background:#eaf4ff;color:#2678d5}.status-pill.pending_approval,.status-pill.pending_acceptance{background:#fff4e5;color:#c97812}.status-pill.completed{background:#eaf8f0;color:#218750}.status-pill.rejected{background:#fff0f0;color:#d14747}.project-card h3{font-size:16px;margin:14px 0 8px;line-height:1.45;min-height:46px}.summary{height:40px;overflow:hidden;margin:0;color:#7f8b9b;font-size:12px;line-height:1.65}.owner-line{display:flex;align-items:center;gap:7px;margin:16px 0;color:#687588;font-size:11px}.owner-line i{width:3px;height:3px;border-radius:50%;background:#bdc5cf}.progress-box{padding:13px 14px;background:#f8fafc;border-radius:10px}.progress-box>div{display:flex;justify-content:space-between;margin-bottom:7px;font-size:11px;color:#8995a5}.progress-box strong{color:#30445f}.card-foot{display:flex;align-items:end;gap:20px;margin-top:14px;padding-top:14px;border-top:1px solid #eef1f5}.card-foot>div{display:flex;flex-direction:column;gap:4px}.card-foot span{font-size:10px;color:#9aa4b2}.card-foot strong{font-size:12px}.card-foot .risk,.card-foot .healthy{margin-left:auto;flex-direction:row;align-items:center;gap:4px;font-size:11px}.risk{color:#d25d42}.healthy{color:#459567}.ai-assist{margin-bottom:18px;padding:14px;border-radius:12px;background:#f4f8fd;border:1px solid #dfeaf7}.ai-assist-head{display:flex;justify-content:space-between;align-items:center}.ai-assist-head>div{display:flex;flex-direction:column;gap:3px}.ai-assist-head strong{font-size:12px}.ai-assist-head span{font-size:10px;color:#8696a9}.ai-input-row{display:flex;gap:8px;margin-top:10px}.ai-result{margin-top:10px;padding:10px;background:#fff;border:1px solid #e3eaf2;border-radius:8px}.ai-result pre{white-space:pre-wrap;font-family:inherit;font-size:11px;line-height:1.6;color:#55657a;max-height:180px;overflow:auto;margin:0 0 8px}.form-grid.two{display:grid;grid-template-columns:1fr 1fr;gap:16px}@media(max-width:1200px){.project-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:760px){.toolbar-card,.filters{flex-direction:column}.filters .el-input,.filters .el-select{width:100%;max-width:none}.project-grid{grid-template-columns:1fr}.form-grid.two{grid-template-columns:1fr}}
</style>
