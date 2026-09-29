import { createWebHistory, createRouter } from 'vue-router'
import Layout from '@/layout'
import ResearchLayout from '@/layout/research'

export const constantRoutes = [
  { path:'/redirect',component:Layout,hidden:true,children:[{path:'/redirect/:path(.*)',component:()=>import('@/views/redirect/index.vue')}] },
  { path:'/login',component:()=>import('@/views/login'),hidden:true },
  { path:'/register',component:()=>import('@/views/register'),hidden:true },
  { path:'/:pathMatch(.*)*',component:()=>import('@/views/error/404'),hidden:true },
  { path:'/401',component:()=>import('@/views/error/401'),hidden:true },
  { path:'/',redirect:'/research/dashboard',hidden:true },
  {
    path:'/research',component:ResearchLayout,redirect:'/research/dashboard',hidden:true,
    children:[
      {path:'dashboard',component:()=>import('@/views/research/dashboard/index.vue'),name:'ResearchDashboard',meta:{title:'科研工作台'}},
      {path:'proposals',component:()=>import('@/views/research/proposals/index.vue'),name:'ResearchProposals',meta:{title:'项目申请'}},
      {path:'projects',component:()=>import('@/views/research/projects/index.vue'),name:'ResearchProjects',meta:{title:'科研项目'}},
      {path:'projects/:projectId',component:()=>import('@/views/research/projectDetail/index.vue'),name:'ResearchProjectDetail',meta:{title:'项目工作空间'}},
      {path:'approvals',component:()=>import('@/views/research/approvals/index.vue'),name:'ResearchApprovals',meta:{title:'审批中心'}},
      {path:'risks',component:()=>import('@/views/research/risks/index.vue'),name:'ResearchRisks',meta:{title:'风险与问题'}},
      {path:'analytics',component:()=>import('@/views/research/analytics/index.vue'),name:'ResearchAnalytics',meta:{title:'数据概览'}}
    ]
  },
  { path:'/index',redirect:'/research/dashboard',hidden:true },
  { path:'/lock',component:()=>import('@/views/lock'),hidden:true,meta:{title:'锁定屏幕'} },
  { path:'/user',component:Layout,hidden:true,redirect:'noredirect',children:[{path:'profile/:activeTab?',component:()=>import('@/views/system/user/profile/index'),name:'Profile',meta:{title:'个人中心',icon:'user'}}]}
]

export const dynamicRoutes = [
  {path:'/system/user-auth',component:Layout,hidden:true,permissions:['system:user:edit'],children:[{path:'role/:userId(\\d+)',component:()=>import('@/views/system/user/authRole'),name:'AuthRole',meta:{title:'分配角色',activeMenu:'/system/user'}}]},
  {path:'/system/role-auth',component:Layout,hidden:true,permissions:['system:role:edit'],children:[{path:'user/:userId(\\d+)',component:()=>import('@/views/system/role/authUser'),name:'AuthUser',meta:{title:'分配用户',activeMenu:'/system/role'}}]},
  {path:'/system/dict-data',component:Layout,hidden:true,permissions:['system:dict:list'],children:[{path:'index/:dictId(\\d+)',component:()=>import('@/views/system/dict/data'),name:'Data',meta:{title:'字典数据',activeMenu:'/system/dict'}}]},
  {path:'/monitor/job-log',component:Layout,hidden:true,permissions:['monitor:job:list'],children:[{path:'index/:jobId(\\d+)',component:()=>import('@/views/monitor/job/log'),name:'JobLog',meta:{title:'调度日志',activeMenu:'/monitor/job'}}]},
  {path:'/tool/gen-edit',component:Layout,hidden:true,permissions:['tool:gen:edit'],children:[{path:'index/:tableId(\\d+)',component:()=>import('@/views/tool/gen/editTable'),name:'GenEdit',meta:{title:'修改生成配置',activeMenu:'/tool/gen'}}]}
]
const router=createRouter({history:createWebHistory(),routes:constantRoutes,scrollBehavior(to,from,saved){return saved||{top:0}}})
export default router
