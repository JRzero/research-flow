# ResearchFlow V2 验收报告

验收日期：2026-09-30
验收对象：ResearchFlow V2 + Acceptance Hardening
目标：验证科研项目从申报到结项的最小业务闭环、核心数据一致性、权限边界和 Demo 可运行性。

## 1. 验收结论

**Demo 级验收结论：PASS。**

主链：Proposal -> Review -> Award -> Project / Planning -> Baseline V1 -> Execution / Governance -> Change / Baseline V2+ -> Outcome -> Acceptance -> Closeout。

本轮验收发现并修复了预算版本、WBS 数据完整性、Change 过期覆盖、Risk / Issue 终态修改等问题。

## 2. 自动化验收

CI 必须同时通过：
- Maven backend tests
- Vue production build
- Docker Compose config
- MySQL 8 全新 Schema 初始化
- V2 Demo Seed 初始化
- V2 Domain Invariants

Domain Invariants 会检查：
1. PLANNING / ACTIVE / CLOSING 项目的 current_budget 与 ACTIVE Budget Version 一致。
2. 当前 BudgetLine 合计等于 Budget total_amount。
3. WorkItem parent 不允许跨项目。
4. TASK / MILESTONE 必须存在负责人，且负责人属于项目。
5. current_baseline_id 必须指向当前项目自己的 Baseline。
6. ACTIVE / CLOSING 项目实际支出不能超过当前预算。
7. CLOSED 项目必须同时具有 APPROVED Acceptance 和 COMPLETED Closeout。
8. UNDER_REVIEW Proposal 必须存在 RUNNING WorkflowInstance。

## 3. Proposal / Award 验收

### PASS
- Proposal 与 Award / Project 分离。
- 申请值不会因批准结果被覆盖。
- Proposal 提交前检查研究目标、研究内容、日期、成员、预算明细和 ExpectedOutput。
- BudgetLine 合计必须等于 requestedBudget。
- ExpectedOutput target_quantity 必须至少为 1。
- Proposal 提交后进入 UNDER_REVIEW 并创建 Workflow。
- Award 可调整名称、周期、预算、范围和目标。
- 批准结束日期不得早于开始日期。
- Award 签发后创建 PLANNING Project。
- Proposal Team 复制到正式 Project Team。
- 结构化 ExpectedOutput 在 Award 签发时冻结为当前 Demo 的量化验收口径。

## 4. Project Planning / Baseline 验收

### PASS
- PI / PROJECT_MANAGER 可维护正式团队和 WBS。
- WorkItem 支持 PHASE / WORK_PACKAGE / TASK / MILESTONE。
- parent_id 构造树形 WBS，父节点必须属于同一项目，MILESTONE 不能作为父节点。
- TASK / MILESTONE 必须指定项目成员负责人并填写计划周期。
- 工作项周期不得超出项目批准周期，weight 范围为 0-100。
- 项目激活前重新验证团队、WBS、Budget 和 BudgetLine。
- 激活生成不可变 Baseline V1。
- Project.current_baseline_id 指向当前有效 Baseline。

## 5. Execution / Governance 验收

### PASS
- 项目成员可提交 Progress Report。
- Risk 等级使用确定性 probability × impact 计算。
- Risk 与 Issue 独立。
- OPEN / MONITORING Risk 可转换为 Issue。
- OCCURRED Risk 不允许重新打开，CLOSED Risk 为终态。
- Issue 主状态链为 OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED。
- Issue RESOLVED 必须填写 resolution。
- CLOSED / TERMINATED Project 不允许继续修改 Risk / Issue。
- Decision Log 可记录关键判断。
- HIGH / CRITICAL 未关闭 Issue 会阻断验收与结项。

## 6. Change Control 验收

### PASS
- 状态：DRAFT -> SUBMITTED -> APPROVED / REJECTED -> APPLIED。
- APPROVED 与 APPLIED 分离。
- 当前 Demo 支持 planned_end_date 和 current_budget / total_budget。
- Submit 和 Apply 都会检查 before_value，防止过期 Change 覆盖新计划。
- 新结束日期不得早于项目开始日期。
- 新预算必须大于 0，且不能低于已发生实际支出。
- Budget 变化创建新 Budget Version。
- Change Apply 创建 Baseline V(next)，历史 Baseline 不覆盖。

## 7. Finance 验收

### PASS
- Budget Version 与 BudgetLine 分离。
- Expense 必须关联当前 Budget Version 的 BudgetLine。
- 单笔 Expense 不得超过项目总预算或预算科目额度。
- Budget Version 切换后，科目累计支出按 project + category 跨版本汇总。
- 前端累计已使用不会因 BudgetLine ID 变化而归零。
- ResearchFlow 不承担会计、付款、发票和报销。

## 8. Outcome / Acceptance / Closeout 验收

### PASS
- ExpectedOutput 表达量化计划成果，ResearchOutcome 表达实际成果。
- Outcome 若关联 ExpectedOutput，该 ExpectedOutput 必须属于当前项目。
- 验收提交前检查高严重度 Issue、未完成 TASK / MILESTONE 和成果缺口。
- Acceptance 支持 SUBMITTED -> RETURNED -> 再次 SUBMITTED -> APPROVED。
- 重提复用 Acceptance 记录和编号，并创建新的 Workflow。
- Acceptance APPROVED 后 Project 进入 CLOSING。
- Closeout 与 Acceptance 分离，并再次检查任务、成果、Issue、预算和归档资料。
- Closeout 完成后 Closeout -> COMPLETED，Project -> CLOSED，ResearchRecord -> CLOSED。

## 9. 权限验收

### PASS
- research_admin：跨项目审批与管理。
- research_manager：全局只读。
- research_owner：科研业务用户。
- admin：RuoYi 超级管理员。
- PI / PROJECT_MANAGER：计划、团队、Change、验收申请。
- 项目成员：Progress / Risk / Issue / Decision / Outcome / Document。
- FINANCE_CONTACT：Expense。
- WorkItem owner：自己的 WorkItem 执行状态。
- 审批中心仅对 research_admin / admin 开放。

## 10. UI 验收

### PASS（Build / Static）
- Sidebar 约 220px。
- Header / Brand 56px。
- Workspace Tab 40px。
- 页面常用 gap 7-10px，Card padding 约 8-12px。
- WBS 使用树形表格。
- 执行页集中 Progress / Risk / Issue / Decision。
- Award 批复采用紧凑弹窗。
- 表格和 compact rows 优先于大 Hero / 大留白。
- 移动端保留不超过 5 个主导航入口。

## 11. 文件与部署验收

### PASS（Demo）
- Demo 只需要 MySQL、Redis、Spring Boot Backend、Vue / Nginx Frontend。
- 不需要 MinIO。
- 单文件限制 10MB，总请求限制 20MB。
- 文件存入 RuoYi profile 对应 Docker Volume，research_document 保存元数据。

## 12. 已知非阻断项

以下不阻断当前面试 Demo，但生产版本前应处理：
1. 当前没有 Playwright / 浏览器级 E2E；CI 目前覆盖构建、Schema、Seed、规则和数据库不变量。
2. RuoYi 默认 /profile/** 为匿名静态资源。研究附件页面受 RBAC / Project Membership 控制，但底层静态文件 URL 若泄露，仍属于知道链接即可访问。生产版应改为私有对象存储或受鉴权的 Document Download API。
3. Simple Workflow 不包含复杂 BPMN Designer、会签、加签、委托等能力。
4. Risk / Issue 编号采用项目当前数量生成，Demo 足够；生产并发环境应改为数据库序列或独立编号服务。
5. 文件先物理上传、后绑定业务对象，取消表单可能留下 orphan file；生产版需要临时上传 token / 清理任务。
6. Award 阶段当前冻结 Proposal 的结构化 ExpectedOutput，不支持逐项调整成果数量；如业务要求，应增加 AwardOutput / ProjectExpectedOutput 模型。

## 13. 本地人工验收建议

账号均使用 admin123：researcher、research_admin、research_manager、admin。

建议主链：
researcher 创建 Proposal -> 团队 / 预算 / ExpectedOutput / 附件 -> Validation -> Submit；
research_admin 在审批中心进行 Award 批复 -> 生成 Project；
researcher 建立团队和 WBS -> Activate -> Baseline V1 -> Progress / Risk / Issue / Decision / Expense / Outcome / Change；
research_admin Approve Change；
researcher Apply Change -> Baseline V2 -> 完成 Task / Milestone -> 补齐 Outcome -> Submit Acceptance；
research_admin Return Acceptance；
researcher Resubmit Acceptance；
research_admin Approve Acceptance -> Complete Closeout。

## 14. 最终判定

当前 V2 满足面试 Demo / 产品原型级验收。

生产化前重点补充：附件私有访问、浏览器 E2E、并发编号、文件 orphan 清理和更完整的 Award 成果变更模型。