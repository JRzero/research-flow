# ResearchFlow V2

ResearchFlow 是一个面向科研院所的科研项目全过程治理 Demo。V2 不再把“申请、立项、执行、验收”塞进同一张项目表，而是通过独立业务事实表达完整生命周期：

```text
ResearchRecord
  -> Proposal
  -> Review
  -> Award
  -> Project
  -> Baseline
  -> Execution / Governance
  -> Acceptance
  -> Closeout
```

## 核心设计

- **Proposal / Award / Project 分离**：申请值、批准值、正式项目状态互不覆盖；审批中心可显式调整批准周期、预算、范围、目标和成果。
- **Immutable Baseline**：项目激活生成 V1；批准变更应用后生成新版本。
- **统一 WBS WorkItem**：Phase / Work Package / Task / Milestone 共用树形模型，支持父子层级、负责人和权重。
- **Risk != Issue**：风险是尚未发生的不确定事件，问题是已经发生的事实。
- **Change Control**：批准和应用分离；应用变更后自动生成新 Baseline。
- **Expected Output vs Outcome**：计划成果与实际成果分别记录。
- **Acceptance != Closeout**：验收支持退回重提；验收通过后还需要完成确定性结项检查和归档。
- **Document 元数据模型**：Demo 使用本地文件存储，业务层不依赖 MinIO。
- **AI Copilot**：AI 负责辅助整理、总结和解释，不直接改变权限、金额、审批和核心状态。

详细范围见 [PRODUCT_REQUIREMENTS.md](./PRODUCT_REQUIREMENTS.md)，架构决策见 [DESIGN_DECISIONS.md](./DESIGN_DECISIONS.md)，实现规范见 [specs](./specs)。

## 产品导航

```text
工作台
项目申请
科研项目
审批中心
风险与问题
数据概览
```

单项目 Project Workspace 内集中提供：

```text
概览
计划
执行
经费
成果
资料
变更
流程
```

桌面端采用紧凑型工作台布局，以表格、紧凑行和结构化状态为主；移动端保留不超过 5 个主导航入口。

## 技术架构

```text
Vue 3 + Vite + Element Plus
            |
            v
Spring Boot 3 + RuoYi
            |
      ruoyi-research
            |
  ResearchFlow V2 Service
            |
          MyBatis
            |
          MySQL 8
```

仍采用 Modular Monolith，不引入微服务、MQ 或复杂 BPM 引擎。

## 本地一键运行

前置要求：Docker + Docker Compose。

> V2 是破坏性 Schema 重构。若本机运行过 V1 Demo，请删除旧数据库卷重新初始化。

```bash
docker compose down -v
docker compose up -d --build
```

访问：

- 产品端：http://localhost:8088
- 后端 API：http://localhost:18080
- Swagger：http://localhost:18080/swagger-ui.html

检查服务：

```bash
docker compose ps
```

Demo 只需要：

```text
MySQL
Redis
Spring Boot Backend
Vue/Nginx Frontend
```

**不需要 MinIO。**

项目文件仍通过 RuoYi `/common/upload` 上传，保存在后端 uploadPath，并通过 Docker Volume `research_flow_uploads` 持久化。业务表 `research_document` 只保存文件元数据和 `storage_key`。

`docker compose down` 不删除附件；`docker compose down -v` 会同时删除数据库与上传卷。

## Demo 数据

全新数据库会初始化：

- 一个待技术评审 Proposal
- 多个已立项 Project
- Proposal / Review / Award 数据
- Baseline V1
- WBS / WorkItem
- Progress Report
- Risk Register / Issue Log / Decision
- Change Request
- Budget / Expense
- Expected Output / Outcome
- Acceptance / Closeout
- Workflow Audit

## 演示账号

默认密码均为 `admin123`。

| 账号 | 角色 | 主要用途 |
| --- | --- | --- |
| `researcher` | 科研用户 | 项目申报、计划与执行、风险、变更、验收 |
| `research_admin` | 科研管理员 | 评审、立项、变更审批、验收与结项 |
| `research_manager` | 管理者 | 全局只读、项目组合与治理分析 |
| `admin` | 超级管理员 | Admin Console 与完整演示 |

## 权限模型

ResearchFlow V2 使用 **RuoYi RBAC + Project Membership**：

- `research_admin`：跨项目评审、变更审批、验收和结项。
- `research_manager`：全局只读与组合分析。
- PI / PROJECT_MANAGER：维护正式项目计划、团队、变更和验收申请。
- 项目成员：提交进展、风险、问题、Decision、成果和资料。
- FINANCE_CONTACT：可记录项目支出。
- WorkItem owner：可更新自己负责的工作项。

## 可选 AI

核心业务不依赖模型。需要启用 AI 申报助手时：

```env
DEEPSEEK_API_KEY=your-api-key
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_ENDPOINT=https://api.deepseek.com
```

关闭或未配置 AI 时，Proposal、Review、Award、Project、Change、Acceptance 等业务均可正常运行。

## 本地开发

后端要求 Java 17+、Maven 3.9+：

```bash
mvn -B -ntp test
mvn -DskipTests package
```

前端：

```bash
cd ruoyi-ui
npm install
npm run dev
```

默认本地基础设施：

```env
DB_HOST=localhost
DB_PORT=13306
DB_NAME=research_flow
DB_USERNAME=root
DB_PASSWORD=password
REDIS_HOST=localhost
REDIS_PORT=16379
```

## V2 核心目录

```text
ruoyi-research/src/main/java/com/ruoyi/research/
  ai/                         # 可选 AI Copilot
  v2/
    ResearchFlowRules.java    # 显式确定性规则
    ResearchFlowService.java
    ResearchFlowServiceImpl.java
    ResearchFlowMapper.java

ruoyi-research/src/main/resources/mapper/research/
  ResearchFlowMapper.xml

ruoyi-ui/src/views/research/
  dashboard/
  proposals/
  projects/
  projectDetail/
  approvals/
  risks/
  analytics/

specs/
  000-v2-architecture/
  001-proposal-award/
  002-project-planning/
  003-governance/
  004-closeout/
  005-ui-density/

sql/z_research_flow.sql       # V2 Schema + Demo Seed
```

## CI

PR 和 main push 会校验：

- Maven 后端测试
- Vue production build
- Docker Compose 配置
- MySQL 8 全新 Schema 初始化

ResearchFlow V2 当前明确不做：动态 BPMN Designer、完整会计/报销、采购合同、专家库、微服务、消息队列、RAG、多 Agent，以及 MinIO 强依赖。
