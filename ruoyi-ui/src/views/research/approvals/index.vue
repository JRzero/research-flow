<template>
  <div v-loading="loading" class="approval-page">
    <div v-if="!canApprove" class="notice-card">
      <el-icon><InfoFilled /></el-icon>
      <div><strong>当前账号为只读视角</strong><p>审批动作仅科研管理员执行，你仍可以查看项目状态。</p></div>
    </div>

    <section v-for="group in groups" :key="group.key" class="approval-section">
      <div class="section-head">
        <div><h3>{{ group.title }}</h3><p>{{ group.description }}</p></div>
        <span>{{ group.items.length }}</span>
      </div>
      <div v-if="group.items.length" class="approval-list">
        <article v-for="p in group.items" :key="p.projectId" class="approval-card">
          <div class="project-info" @click="openProject(p.projectId)">
            <span class="project-no">{{ p.projectNo }}</span>
            <h4>{{ p.projectName }}</h4>
            <p>{{ p.summary || '暂无项目简介' }}</p>
            <div class="meta"><span>{{ p.ownerName }}</span><i></i><span>{{ p.deptName }}</span><i></i><span>预算 ¥{{ money(p.totalBudget) }}</span></div>
          </div>
          <div class="approval-status">
            <span class="status-dot" :class="p.status.toLowerCase()"></span>
            {{ statusText(p.status) }}
          </div>
          <div class="actions" v-if="canApprove">
            <template v-if="p.status==='PENDING_APPROVAL'">
              <el-button @click="reject(p)">驳回</el-button>
              <el-button type="primary" @click="approve(p)">审批通过</el-button>
            </template>
            <template v-else-if="p.status==='APPROVED'">
              <el-button type="primary" plain @click="start(p)">启动项目</el-button>
            </template>
            <template v-else-if="p.status==='PENDING_ACCEPTANCE'">
              <el-button @click="rejectAcceptance(p)">退回整改</el-button>
              <el-button type="success" @click="approveAcceptance(p)">验收通过</el-button>
            </template>
          </div>
        </article>
      </div>
      <div v-else class="empty-line">当前没有{{ group.title }}</div>
    </section>
  </div>
</template>
<script setup>
import { listResearchProjects, approveResearchProject, rejectResearchProject, startResearchProject, approveResearchAcceptance, rejectResearchAcceptance } from '@/api/research'
import useUserStore from '@/store/modules/user'
const router=useRouter(); const userStore=useUserStore(); const {proxy}=getCurrentInstance(); const loading=ref(false)
const projects=ref([])
const canApprove=computed(()=>userStore.roles.includes('admin')||userStore.roles.includes('research_admin'))
const groups=computed(()=>[
 {key:'approval',title:'待审批项目',description:'等待科研管理人员确认立项',items:projects.value.filter(p=>p.status==='PENDING_APPROVAL')},
 {key:'start',title:'待启动项目',description:'审批已通过，等待进入执行阶段',items:projects.value.filter(p=>p.status==='APPROVED')},
 {key:'acceptance',title:'待验收项目',description:'项目负责人已提交结题材料',items:projects.value.filter(p=>p.status==='PENDING_ACCEPTANCE')}
])
async function load(){loading.value=true;try{const r=await listResearchProjects({});projects.value=r.data||r||[]}finally{loading.value=false}}
function openProject(id){router.push(`/research/projects/${id}`)}
async function promptComment(title,placeholder){try{const {value}=await ElMessageBox.prompt(placeholder,title,{confirmButtonText:'确认',cancelButtonText:'取消',inputType:'textarea'});return value||''}catch{return null}}
async function approve(p){const c=await promptComment('审批通过','填写审批意见（可选）');if(c===null)return;await approveResearchProject(p.projectId,c);proxy.$modal.msgSuccess('审批通过');load()}
async function reject(p){const c=await promptComment('驳回项目','请填写驳回原因');if(c===null)return;await rejectResearchProject(p.projectId,c);proxy.$modal.msgSuccess('已驳回');load()}
async function start(p){await proxy.$modal.confirm(`确认启动“${p.projectName}”吗？`);await startResearchProject(p.projectId);proxy.$modal.msgSuccess('项目已启动');load()}
async function approveAcceptance(p){const c=await promptComment('验收通过','填写验收意见');if(c===null)return;await approveResearchAcceptance(p.projectId,c);proxy.$modal.msgSuccess('项目已结项');load()}
async function rejectAcceptance(p){const c=await promptComment('退回整改','请填写需要整改的内容');if(c===null)return;await rejectResearchAcceptance(p.projectId,c);proxy.$modal.msgSuccess('已退回项目负责人整改');load()}
function statusText(s){return ({PENDING_APPROVAL:'待审批',APPROVED:'已立项',PENDING_ACCEPTANCE:'待验收'})[s]||s}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
onMounted(load)
</script>
<style scoped lang="scss">
.approval-page{display:flex;flex-direction:column;gap:20px}.notice-card{display:flex;gap:12px;align-items:center;padding:16px 18px;background:#eef6ff;color:#2f6fae;border:1px solid #d5e8fb;border-radius:12px}.notice-card .el-icon{font-size:22px}.notice-card p{margin:4px 0 0;font-size:11px;color:#6c8cad}.approval-section{background:#fff;border:1px solid #e7ecf2;border-radius:15px;padding:20px}.section-head{display:flex;justify-content:space-between;align-items:center;padding-bottom:14px;border-bottom:1px solid #edf1f5}.section-head h3{margin:0;font-size:15px}.section-head p{margin:5px 0 0;color:#99a3b1;font-size:11px}.section-head>span{width:30px;height:30px;border-radius:9px;background:#f0f5fb;color:#3b6f9f;display:grid;place-items:center;font-weight:700}.approval-list{display:flex;flex-direction:column}.approval-card{display:grid;grid-template-columns:minmax(0,1fr) 120px auto;gap:24px;align-items:center;padding:18px 2px;border-bottom:1px solid #eef1f4}.approval-card:last-child{border-bottom:0}.project-info{cursor:pointer}.project-info h4{margin:5px 0;font-size:14px}.project-info>p{margin:0;color:#8390a1;font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:650px}.project-no{font-size:10px;color:#a0a9b5}.meta{display:flex;gap:8px;align-items:center;margin-top:8px;color:#8e99a8;font-size:10px}.meta i{width:3px;height:3px;border-radius:50%;background:#c3cad2}.approval-status{font-size:11px;color:#59687b;display:flex;align-items:center;gap:7px}.status-dot{width:8px;height:8px;border-radius:50%;background:#f2c94c}.status-dot.approved{background:#2d9cdb}.status-dot.pending_acceptance{background:#27ae60}.actions{display:flex;justify-content:flex-end}.empty-line{text-align:center;color:#a0a9b6;font-size:12px;padding:28px}.section-head+ .empty-line{border:0}@media(max-width:850px){.approval-card{grid-template-columns:1fr}.actions{justify-content:flex-start}}
</style>
