# ResearchFlow

ResearchFlow 是一个面向科研院所的科研项目全过程治理 Demo。V2 不再用一张 Project 表同时承载申报、立项和执行，而是采用：

```text
ResearchRecord
  → Proposal
  → Review
  → Award
  → Project (PLANNING)
  → Baseline
  → Project (ACTIVE)
  → Acceptance
  → Closeout
```

项目执行阶段进一步覆盖 WBS / WorkItem、进展报告、风险、问题、变更、经费与科研成果。

## V2 核心设计

- **ResearchRecord**：统一科研事项身份，不承担巨型聚合职责。
- **Proposal ≠ Project**：申请事实与正式项目分离。
- **Award**：保存批准后的周期、预算、范围和成果，不覆盖原始 Proposal。
- **Baseline**：项目启动和批准变更时创建的不可变计划快照。
- **WorkItem**：统一建模 PHASE / WORK_PACKAGE / TASK / MILESTONE。
- **Risk / Issue**：风险是可能发生，Issue 是已经发生；风险可正式转 Issue。
- **Change Control**：APPROVED 与 APPLIED 分开，应用变更后生成新 Baseline。
- **Finance**：记录预算版本、预算科目和执行支出，不承担会计核算。
- **ExpectedOutput / Outcome**：计划成果与实际成果分开。
- **Acceptance / Closeout**：验收通过不等于项目行政关闭。
- **Document**：附件元数据正式入库，文件本体使用可替换存储接口；Demo 使用本地 Docker Volume。
- **Workflow**：统一记录 Proposal、Change、Acceptance 等业务审批轨迹。

## 产品界面

ResearchFlow V2 使用紧凑型科研管理界面，而不是大面积卡片式 Dashboard：

- 工作台：指标条、最近项目、待办、高风险。
- 项目申请：独立 Proposal 列表，支持团队、预算、成果、附件和完整性检查。
- 科研项目：只展示已正式立项项目。
- Project Workspace：概览 / 计划 / 执行 / 治理 / 经费 / 成果 / 结项。
- 审批中心：统一处理申报、变更和验收。
- 风险与治理：跨项目风险登记册。
- 数据概览：科研组合和预算执行。

桌面端以表格、分栏和紧凑工具栏为主；移动端保留 5 项底部导航。

## 技术架构

```text
Vue 3 + Vite + Element Plus
             │
             ▼
Spring Boot 3 + RuoYi
             │
        ruoyi-research
             │
  ┌──────────┼─────────────┐
Proposal   Project      Governance
Review     Baseline     Risk / Issue
Award      WorkItem     Change
           Progress     Finance / Outcome
             │
             ▼
           MySQL 8
```

- 模块化单体，不引入微服务、注册中心或 MQ。
- MyBatis 持久化。
- RBAC + Project Membership 数据权限。
- Command/Application Service 风格处理写操作。
- 查询返回专用 Query Model。
- AI 只读取结构化业务上下文，不直接修改审批、权限、金额和生命周期状态。

## Demo 不使用 MinIO

本地 Demo 仅包含 4 类服务：

```text
MySQL
Redis
Spring Boot Backend
Vue/Nginx Frontend
```

附件通过 RuoYi `/common/upload` 上传，文件保存在后端 `uploadPath`，Docker 使用持久化卷：

```text
research_flow_uploads
```

`research_document` 只保存业务关联、文件名、storage key、版本等元数据。

Demo **不需要启动 MinIO**。未来需要对象存储时，可替换 Document Storage 实现为 MinIO / S3 / OSS，而不改变科研业务模型。

## 本地运行

前置要求：Docker + Docker Compose。

V2 对科研业务 Schema 做了完整重建。如果本地曾运行过 V1 Demo，请重置 Demo 数据卷：

```bash
docker compose down -v
docker compose up -d --build
```

> `docker compose down -v` 会删除本地 MySQL、Redis 和上传文件卷。Demo 环境建议直接重建；如有需要保留的本地数据或附件，请先备份。

首次启动自动初始化：

- RuoYi 基础表
- Quartz 表
- ResearchFlow V2 业务表
- 科研角色和演示账号
- Proposal / Review / Award / Project / Baseline 示例
- WorkItem / Progress / Risk / Issue / Change / Finance / Outcome 示例数据

访问：

- 产品端：http://localhost:8088
- 后端 API：http://localhost:18080
- Swagger：http://localhost:18080/swagger-ui.html

## 演示账号

默认密码均为 `admin123`。

| 账号 | 角色 | 主要用途 |
| --- | --- | --- |
| `researcher` | 科研人员 / PI | 项目申请、计划和执行 |
| `research_admin` | 科研管理员 | 评审、立项、变更、验收和全局管理 |
| `research_manager` | 管理者 | 科研组合只读视角 |
| `admin` | 超级管理员 | 系统管理 |

## AI 配置

核心业务不依赖大模型。可选启用 DeepSeek 申报助手：

```env
DEEPSEEK_API_KEY=your-api-key
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_ENDPOINT=https://api.deepseek.com
```

AI 原则：

```text
AI → 整理、总结、解释、建议
Rules → 完整性、风险等级、验收就绪检查
Workflow → 审批和生命周期
RBAC → 权限
Database → 业务事实
```

## 核心目录

```text
ruoyi-research/
  src/main/java/com/ruoyi/research/
    ai/                              # AI Copilot
    v2/
      application/                   # V2 Application Service
      mapper/                        # V2 MyBatis Mapper

ruoyi-admin/src/main/java/com/ruoyi/web/controller/research/
                                     # V2 REST API

ruoyi-ui/src/views/research/
  dashboard/
  proposals/
  projects/
  projectDetail/
  approvals/
  risks/
  analytics/

specs/
  v2-domain-model/
  v2-lifecycle-api/

sql/z_research_flow.sql              # V2 Schema + Demo Seed
PRODUCT_REQUIREMENTS.md
DESIGN_DECISIONS.md
```

## 当前明确不做

动态 BPMN 设计器、财务报销/会计凭证、采购合同、专家库后台、复杂资源平衡、完整 EVM、自定义表单平台、微服务、RAG、多 Agent、OA/财务系统深度集成。

ResearchFlow V2 的重点是：**把科研项目治理的业务模型、完整生命周期和可审计变更机制先做正确。**
