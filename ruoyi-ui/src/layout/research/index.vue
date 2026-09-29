<template>
  <div class="research-shell">
    <a class="skip-link" href="#research-main">跳到主内容</a>
    <aside class="sidebar">
      <button class="brand" type="button" @click="router.push('/research/dashboard')"><img :src="brandMark" alt=""><div><strong>ResearchFlow</strong><span>科研项目治理</span></div></button>
      <nav class="nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" :class="['nav-item',{'mobile-hide':item.mobileHide}]">
          <el-icon><component :is="item.icon"/></el-icon><span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="user"><el-avatar :size="32">{{ (userStore.nickName||userStore.name||'R').slice(0,1) }}</el-avatar><div><strong>{{ userStore.nickName||userStore.name }}</strong><span>{{ roleText }}</span></div><el-dropdown @command="command"><el-button text circle><el-icon><MoreFilled/></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人中心</el-dropdown-item><el-dropdown-item v-if="isAdmin" command="admin">系统管理</el-dropdown-item><el-dropdown-item divided command="logout">退出</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div>
    </aside>
    <main id="research-main" class="main">
      <header class="header"><div><h1>{{ page[0] }}</h1><p>{{ page[1] }}</p></div><span class="role">{{ roleText }}</span></header>
      <section class="content"><router-view/></section>
    </main>
  </div>
</template>
<script setup>
import useUserStore from '@/store/modules/user'
import brandMark from '@/assets/logo/researchflow-mark.svg'
const router=useRouter(),route=useRoute(),userStore=useUserStore()
const navItems=[
 {path:'/research/dashboard',label:'工作台',icon:'HomeFilled'},
 {path:'/research/proposals',label:'项目申请',icon:'EditPen'},
 {path:'/research/projects',label:'科研项目',icon:'FolderOpened'},
 {path:'/research/approvals',label:'审批中心',icon:'Finished'},
 {path:'/research/risks',label:'风险问题',icon:'Warning'},
 {path:'/research/analytics',label:'数据概览',icon:'DataAnalysis',mobileHide:true}
]
const isAdmin=computed(()=>userStore.roles.includes('admin'))
const roleText=computed(()=>isAdmin.value||userStore.roles.includes('research_admin')?'科研管理员':userStore.roles.includes('research_manager')?'管理者':'科研用户')
const map={
 '/research/dashboard':['科研工作台','申报、执行与治理事项一屏掌握'],
 '/research/proposals':['项目申请','从研究想法到正式立项'],
 '/research/projects':['科研项目','正式立项后的计划、执行与结项'],
 '/research/approvals':['审批中心','集中处理申报、变更与验收'],
 '/research/risks':['风险与问题','区分潜在风险与已发生问题'],
 '/research/analytics':['数据概览','项目组合、进度与经费结构']
}
const page=computed(()=>route.path.startsWith('/research/projects/')?['项目工作空间','计划、执行、治理与结项集中处理']:(map[route.path]||['ResearchFlow','科研项目全过程治理']))
function command(c){if(c==='profile')router.push('/user/profile');if(c==='admin')router.push('/system/user');if(c==='logout')userStore.logOut().then(()=>router.push('/login'))}
</script>
<style scoped lang="scss">
.research-shell{min-height:100dvh;background:var(--rf-bg);color:var(--rf-text)}.skip-link{position:fixed;left:12px;top:8px;z-index:1000;transform:translateY(-140%);padding:8px 12px;background:#fff;border-radius:8px}.skip-link:focus{transform:none}
.sidebar{position:fixed;inset:0 auto 0 0;width:220px;background:var(--rf-sidebar);display:flex;flex-direction:column;z-index:40}.brand{height:64px;padding:0 16px;border:0;border-bottom:1px solid rgba(148,163,184,.14);background:none;color:#fff;display:flex;align-items:center;gap:10px;text-align:left;cursor:pointer}.brand img{width:34px;height:34px}.brand>div{display:flex;flex-direction:column;gap:2px}.brand strong{font-size:14px}.brand span{font-size:11px;color:var(--rf-sidebar-muted)}
.nav{padding:10px 8px;display:flex;flex-direction:column;gap:3px;flex:1}.nav-item{min-height:40px;padding:0 11px;border-radius:8px;display:flex;align-items:center;gap:10px;color:#a8b5c7;text-decoration:none;font-size:13px}.nav-item:hover{background:rgba(255,255,255,.06);color:#fff}.nav-item.router-link-active{background:rgba(37,99,235,.25);color:#fff;box-shadow:inset 2px 0 #60a5fa}.nav-item .el-icon{font-size:17px}
.user{margin:8px;padding:8px;border-top:1px solid rgba(148,163,184,.14);display:flex;align-items:center;gap:8px;color:#fff}.user>div{min-width:0;flex:1;display:flex;flex-direction:column}.user strong{font-size:12px;overflow:hidden;text-overflow:ellipsis}.user span{font-size:11px;color:var(--rf-sidebar-muted)}.user :deep(.el-button){color:#a8b5c7}
.main{min-height:100dvh;margin-left:220px}.header{height:64px;padding:0 20px;border-bottom:1px solid var(--rf-border);position:sticky;top:0;z-index:30;background:color-mix(in srgb,var(--rf-surface) 94%,transparent);backdrop-filter:blur(10px);display:flex;align-items:center;justify-content:space-between}.header h1{margin:0;font-size:17px;font-weight:720}.header p{margin:3px 0 0;color:var(--rf-text-muted);font-size:11px}.role{padding:5px 8px;border:1px solid var(--rf-border);border-radius:999px;background:var(--rf-surface-subtle);font-size:11px;color:var(--rf-text-secondary)}.content{width:min(100%,1540px);margin:auto;padding:16px 20px 36px}
@media(max-width:1024px){.sidebar{width:72px}.brand{justify-content:center;padding:0}.brand>div,.nav-item span,.user>div,.user .el-dropdown{display:none}.nav-item{justify-content:center;padding:0}.main{margin-left:72px}.content{padding:14px 16px}}
@media(max-width:767px){.research-shell{padding-bottom:68px}.sidebar{inset:auto 0 0 0;width:auto;height:68px;background:var(--rf-surface);border-top:1px solid var(--rf-border)}.brand,.user{display:none}.nav{padding:5px 8px;flex-direction:row;gap:2px}.nav-item{min-width:0;min-height:56px;flex:1;padding:4px;flex-direction:column;justify-content:center;gap:2px;color:var(--rf-text-muted);font-size:10px}.nav-item span{display:block}.nav-item.router-link-active{box-shadow:none;background:var(--rf-primary-soft);color:var(--rf-primary)}.nav-item.mobile-hide{display:none}.main{margin-left:0}.header{height:58px;padding:0 14px}.header p{display:none}.content{padding:12px 12px 24px}}
</style>
