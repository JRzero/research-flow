<template>
  <div class="research-shell">
    <aside class="research-sidebar">
      <div class="brand" @click="router.push('/research/dashboard')">
        <div class="brand-mark">R</div>
        <div>
          <div class="brand-name">ResearchFlow</div>
          <div class="brand-sub">科研项目管理</div>
        </div>
      </div>

      <nav class="product-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card">
          <el-avatar :size="38" :src="userStore.avatar" />
          <div class="user-meta">
            <strong>{{ userStore.nickName || userStore.name }}</strong>
            <span>{{ roleText }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-button text circle><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">系统管理</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>

    <main class="research-main">
      <header class="research-header">
        <div>
          <div class="page-title">{{ currentTitle }}</div>
          <div class="page-subtitle">{{ currentSubtitle }}</div>
        </div>
        <div class="header-actions">
          <el-tag v-if="isResearchAdmin" effect="plain" type="primary">科研管理员</el-tag>
          <el-tag v-else-if="isManager" effect="plain" type="success">管理者</el-tag>
          <el-tag v-else effect="plain">项目负责人</el-tag>
        </div>
      </header>
      <section class="research-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import useUserStore from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { path: '/research/dashboard', label: '工作台', icon: 'HomeFilled' },
  { path: '/research/projects', label: '项目', icon: 'FolderOpened' },
  { path: '/research/approvals', label: '审批中心', icon: 'Finished' },
  { path: '/research/risks', label: '风险项目', icon: 'Warning' },
  { path: '/research/analytics', label: '数据概览', icon: 'DataAnalysis' }
]

const isAdmin = computed(() => userStore.roles.includes('admin'))
const isResearchAdmin = computed(() => isAdmin.value || userStore.roles.includes('research_admin'))
const isManager = computed(() => userStore.roles.includes('research_manager'))
const roleText = computed(() => isResearchAdmin.value ? '科研管理员' : isManager.value ? '管理者' : '项目负责人')

const titleMap = {
  '/research/dashboard': ['科研项目工作台', '聚焦待办、项目进展与风险'],
  '/research/projects': ['科研项目', '从申报到结项的统一项目空间'],
  '/research/approvals': ['审批中心', '集中处理项目申报与成果验收'],
  '/research/risks': ['风险项目', '基于时间、进度和预算的可解释风险识别'],
  '/research/analytics': ['数据概览', '掌握科研项目整体运行情况']
}
const currentTitle = computed(() => route.path.startsWith('/research/projects/') ? '项目工作空间' : (titleMap[route.path]?.[0] || 'ResearchFlow'))
const currentSubtitle = computed(() => route.path.startsWith('/research/projects/') ? '项目全生命周期信息集中在一个工作空间中' : (titleMap[route.path]?.[1] || ''))

function handleCommand(command) {
  if (command === 'profile') router.push('/user/profile')
  if (command === 'admin') router.push('/system/user')
  if (command === 'logout') {
    userStore.logOut().then(() => router.push('/login'))
  }
}
</script>

<style scoped lang="scss">
.research-shell { min-height: 100vh; background: #f5f7fb; color: #162033; }
.research-sidebar { position: fixed; inset: 0 auto 0 0; width: 248px; background: #0d1b2a; color: #fff; display: flex; flex-direction: column; z-index: 20; box-shadow: 12px 0 32px rgba(13,27,42,.08); }
.brand { height: 88px; display: flex; align-items: center; gap: 12px; padding: 0 24px; cursor: pointer; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-mark { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; background: linear-gradient(135deg,#2f80ed,#56ccf2); font-weight: 800; font-size: 21px; }
.brand-name { font-size: 17px; font-weight: 700; letter-spacing: .2px; }
.brand-sub { color: #8fa4bb; font-size: 12px; margin-top: 3px; }
.product-nav { padding: 22px 14px; display: flex; flex-direction: column; gap: 6px; flex: 1; }
.nav-item { position: relative; display: flex; align-items: center; gap: 12px; min-height: 46px; padding: 0 14px; color: #9fb0c3; border-radius: 10px; text-decoration: none; transition: all .2s; font-size: 14px; }
.nav-item:hover { color: #fff; background: rgba(255,255,255,.06); }
.nav-item.router-link-active { color: #fff; background: linear-gradient(90deg,rgba(47,128,237,.28),rgba(47,128,237,.08)); box-shadow: inset 3px 0 #56ccf2; }
.nav-item .el-icon { font-size: 18px; }
.nav-badge { margin-left: auto; min-width: 20px; height: 20px; border-radius: 10px; background: #ef4444; display: grid; place-items: center; font-size: 11px; }
.sidebar-footer { padding: 16px; border-top: 1px solid rgba(255,255,255,.08); }
.user-card { display: flex; align-items: center; gap: 10px; }
.user-meta { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.user-meta strong { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-meta span { margin-top: 3px; color: #8398ae; font-size: 11px; }
.sidebar-footer :deep(.el-button) { color: #8fa4bb; }
.research-main { margin-left: 248px; min-height: 100vh; }
.research-header { height: 88px; background: rgba(255,255,255,.92); backdrop-filter: blur(12px); border-bottom: 1px solid #e9eef5; display: flex; align-items: center; justify-content: space-between; padding: 0 32px; position: sticky; top: 0; z-index: 10; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #8794a6; font-size: 12px; margin-top: 5px; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.research-content { padding: 28px 32px 48px; max-width: 1600px; margin: 0 auto; }
@media (max-width: 900px) { .research-sidebar { width: 76px; } .brand { padding: 0 18px; } .brand-name,.brand-sub,.nav-item span,.user-meta,.sidebar-footer .el-dropdown { display:none; } .nav-item { justify-content:center; padding:0; } .research-main { margin-left:76px; } .research-header,.research-content { padding-left:20px; padding-right:20px; } }
</style>
