<template>
  <div class="projects-page" v-loading="loading">
    <section class="toolbar-card" aria-label="项目筛选">
      <div class="filters">
        <label class="filter-field">
          <span>搜索项目</span>
          <el-input
            v-model="query.keyword"
            placeholder="项目名称或编号"
            clearable
            :prefix-icon="Search"
            @keyup.enter="load"
            @clear="load"
          />
        </label>
        <label class="filter-field status-filter">
          <span>项目状态</span>
          <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
      </div>

      <div class="toolbar-actions">
        <div class="project-count"><strong class="rf-tabular">{{ projects.length }}</strong><span>个项目</span></div>
        <el-button v-if="canCreate" type="primary" @click="openCreate">
          <el-icon aria-hidden="true"><Plus /></el-icon>
          新建项目
        </el-button>
      </div>
    </section>

    <div class="project-summary">
      <span>项目工作空间集中管理进度、里程碑、经费、成果和验收。</span>
      <button v-if="query.keyword || query.status" type="button" class="clear-filter" @click="resetFilters">清除筛选</button>
    </div>

    <div v-if="projects.length" class="project-grid">
      <button
        v-for="project in projects"
        :key="project.projectId"
        class="project-card"
        type="button"
        @click="openProject(project.projectId)"
      >
        <div class="card-top">
          <span class="project-no">{{ project.projectNo }}</span>
          <span class="status-pill" :class="project.status.toLowerCase()">
            <i aria-hidden="true"></i>{{ statusText(project.status) }}
          </span>
        </div>

        <h2>{{ project.projectName }}</h2>
        <p class="summary">{{ project.summary || '暂无项目简介' }}</p>

        <div class="owner-line">
          <el-avatar :size="32" aria-hidden="true">{{ (project.ownerName || '项').slice(0,1) }}</el-avatar>
          <div>
            <strong>{{ project.ownerName || '未指定负责人' }}</strong>
            <span>{{ project.deptName || '未指定部门' }}</span>
          </div>
        </div>

        <div class="progress-box">
          <div class="progress-label">
            <span>项目进度</span>
            <strong class="rf-tabular">{{ project.progress || 0 }}%</strong>
          </div>
          <el-progress :percentage="project.progress || 0" :stroke-width="8" :show-text="false" />
        </div>

        <div class="card-foot">
          <div>
            <span>总预算</span>
            <strong class="rf-tabular">¥{{ compactMoney(project.totalBudget) }}</strong>
          </div>
          <div>
            <span>已使用</span>
            <strong class="rf-tabular">¥{{ compactMoney(project.usedBudget) }}</strong>
          </div>
          <span v-if="project.riskLevel !== 'NONE'" class="risk-indicator">
            <el-icon aria-hidden="true"><WarningFilled /></el-icon>
            {{ project.riskLevel === 'HIGH' ? '高风险' : '需关注' }}
          </span>
          <span v-else class="healthy-indicator">
            <el-icon aria-hidden="true"><CircleCheck /></el-icon>
            正常
          </span>
        </div>

        <div class="card-open">
          进入项目工作空间
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
        </div>
      </button>
    </div>

    <el-empty v-else description="没有找到符合条件的项目">
      <el-button v-if="query.keyword || query.status" @click="resetFilters">清除筛选</el-button>
      <el-button v-else-if="canCreate" type="primary" @click="openCreate">创建第一个项目</el-button>
    </el-empty>

    <el-dialog
      v-model="formVisible"
      :title="form.projectId ? '编辑项目申报' : '创建科研项目'"
      width="720px"
      class="project-dialog"
      destroy-on-close
    >
      <section class="ai-assist" aria-labelledby="ai-assist-title">
        <div class="ai-assist-head">
          <div class="ai-title">
            <span class="ai-icon" aria-hidden="true"><el-icon><MagicStick /></el-icon></span>
            <div>
              <strong id="ai-assist-title">AI 申报助手</strong>
              <span>将模糊想法整理成可编辑草稿，不会自动提交。</span>
            </div>
          </div>
          <span class="copilot-chip">Copilot</span>
        </div>

        <label class="ai-input-label" for="research-ai-description">描述你的项目想法</label>
        <div class="ai-input-row">
          <el-input
            id="research-ai-description"
            v-model="aiDescription"
            placeholder="例如：研究大模型在工业缺陷检测中的应用，一年内完成可验证原型"
          />
          <el-button :loading="aiLoading" @click="generateAiDraft">
            <el-icon aria-hidden="true"><MagicStick /></el-icon>
            帮我整理
          </el-button>
        </div>
        <div v-if="aiContent" class="ai-result">
          <pre>{{ aiContent }}</pre>
          <el-button type="primary" plain @click="applyAiDraft">应用到申报表单</el-button>
        </div>
      </section>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-section-title">基本信息</div>
        <div class="form-grid two">
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="form.projectName" placeholder="例如：AI驱动工业质量检测关键技术研究" />
          </el-form-item>
          <el-form-item label="项目编号">
            <el-input v-model="form.projectNo" placeholder="留空自动生成" />
          </el-form-item>
        </div>

        <el-form-item label="项目简介">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="用一句话说明项目要解决的问题" />
        </el-form-item>

        <div class="form-grid two">
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="计划结束日期" prop="plannedEndDate">
            <el-date-picker v-model="form.plannedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </div>

        <el-form-item label="项目总预算（元）">
          <el-input-number v-model="form.totalBudget" :min="0" :step="10000" controls-position="right" style="width:100%" />
        </el-form-item>

        <div class="form-section-title">研究计划</div>
        <el-form-item label="研究目标">
          <el-input v-model="form.researchObjectives" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="研究内容">
          <el-input v-model="form.researchContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预期成果">
          <el-input v-model="form.expectedDeliverables" type="textarea" :rows="2" placeholder="论文、专利、软件、技术报告等" />
        </el-form-item>

        <div class="form-section-title">申报材料</div>
        <el-form-item label="项目附件">
          <ResearchAttachmentUpload v-model="form.applicationAttachments" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <span>保存后仍为草稿，可在项目工作空间中继续完善并提交。</span>
          <div>
            <el-button @click="formVisible=false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveProject">保存草稿</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import ResearchAttachmentUpload from '@/components/ResearchAttachmentUpload/index.vue'
import { createResearchProject, listResearchProjects, updateResearchProject, generateResearchProposal } from '@/api/research'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()

const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const projects = ref([])
const aiDescription = ref('')
const aiContent = ref('')
const aiLoading = ref(false)
const query = reactive({ keyword: '', status: '' })

const canCreate = computed(() =>
  userStore.roles.includes('admin') ||
  userStore.roles.includes('research_admin') ||
  userStore.roles.includes('research_owner')
)

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING_APPROVAL', label: '待审批' },
  { value: 'APPROVED', label: '已立项' },
  { value: 'IN_PROGRESS', label: '执行中' },
  { value: 'PENDING_ACCEPTANCE', label: '待验收' },
  { value: 'COMPLETED', label: '已结项' },
  { value: 'REJECTED', label: '已驳回' }
]

const blankForm = () => ({
  projectId: null,
  projectNo: '',
  projectName: '',
  summary: '',
  startDate: '',
  plannedEndDate: '',
  totalBudget: 0,
  researchObjectives: '',
  researchContent: '',
  expectedDeliverables: '',
  applicationAttachments: ''
})

const form = reactive(blankForm())
const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  plannedEndDate: [{ required: true, message: '请选择计划结束时间', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const res = await listResearchProjects(query)
    projects.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  load()
}

function openCreate() {
  Object.assign(form, blankForm())
  aiDescription.value = ''
  aiContent.value = ''
  formVisible.value = true
}

function openProject(id) { router.push('/research/projects/' + id) }

async function generateAiDraft() {
  if (!aiDescription.value.trim()) return proxy.$modal.msgWarning('请先描述项目想法')
  aiLoading.value = true
  try {
    const res = await generateResearchProposal(aiDescription.value.trim())
    const payload = res.data || {}
    aiContent.value = payload.content || ''
    if (!payload.enabled) proxy.$modal.msgWarning('AI API 未配置，核心业务仍可正常使用')
  } finally {
    aiLoading.value = false
  }
}

function section(text, title, nextTitles) {
  const start = text.indexOf(title)
  if (start < 0) return ''
  const contentStart = start + title.length
  let end = text.length
  nextTitles.forEach((nextTitle) => {
    const index = text.indexOf(nextTitle, contentStart)
    if (index >= 0 && index < end) end = index
  })
  return text.slice(contentStart, end).replace(/^[：:\s#-]*/, '').trim().replace(/^[-*]\s*/gm, '')
}

function applyAiDraft() {
  const text = aiContent.value
  const summary = section(text, '项目简介', ['研究目标', '研究内容', '预期成果'])
  const objectives = section(text, '研究目标', ['研究内容', '预期成果'])
  const researchContent = section(text, '研究内容', ['预期成果'])
  const outputs = section(text, '预期成果', [])
  if (summary) form.summary = summary
  if (objectives) form.researchObjectives = objectives
  if (researchContent) form.researchContent = researchContent
  if (outputs) form.expectedDeliverables = outputs
  proxy.$modal.msgSuccess('AI 草稿已填入表单，请人工确认后保存')
}

async function saveProject() {
  await proxy.$refs.formRef.validate()
  saving.value = true
  try {
    if (form.projectId) await updateResearchProject(form.projectId, form)
    else await createResearchProject(form)
    proxy.$modal.msgSuccess('项目草稿已保存')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function statusText(status) {
  return ({
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '已立项',
    IN_PROGRESS: '执行中',
    PENDING_ACCEPTANCE: '待验收',
    COMPLETED: '已结项',
    REJECTED: '已驳回',
    TERMINATED: '已终止'
  })[status] || status
}

function compactMoney(v) {
  const n = Number(v || 0)
  return n >= 10000 ? (n / 10000).toFixed(n % 10000 === 0 ? 0 : 1) + '万' : n.toLocaleString()
}

onMounted(load)
</script>

<style scoped lang="scss">
.projects-page { display: flex; flex-direction: column; gap: 16px; }

.toolbar-card {
  padding: 14px 16px;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  background: var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.filters { flex: 1; display: flex; align-items: flex-end; gap: 12px; }
.filter-field { min-width: 0; display: flex; flex-direction: column; gap: 7px; }
.filter-field > span { color: var(--rf-text-secondary); font-size: 12px; font-weight: 650; }
.filter-field:first-child { width: min(420px, 100%); }
.status-filter { width: 170px; }
.toolbar-actions { display: flex; align-items: center; gap: 14px; }
.project-count { display: flex; align-items: baseline; gap: 5px; color: var(--rf-text-muted); font-size: 12px; white-space: nowrap; }
.project-count strong { color: var(--rf-text); font-size: 20px; }

.project-summary { min-height: 28px; display: flex; justify-content: space-between; align-items: center; gap: 16px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.clear-filter { min-height: 32px; padding: 0 8px; border: 0; background: transparent; color: var(--rf-primary); font-size: 12px; font-weight: 600; cursor: pointer; }

.project-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.project-card {
  min-width: 0;
  padding: 19px;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  display: block;
  background: var(--rf-surface);
  color: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--rf-shadow-sm);
  transition: transform var(--rf-motion-base) ease, border-color var(--rf-motion-base) ease, box-shadow var(--rf-motion-base) ease;
}
.project-card:hover { transform: translateY(-2px); border-color: var(--rf-primary-border); box-shadow: var(--rf-shadow-md); }
.project-card:active { transform: translateY(0) scale(.995); }
.project-card:focus-visible { box-shadow: var(--rf-focus), var(--rf-shadow-md); }

.card-top { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.project-no { color: var(--rf-text-muted); font-size: 12px; font-weight: 550; letter-spacing: .2px; }
.status-pill {
  min-height: 25px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  font-size: 12px;
  font-weight: 600;
}
.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: var(--rf-text-muted); }
.status-pill.in_progress { background: var(--rf-primary-soft); color: var(--rf-primary-hover); }
.status-pill.in_progress i { background: var(--rf-primary); }
.status-pill.pending_approval, .status-pill.pending_acceptance { background: var(--rf-warning-soft); color: var(--rf-warning); }
.status-pill.pending_approval i, .status-pill.pending_acceptance i { background: var(--rf-warning); }
.status-pill.completed { background: var(--rf-success-soft); color: var(--rf-success); }
.status-pill.completed i { background: var(--rf-success); }
.status-pill.rejected, .status-pill.terminated { background: var(--rf-danger-soft); color: var(--rf-danger); }
.status-pill.rejected i, .status-pill.terminated i { background: var(--rf-danger); }

.project-card h2 {
  min-height: 48px;
  margin: 14px 0 7px;
  color: var(--rf-text);
  font-size: 16px;
  font-weight: 680;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.summary {
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: var(--rf-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.owner-line { margin: 16px 0; display: flex; align-items: center; gap: 9px; }
.owner-line > div { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.owner-line strong { color: var(--rf-text-secondary); font-size: 12px; font-weight: 650; }
.owner-line span { overflow: hidden; color: var(--rf-text-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }

.progress-box { padding: 12px 13px; border-radius: 10px; background: var(--rf-surface-subtle); }
.progress-label { margin-bottom: 7px; display: flex; justify-content: space-between; color: var(--rf-text-muted); font-size: 12px; }
.progress-label strong { color: var(--rf-text-secondary); }

.card-foot { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--rf-border); display: flex; align-items: flex-end; gap: 18px; }
.card-foot > div { display: flex; flex-direction: column; gap: 4px; }
.card-foot span { color: var(--rf-text-muted); font-size: 12px; }
.card-foot strong { color: var(--rf-text-secondary); font-size: 13px; }
.risk-indicator, .healthy-indicator {
  margin-left: auto;
  min-height: 28px;
  padding: 0 8px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 650;
}
.risk-indicator { background: var(--rf-danger-soft); color: var(--rf-danger); }
.healthy-indicator { background: var(--rf-success-soft); color: var(--rf-success); }
.card-open { margin-top: 14px; display: flex; align-items: center; justify-content: flex-end; gap: 5px; color: var(--rf-primary); font-size: 12px; font-weight: 650; }

.ai-assist { margin-bottom: 20px; padding: 16px; border: 1px solid var(--rf-primary-border); border-radius: 12px; background: var(--rf-primary-soft); }
.ai-assist-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.ai-title { display: flex; align-items: flex-start; gap: 10px; }
.ai-icon { width: 36px; height: 36px; flex: 0 0 36px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-surface); color: var(--rf-primary); box-shadow: var(--rf-shadow-sm); }
.ai-title > div { display: flex; flex-direction: column; gap: 4px; }
.ai-title strong { color: var(--rf-text); font-size: 13px; }
.ai-title span { color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.copilot-chip { min-height: 26px; padding: 0 8px; border: 1px solid var(--rf-primary-border); border-radius: 999px; display: inline-flex; align-items: center; color: var(--rf-primary); font-size: 11px; font-weight: 700; }
.ai-input-label { display: block; margin: 14px 0 7px; color: var(--rf-text-secondary); font-size: 12px; font-weight: 650; }
.ai-input-row { display: flex; gap: 8px; }
.ai-input-row .el-input { min-width: 0; flex: 1; }
.ai-result { margin-top: 10px; padding: 12px; border: 1px solid var(--rf-border); border-radius: 10px; background: var(--rf-surface); }
.ai-result pre { max-height: 190px; margin: 0 0 10px; overflow: auto; white-space: pre-wrap; color: var(--rf-text-secondary); font-family: inherit; font-size: 12px; line-height: 1.65; }

.form-section-title { margin: 4px 0 16px; padding-bottom: 8px; border-bottom: 1px solid var(--rf-border); color: var(--rf-text); font-size: 13px; font-weight: 700; }
.form-grid.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.dialog-footer { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.dialog-footer > span { max-width: 420px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.dialog-footer > div { display: flex; gap: 8px; }

:deep(.project-dialog) { max-width: calc(100vw - 32px); }
:deep(.project-dialog .el-dialog__body) { padding-top: 10px; }

@media (max-width: 1200px) {
  .project-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .toolbar-card { align-items: stretch; flex-direction: column; }
  .filters { align-items: stretch; flex-direction: column; }
  .filter-field:first-child, .status-filter { width: 100%; max-width: none; }
  .toolbar-actions { justify-content: space-between; }
  .project-grid { grid-template-columns: 1fr; }
  .project-card h2 { min-height: 0; }
  .form-grid.two { grid-template-columns: 1fr; gap: 0; }
  .ai-input-row { flex-direction: column; }
  .dialog-footer { align-items: stretch; flex-direction: column; }
  .dialog-footer > div { justify-content: flex-end; }
}
@media (max-width: 420px) {
  .toolbar-actions { align-items: stretch; flex-direction: column; }
  .toolbar-actions .el-button { width: 100%; }
  .card-foot { gap: 12px; }
}
</style>
