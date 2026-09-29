<template>
  <div v-loading="loading" class="approval-page">
    <div v-if="!canApprove" class="notice-card" role="status">
      <div class="notice-icon" aria-hidden="true"><el-icon><InfoFilled /></el-icon></div>
      <div>
        <strong>当前账号为只读视角</strong>
        <p>审批动作仅由科研管理员执行，你仍可以查看所有待办项目和当前状态。</p>
      </div>
    </div>

    <section class="queue-overview" aria-label="审批队列概览">
      <article v-for="group in groups" :key="group.key" class="queue-card">
        <div class="queue-icon" :class="group.key" aria-hidden="true">
          <el-icon><component :is="group.icon" /></el-icon>
        </div>
        <div>
          <span>{{ group.title }}</span>
          <strong class="rf-tabular">{{ group.items.length }}</strong>
        </div>
        <p>{{ group.shortDescription }}</p>
      </article>
    </section>

    <section v-for="group in groups" :key="group.key + '-section'" class="approval-section">
      <div class="section-head">
        <div>
          <h2>{{ group.title }}</h2>
          <p>{{ group.description }}</p>
        </div>
        <span class="count-chip"><strong class="rf-tabular">{{ group.items.length }}</strong> 项</span>
      </div>

      <div v-if="group.items.length" class="approval-list">
        <article v-for="p in group.items" :key="p.projectId" class="approval-card">
          <button type="button" class="project-info" @click="openProject(p.projectId)">
            <div class="project-heading">
              <span class="project-no">{{ p.projectNo }}</span>
              <span class="approval-status" :class="p.status.toLowerCase()">
                <i aria-hidden="true"></i>{{ statusText(p.status) }}
              </span>
            </div>
            <h3>{{ p.projectName }}</h3>
            <p>{{ p.summary || '暂无项目简介' }}</p>
            <div class="meta">
              <span>{{ p.ownerName || '未指定负责人' }}</span>
              <span>{{ p.deptName || '未指定部门' }}</span>
              <span class="rf-tabular">预算 ¥{{ money(p.totalBudget) }}</span>
            </div>
          </button>

          <div v-if="canApprove" class="actions" :aria-label="p.projectName + ' 审批操作'">
            <template v-if="p.status === 'PENDING_APPROVAL'">
              <el-button @click="reject(p)">驳回</el-button>
              <el-button type="primary" @click="approve(p)">审批通过</el-button>
            </template>
            <template v-else-if="p.status === 'APPROVED'">
              <el-button type="primary" @click="start(p)">启动项目</el-button>
            </template>
            <template v-else-if="p.status === 'PENDING_ACCEPTANCE'">
              <el-button @click="rejectAcceptance(p)">退回整改</el-button>
              <el-button type="success" @click="approveAcceptance(p)">验收通过</el-button>
            </template>
          </div>
          <div v-else class="read-only-label">只读</div>
        </article>
      </div>

      <div v-else class="empty-line">
        <el-icon aria-hidden="true"><CircleCheck /></el-icon>
        <span>当前没有{{ group.title }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  listResearchProjects,
  approveResearchProject,
  rejectResearchProject,
  startResearchProject,
  approveResearchAcceptance,
  rejectResearchAcceptance
} from '@/api/research'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()
const loading = ref(false)
const projects = ref([])

const canApprove = computed(() =>
  userStore.roles.includes('admin') || userStore.roles.includes('research_admin')
)

const groups = computed(() => [
  {
    key: 'approval',
    title: '待审批项目',
    shortDescription: '确认是否立项',
    description: '负责人已提交项目申报，等待科研管理人员确认立项。',
    icon: 'DocumentChecked',
    items: projects.value.filter(p => p.status === 'PENDING_APPROVAL')
  },
  {
    key: 'start',
    title: '待启动项目',
    shortDescription: '进入执行阶段',
    description: '立项审批已经通过，等待科研管理员启动项目。',
    icon: 'VideoPlay',
    items: projects.value.filter(p => p.status === 'APPROVED')
  },
  {
    key: 'acceptance',
    title: '待验收项目',
    shortDescription: '确认结项结果',
    description: '项目负责人已提交结题材料，等待验收意见。',
    icon: 'Finished',
    items: projects.value.filter(p => p.status === 'PENDING_ACCEPTANCE')
  }
])

async function load() {
  loading.value = true
  try {
    const res = await listResearchProjects({})
    projects.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

function openProject(id) { router.push('/research/projects/' + id) }

async function promptComment(title, placeholder) {
  try {
    const result = await ElMessageBox.prompt(placeholder, title, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    return result.value || ''
  } catch {
    return null
  }
}

async function approve(p) {
  const comment = await promptComment('审批通过', '填写审批意见（可选）')
  if (comment === null) return
  await approveResearchProject(p.projectId, comment)
  proxy.$modal.msgSuccess('审批通过')
  load()
}

async function reject(p) {
  const comment = await promptComment('驳回项目', '请填写驳回原因')
  if (comment === null) return
  await rejectResearchProject(p.projectId, comment)
  proxy.$modal.msgSuccess('已驳回')
  load()
}

async function start(p) {
  await proxy.$modal.confirm('确认启动“' + p.projectName + '”吗？')
  await startResearchProject(p.projectId)
  proxy.$modal.msgSuccess('项目已启动')
  load()
}

async function approveAcceptance(p) {
  const comment = await promptComment('验收通过', '填写验收意见')
  if (comment === null) return
  await approveResearchAcceptance(p.projectId, comment)
  proxy.$modal.msgSuccess('项目已结项')
  load()
}

async function rejectAcceptance(p) {
  const comment = await promptComment('退回整改', '请填写需要整改的内容')
  if (comment === null) return
  await rejectResearchAcceptance(p.projectId, comment)
  proxy.$modal.msgSuccess('已退回项目负责人整改')
  load()
}

function statusText(status) {
  return ({
    PENDING_APPROVAL: '待审批',
    APPROVED: '待启动',
    PENDING_ACCEPTANCE: '待验收'
  })[status] || status
}

function money(v) {
  return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

onMounted(load)
</script>

<style scoped lang="scss">
.approval-page { display: flex; flex-direction: column; gap: 16px; }

.notice-card {
  min-height: 68px;
  padding: 14px 16px;
  border: 1px solid var(--rf-primary-border);
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--rf-primary-soft);
}
.notice-icon { width: 38px; height: 38px; flex: 0 0 38px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-surface); color: var(--rf-primary); font-size: 19px; }
.notice-card strong { color: var(--rf-text); font-size: 13px; }
.notice-card p { margin: 4px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.5; }

.queue-overview { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.queue-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--rf-border);
  border-radius: 12px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 10px 12px;
  background: var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.queue-icon { width: 38px; height: 38px; border-radius: 10px; display: grid; place-items: center; background: var(--rf-warning-soft); color: var(--rf-warning); font-size: 18px; }
.queue-icon.start { background: var(--rf-primary-soft); color: var(--rf-primary); }
.queue-icon.acceptance { background: var(--rf-success-soft); color: var(--rf-success); }
.queue-card > div:nth-child(2) { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.queue-card span { color: var(--rf-text-secondary); font-size: 12px; font-weight: 650; }
.queue-card strong { color: var(--rf-text); font-size: 24px; }
.queue-card p { grid-column: 1 / -1; margin: 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.45; }

.approval-section {
  overflow: hidden;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  background: var(--rf-surface);
  box-shadow: var(--rf-shadow-sm);
}
.section-head { min-height: 72px; padding: 16px 20px; border-bottom: 1px solid var(--rf-border); display: flex; align-items: center; justify-content: space-between; gap: 16px; background: var(--rf-surface-subtle); }
.section-head h2 { margin: 0; color: var(--rf-text); font-size: 15px; font-weight: 700; }
.section-head p { margin: 5px 0 0; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.count-chip { min-height: 30px; padding: 0 9px; border: 1px solid var(--rf-border); border-radius: 999px; display: inline-flex; align-items: center; gap: 4px; background: var(--rf-surface); color: var(--rf-text-muted); font-size: 11px; white-space: nowrap; }
.count-chip strong { color: var(--rf-text); font-size: 13px; }

.approval-list { padding: 0 20px; display: flex; flex-direction: column; }
.approval-card {
  min-height: 112px;
  padding: 16px 0;
  border-bottom: 1px solid var(--rf-border);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
}
.approval-card:last-child { border-bottom: 0; }

.project-info {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.project-info:focus-visible { border-radius: 8px; box-shadow: var(--rf-focus); }
.project-heading { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.project-no { color: var(--rf-text-muted); font-size: 12px; font-weight: 550; }
.approval-status {
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--rf-warning-soft);
  color: var(--rf-warning);
  font-size: 11px;
  font-weight: 650;
}
.approval-status i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.approval-status.approved { background: var(--rf-primary-soft); color: var(--rf-primary); }
.approval-status.pending_acceptance { background: var(--rf-success-soft); color: var(--rf-success); }

.project-info h3 { margin: 7px 0 5px; color: var(--rf-text); font-size: 15px; font-weight: 680; line-height: 1.45; overflow-wrap: anywhere; }
.project-info > p { max-width: 820px; margin: 0; overflow: hidden; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.55; text-overflow: ellipsis; white-space: nowrap; }
.meta { margin-top: 8px; display: flex; align-items: center; flex-wrap: wrap; gap: 5px 13px; color: var(--rf-text-muted); font-size: 12px; }
.meta span + span::before { content: "·"; margin-right: 13px; color: var(--rf-border-strong); }

.actions { min-width: 190px; display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.read-only-label { min-width: 52px; min-height: 28px; padding: 0 8px; border-radius: 8px; display: grid; place-items: center; background: var(--rf-surface-subtle); color: var(--rf-text-muted); font-size: 11px; }

.empty-line { min-height: 86px; padding: 20px; display: flex; align-items: center; justify-content: center; gap: 7px; color: var(--rf-text-muted); font-size: 12px; }
.empty-line .el-icon { color: var(--rf-success); font-size: 17px; }

@media (max-width: 900px) {
  .queue-overview { grid-template-columns: 1fr; }
  .queue-card { grid-template-columns: 38px 1fr auto; align-items: center; }
  .queue-card p { grid-column: auto; }
  .approval-card { grid-template-columns: 1fr; gap: 12px; }
  .actions { justify-content: flex-start; }
  .read-only-label { width: fit-content; }
}
@media (max-width: 560px) {
  .section-head { align-items: flex-start; }
  .approval-list { padding: 0 16px; }
  .project-info > p { white-space: normal; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
  .meta span + span::before { display: none; }
  .actions { min-width: 0; }
  .actions .el-button { flex: 1; }
}
</style>
