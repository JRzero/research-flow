# ResearchFlow V2 产品需求说明

## 1. 产品定位

ResearchFlow 面向科研院所，以“科研事项统一记录 + 申请与正式项目分离 + 项目执行治理”为核心，提供从项目申请到结项归档的最小完整闭环。

V2 不是通用任务管理工具，也不是财务系统。它重点解决：

- 科研申报和正式立项之间的数据边界。
- 立项计划与实际执行之间的偏差。
- 风险、问题和项目变更的正式治理。
- 经费执行、计划成果和实际成果的项目级追踪。
- 验收与行政结项的可审计闭环。

## 2. 核心角色

- **科研人员 / PI**：创建 Proposal，维护申报材料，制定项目计划，更新执行信息、风险、问题、成果和验收材料。
- **科研管理员**：评审、立项、项目变更审批、验收和 Closeout。
- **管理者**：查看科研组合、预算和风险，不修改核心业务事实。
- **Admin**：RuoYi 系统管理。

权限模型：系统 RBAC + Project Membership + Business State。

## 3. 核心生命周期

```text
ResearchCall
   ↓
ResearchRecord
   ↓
Proposal
   ↓
Review
   ↓
Award
   ↓
Project / PLANNING
   ↓
Baseline V1
   ↓
Project / ACTIVE
   ↓
Acceptance
   ↓
Closeout
   ↓
CLOSED
```

Proposal 被批准后才可形成 Award；Award 产生正式 Project。项目启动前必须形成执行计划和首个 Baseline。

## 4. Proposal

Proposal 保存申请事实，不被后续批准值覆盖。

包括：

- 项目背景、目标、范围、非范围。
- 研究内容、技术路线、创新点、成功标准。
- 项目团队。
- 申请预算和预算科目。
- 计划成果。
- 申报附件。
- 完整性检查。
- Review / Workflow。

状态：

```text
DRAFT → UNDER_REVIEW → APPROVED / REJECTED
             ↑
      REVISION_REQUIRED
```

## 5. Award 与正式项目

Award 保存最终批准的项目标题、周期、预算、范围、目标和成果。

```text
Proposal requested budget
≠
Award approved budget
```

Award 创建后生成 `Project(PLANNING)`。正式项目不再承担申报草稿职责。

## 6. Project Planning / Baseline

Project Planning 使用统一 WorkItem：

- PHASE
- WORK_PACKAGE
- TASK
- MILESTONE

项目启动要求：

- 正式项目团队存在。
- 执行计划存在。
- 至少一个 Milestone。
- 正式预算存在。

启动时生成不可变 `Baseline V1`。

项目 ACTIVE 后，正式计划调整必须走 ChangeRequest。

## 7. Progress

进展使用 ProgressReport，而不是简单“改一个完成度”。

支持：

- MONTHLY
- QUARTERLY
- ANNUAL
- MIDTERM
- AD_HOC

记录完成工作、关键成果、问题、风险、下一阶段计划和总体进度。

## 8. Risk / Issue / Decision

风险由 probability × impact 计算等级：

- 1–4 LOW
- 5–9 MEDIUM
- 10–16 HIGH
- 17–25 CRITICAL

风险来源可为 MANUAL / RULE / AI_SUGGESTED；AI 建议不能直接改变正式风险状态。

Risk 是可能发生；Issue 是已经发生。风险发生时可直接转为 Issue，并保留来源关系。

Decision Log 用于记录“当时为什么这样决定”。

## 9. Change Control

ChangeRequest 是独立业务对象：

```text
DRAFT
→ SUBMITTED
→ ASSESSING
→ APPROVED / REJECTED
→ APPLIED
```

APPROVED 不等于 APPLIED。

一次 ChangeRequest 可以包含多个 ChangeItem。应用批准的变更时：

1. 修改当前执行计划。
2. 生成新的不可变 Baseline。
3. 更新 Project.currentBaselineId。
4. 保留完整 Workflow / Audit。

## 10. Finance

记录：

- Budget Version
- Budget Line
- Expense
- Budget Execution Rate

不做会计凭证、发票、付款和报销系统。

## 11. Outcomes

ExpectedOutput 和 ResearchOutcome 分离。

验收可比较：

```text
计划成果 vs 实际成果
```

成果类型包括论文、专利、软件、数据集、标准、报告、原型等。

## 12. Document

Document 是正式元数据实体，可关联 Proposal、Project、ChangeRequest、Acceptance 等业务对象。

Demo 文件存储：

- `/common/upload`
- Local uploadPath
- Docker Volume `research_flow_uploads`

Demo 不使用 MinIO。

## 13. Acceptance / Closeout

Acceptance 判断项目成果是否通过验收；Closeout 判断项目是否完成行政关闭。

验收通过之后，还要检查：

- 最终报告。
- 经费信息。
- 成果登记。
- 项目资料。
- 高严重度 Issue。
- 档案。

全部完成后 Project 才进入 CLOSED。

## 14. 页面

- 工作台
- 项目申请
- 科研项目
- 审批中心
- 风险与治理
- 数据概览
- Project Workspace：概览 / 计划 / 执行 / 治理 / 经费 / 成果 / 结项

桌面 UI 采用紧凑高密度布局：表格优先、小型指标条、低留白、短 Header；不采用大面积 Hero/Bento 卡片。

## 15. AI

AI 用于：

- Proposal 草稿整理。
- 缺失材料解释。
- 项目摘要。
- 风险解释。
- ProgressReport 草稿。
- Change Impact 辅助分析。
- Acceptance readiness 解释。

AI 不直接控制审批、权限、金额、风险正式等级和生命周期状态。

## 16. 明确不做

动态 BPMN 设计器、完整财务报销、会计、采购合同、专家库后台、复杂资源平衡、完整 EVM、自定义表单平台、微服务、RAG、多 Agent、OA/财务系统深度集成。
