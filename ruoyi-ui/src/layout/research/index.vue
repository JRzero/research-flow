<template>
  <div class="research-shell">
    <a class="skip-link" href="#research-main">跳到主内容</a>

    <aside class="research-sidebar" aria-label="ResearchFlow 主导航">
      <button class="brand" type="button" aria-label="返回科研项目工作台" @click="router.push('/research/dashboard')">
        <div class="brand-mark" aria-hidden="true">R</div>
        <div class="brand-copy">
          <div class="brand-name">ResearchFlow</div>
          <div class="brand-sub">科研项目管理</div>
        </div>
      </button>

      <nav class="product-nav" aria-label="产品导航">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :aria-label="item.label"
        >
          <el-icon aria-hidden="true"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card">
          <el-avatar :size="40" :src="userStore.avatar" aria-hidden="true">
            {{ (userStore.nickName || userStore.name || 'R').slice(0, 1) }}
          </el-avatar>
          <div class="user-meta">
            <strong>{{ userStore.nickName || userStore.name }}</strong>
            <span>{{ roleText }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-button text circle aria-label="打开用户菜单"><el-icon aria-hidden="true"><MoreFilled /></el-icon></el-button>
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

    <main id="research-main" class="research-main" tabindex="-1">
      <header class="research-header">
        <div class="header-copy">
          <div class="page-title">{{ currentTitle }}</div>
          <div class="page-subtitle">{{ currentSubtitle }}</div>
        </div>
        <div class="header-actions" aria-label="当前角色">
          <span class="role-chip" :class="roleClass">
            <span class="role-dot" aria-hidden="true"></span>
            {{ roleText }}
          </span>
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
  { path: '/research/approvals', label: '审批', icon: 'Finished' },
  { path: '/research/risks', label: '风险', icon: 'Warning' },
  { path: '/research/analytics', label: '数据', icon: 'DataAnalysis' }
]

const isAdmin = computed(() => userStore.roles.includes('admin'))
const isResearchAdmin = computed(() => isAdmin.value || userStore.roles.includes('research_admin'))
const isManager = computed(() => userStore.roles.includes('research_manager'))
const roleText = computed(() => isResearchAdmin.value ? '科研管理员' : isManager.value ? '管理者' : '项目负责人')
const roleClass = computed(() => isResearchAdmin.value ? 'admin' : isManager.value ? 'manager' : 'owner')

const titleMap = {
  '/research/dashboard': ['科研项目工作台', '优先处理待办，持续关注进展与风险'],
  '/research/projects': ['科研项目', '从申报到结项的统一项目空间'],
  '/research/approvals': ['审批中心', '集中处理项目申报、启动与成果验收'],
  '/research/risks': ['风险项目', '用可解释规则识别需要优先关注的项目'],
  '/research/analytics': ['数据概览', '掌握项目结构、进度与预算执行情况']
}
const currentTitle = computed(() => route.path.startsWith('/research/projects/') ? '项目工作空间' : (titleMap[route.path]?.[0] || 'ResearchFlow'))
const currentSubtitle = computed(() => route.path.startsWith('/research/projects/') ? '在一个空间内推进项目全生命周期工作' : (titleMap[route.path]?.[1] || ''))

function handleCommand(command) {
  if (command === 'profile') router.push('/user/profile')
  if (command === 'admin') router.push('/system/user')
  if (command === 'logout') {
    userStore.logOut().then(() => router.push('/login'))
  }
}
</script>

<style scoped lang="scss">
.research-shell {
  min-height: 100dvh;
  background: var(--rf-bg);
  color: var(--rf-text);
  font-size: 14px;
}
.skip-link {
  position: fixed;
  top: 10px;
  left: 12px;
  z-index: 1000;
  transform: translateY(-140%);
  padding: 10px 14px;
  border-radius: var(--rf-radius-sm);
  background: var(--rf-surface);
  color: var(--rf-primary);
  font-weight: 700;
  box-shadow: var(--rf-shadow-md);
  transition: transform var(--rf-motion-fast) ease;
}
.skip-link:focus { transform: translateY(0); }

.research-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 248px;
  background: var(--rf-sidebar);
  color: #fff;
  display: flex;
  flex-direction: column;
  z-index: 40;
  border-right: 1px solid rgba(148, 163, 184, .12);
}
.brand {
  width: 100%;
  height: 88px;
  border: 0;
  background: transparent;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 22px;
  text-align: left;
  cursor: pointer;
  border-bottom: 1px solid rgba(148, 163, 184, .12);
}
.brand:hover { background: rgba(255, 255, 255, .035); }
.brand-mark {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #2563eb, #38bdf8);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(37, 99, 235, .24);
}
.brand-name { font-size: 16px; font-weight: 750; letter-spacing: .1px; }
.brand-sub { margin-top: 3px; color: var(--rf-sidebar-muted); font-size: 12px; }

.product-nav {
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.nav-item {
  position: relative;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #a8b5c7;
  font-size: 14px;
  font-weight: 520;
  text-decoration: none;
  transition: background var(--rf-motion-fast) ease, color var(--rf-motion-fast) ease, transform var(--rf-motion-fast) ease;
  touch-action: manipulation;
}
.nav-item:hover { color: #fff; background: rgba(255, 255, 255, .065); }
.nav-item:active { transform: scale(.985); }
.nav-item.router-link-active {
  color: #fff;
  background: rgba(37, 99, 235, .24);
  box-shadow: inset 3px 0 #60a5fa;
}
.nav-item .el-icon { font-size: 18px; }
.nav-badge {
  margin-left: auto;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #dc2626;
  color: #fff;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.sidebar-footer { padding: 14px; border-top: 1px solid rgba(148, 163, 184, .12); }
.user-card {
  min-height: 60px;
  padding: 8px 8px 8px 10px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, .035);
}
.user-meta { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.user-meta strong { overflow: hidden; color: #f8fafc; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.user-meta span { margin-top: 4px; color: var(--rf-sidebar-muted); font-size: 12px; }
.sidebar-footer :deep(.el-button) { width: 44px; height: 44px; color: #a8b5c7; }

.research-main { min-height: 100dvh; margin-left: 248px; }
.research-header {
  height: 88px;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: color-mix(in srgb, var(--rf-surface) 92%, transparent);
  border-bottom: 1px solid var(--rf-border);
  backdrop-filter: blur(12px);
}
.page-title { color: var(--rf-text); font-size: 20px; font-weight: 720; letter-spacing: -.2px; }
.page-subtitle { margin-top: 5px; color: var(--rf-text-muted); font-size: 13px; line-height: 1.4; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.role-chip {
  min-height: 34px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--rf-border);
  border-radius: 999px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  font-size: 12px;
  font-weight: 650;
}
.role-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--rf-primary); }
.role-chip.manager .role-dot { background: var(--rf-success); }
.role-chip.owner .role-dot { background: var(--rf-warning); }

.research-content {
  width: min(100%, 1500px);
  margin: 0 auto;
  padding: 28px 32px 56px;
}

@media (max-width: 1024px) {
  .research-sidebar { width: 84px; }
  .brand { justify-content: center; padding: 0; }
  .brand-copy, .nav-item > span:not(.nav-badge), .user-meta, .sidebar-footer .el-dropdown { display: none; }
  .product-nav { padding: 16px 10px; }
  .nav-item { justify-content: center; padding: 0; }
  .nav-item.router-link-active { box-shadow: inset 3px 0 #60a5fa; }
  .sidebar-footer { padding: 12px; }
  .user-card { justify-content: center; padding: 8px; background: transparent; }
  .research-main { margin-left: 84px; }
  .research-header, .research-content { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 767px) {
  .research-shell { padding-bottom: 72px; }
  .research-sidebar {
    inset: auto 0 0 0;
    width: auto;
    height: 72px;
    border-top: 1px solid var(--rf-border);
    border-right: 0;
    background: color-mix(in srgb, var(--rf-surface) 96%, transparent);
    backdrop-filter: blur(14px);
  }
  .brand, .sidebar-footer { display: none; }
  .product-nav {
    height: 100%;
    padding: 6px max(8px, env(safe-area-inset-right)) calc(6px + env(safe-area-inset-bottom)) max(8px, env(safe-area-inset-left));
    flex-direction: row;
    gap: 2px;
  }
  .nav-item {
    min-width: 0;
    min-height: 58px;
    flex: 1;
    padding: 5px 2px;
    border-radius: 10px;
    flex-direction: column;
    justify-content: center;
    gap: 3px;
    color: var(--rf-text-muted);
    font-size: 11px;
  }
  .nav-item > span:not(.nav-badge) { display: block; }
  .nav-item .el-icon { font-size: 20px; }
  .nav-item:hover { color: var(--rf-text); background: var(--rf-surface-subtle); }
  .nav-item.router-link-active {
    color: var(--rf-primary);
    background: var(--rf-primary-soft);
    box-shadow: none;
  }
  .nav-badge { position: absolute; top: 5px; right: calc(50% - 24px); }
  .research-main { margin-left: 0; }
  .research-header {
    height: auto;
    min-height: 78px;
    padding: 14px 16px;
    align-items: flex-start;
  }
  .header-copy { min-width: 0; }
  .page-title { font-size: 18px; }
  .page-subtitle { font-size: 12px; }
  .role-chip { min-height: 30px; padding: 0 8px; font-size: 11px; white-space: nowrap; }
  .research-content { padding: 18px 16px 32px; }
}

@media (max-width: 420px) {
  .page-subtitle { display: none; }
  .research-header { min-height: 62px; align-items: center; }
  .role-chip { max-width: 112px; overflow: hidden; text-overflow: ellipsis; }
}
</style>
