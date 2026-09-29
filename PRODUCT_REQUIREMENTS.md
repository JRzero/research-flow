# ResearchFlow V2 产品需求说明

## 1. 产品定位
ResearchFlow V2 是面向科研院所的科研项目全过程治理平台。系统以 ResearchRecord 统一标识科研事项，以 Proposal、Award、Project、Baseline、Execution、Governance、Acceptance 分离业务事实，覆盖申报、评审、立项、计划、执行、风险、变更、经费、成果、验收和结项。

## 2. 核心原则
- Proposal 与 Project 分离：申请事实不可被批准结果覆盖。
- Award 记录正式批复：批准预算、周期和范围独立保存。
- Baseline 不可变：项目激活和批准变更生成新版本，不覆盖历史计划。
- 执行事实与计划事实分开：WorkItem/Expense/Outcome 表达实际执行。
- Risk 与 Issue 分开：Risk 是可能发生，Issue 是已经发生。
- Acceptance 与 Closeout 分开：验收通过不等于行政归档完成。
- AI 只做整理、总结、建议与解释，不直接控制状态、权限、金额和审批。
- 文件采用 Document 元数据模型；Demo 使用本地 Volume，不依赖 MinIO。

## 3. 用户角色
- 科研用户：创建 Proposal，维护项目计划与执行数据，提交变更和验收。
- 科研管理员：评审、立项、变更审批、验收与结项。
- 管理者：查看全局项目组合、风险、预算和成果，不直接修改核心业务。
- 项目级角色：PI、PROJECT_MANAGER、RESEARCHER、FINANCE_CONTACT、SPONSOR、MEMBER。

## 4. 生命周期
Proposal: DRAFT -> UNDER_REVIEW -> APPROVED / REJECTED
Award: ISSUED
Project: PLANNING -> ACTIVE -> CLOSING -> CLOSED
Risk: OPEN -> MONITORING -> CLOSED / OCCURRED
Issue: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
Change: DRAFT -> SUBMITTED -> APPROVED -> APPLIED
Acceptance: DRAFT -> SUBMITTED -> APPROVED / RETURNED
Closeout: PENDING -> COMPLETED

## 5. 产品导航
- 工作台
- 项目申请
- 科研项目
- 审批中心
- 风险与问题
- 数据概览

Project Workspace 内部：
- 概览
- 计划
- 执行
- 经费
- 成果
- 资料
- 变更
- 流程

## 6. Demo 范围
必须跑通：
Proposal -> Review -> Award -> Project -> Baseline -> WorkItem/Progress -> Risk/Issue -> Change -> Expense/Outcome -> Acceptance -> Closeout。

明确不做：
微服务、MQ、复杂 BPMN Designer、完整会计/报销、专家库、采购合同、RAG、多 Agent、MinIO 强依赖。

## 7. 前端密度
桌面端以高信息密度工作台为主：
- 页面间距 12-16px。
- 卡片内边距 12-16px。
- Header 64px 左右。
- 列表优先于大面积装饰卡片。
- 关键点击区域仍保持 >= 40px。
- 移动端只保留 5 个底部主导航入口。
