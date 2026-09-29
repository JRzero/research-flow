# ResearchFlow

ResearchFlow 是一个面向科研院所的科研项目全生命周期管理 MVP，覆盖项目申报、立项审批、执行跟踪、经费管理、成果登记和验收结项。

这个 MVP 不试图一次性固化完整科研管理制度，而是先提供一个可运行的业务闭环，并作为下一轮需求澄清的载体。

## 核心体验

- **科研项目工作台**：项目总览、待办、风险和预算执行。
- **项目集合**：以项目卡片而不是 CRUD 表格浏览科研项目，申报支持多附件上传。
- **Project Workspace**：在一个项目空间内查看概览、里程碑、进展、经费、成果、审批和验收。
- **审批中心**：处理项目申报、项目启动与成果验收。
- **风险项目**：基于时间进度、任务进度和预算执行的可解释规则识别。
- **AI 申报助手**：可选 DeepSeek API，将模糊项目想法整理为申报草稿；AI 不控制审批、权限、金额或项目状态。
- **Admin Console**：保留 RuoYi 的用户、角色、部门、日志等基础管理能力，但不作为科研用户的主要产品界面。

## 技术架构

```text
Vue 3 + Vite + Element Plus
          │
          ▼
Spring Boot 3 + RuoYi
          │
   ┌──────┴─────────┐
   │                │
RuoYi 基础设施   ruoyi-research
用户/角色/部门   Project/Workflow
认证/权限/日志   Progress/Budget
                 Deliverable/Acceptance/Risk/AI
   │                │
   └──────┬─────────┘
          ▼
        MySQL 8
```

审批流程当前使用 `SimpleWorkflowService` + 显式状态机：

```text
DRAFT
  ↓
PENDING_APPROVAL
  ↓
APPROVED
  ↓
IN_PROGRESS
  ↓
PENDING_ACCEPTANCE
  ↓
COMPLETED
```

未来需要多级审批、会签、条件分支时，可以在 `WorkflowService` 后替换为 Flowable 等 BPM 引擎，而无需让 Project 领域直接依赖流程引擎。

## 本地一键运行

前置要求：Docker + Docker Compose。

```bash
docker compose up -d --build
```

首次启动会自动初始化：

- RuoYi 基础表
- Quartz 表
- ResearchFlow 业务表
- 科研角色和演示账号
- 6 个不同生命周期的演示项目
- 里程碑、进展、经费、成果、审批和验收演示数据
- 项目申报附件字段及本地上传目录

访问：

- 产品端：http://localhost:8088
- 后端 API：http://localhost:18080
- Swagger：http://localhost:18080/swagger-ui.html

项目申报附件默认保存在 Docker 卷 `research_flow_uploads` 中；项目表保存附件名称和访问路径。现有数据库升级时，`sql/zz_20260929_project_application_attachments.sql` 会补充附件字段。

如果之前启动过旧数据库卷，初始化 SQL 不会再次执行。需要全新演示库时：

```bash
docker compose down -v
docker compose up -d --build
```

## 演示账号

默认密码均为：`admin123`

| 账号 | 角色 | 主要用途 |
| --- | --- | --- |
| `researcher` | 项目负责人 | 申报、执行、经费、成果、验收申请 |
| `research_admin` | 科研管理员 | 审批、启动、验收、全局项目管理 |
| `research_manager` | 管理者 | 组合视角查看项目、经费和风险 |
| `admin` | 超级管理员 | 系统管理与完整演示 |

登录页默认填充 `research_admin / admin123`，便于快速查看完整业务闭环。

## 可选 AI 配置

核心业务不依赖大模型。需要启用 AI 申报助手时，在项目根目录创建 `.env`：

```env
DEEPSEEK_API_KEY=your-api-key
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_ENDPOINT=https://api.deepseek.com
```

未配置 API Key 时，AI 接口会明确返回“未配置”，项目的申报、审批、执行和验收仍可正常使用。

## 本地开发

### 后端

要求 Java 17+、Maven 3.9+。

```bash
mvn -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

本机数据库默认配置：

```env
DB_HOST=localhost
DB_PORT=13306
DB_NAME=research_flow
DB_USERNAME=root
DB_PASSWORD=password
REDIS_HOST=localhost
REDIS_PORT=16379
```

### 前端

要求 Node.js 20+。当前仓库未提交 package-lock.json，因此本地开发使用 `npm install`。

```bash
cd ruoyi-ui
npm install
npm run dev
```

默认开发代理指向 `http://localhost:18080`。

## 核心目录

```text
ruoyi-research/                     # 科研业务领域模块
  domain/
  mapper/
  service/
  workflow/                         # WorkflowService 扩展点
  ai/                               # AI Copilot

ruoyi-admin/src/main/java/com/ruoyi/web/controller/research/
                                     # 科研业务 REST API

ruoyi-ui/src/views/research/
  dashboard/                         # 工作台
  projects/                          # 项目集合与申报
  projectDetail/                     # Project Workspace
  approvals/                         # 审批中心
  risks/                             # 风险项目
  analytics/                         # 数据概览

sql/z_research_flow.sql              # 业务表和演示数据
sql/zz_20260929_project_application_attachments.sql # 现有数据库附件字段迁移
PRODUCT_REQUIREMENTS.md              # MVP 需求说明
DESIGN_DECISIONS.md                  # 设计决策说明
```

## 设计边界

MVP 当前明确不做：动态 BPMN 设计器、完整财务报销、采购合同、专家库、自定义表单平台、微服务、RAG、多 Agent、OA/财务系统集成。

相关判断见 [DESIGN_DECISIONS.md](./DESIGN_DECISIONS.md)，产品范围见 [PRODUCT_REQUIREMENTS.md](./PRODUCT_REQUIREMENTS.md)。

## 基础框架

项目使用 RuoYi 作为企业应用基础设施，保留原项目许可证。科研产品界面和 `ruoyi-research` 业务模块为本项目新增实现。
