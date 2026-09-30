# 003 Risk / Issue / Decision / Change

## Permissions
- PI / PROJECT_MANAGER：可发起和应用正式 Change Request。
- 项目成员：可提交 Progress Report，登记 Risk / Issue / Decision / Outcome / Document。
- FINANCE_CONTACT：可维护 Expense。
- 系统 research_admin 拥有跨项目管理权限。
- CLOSED / TERMINATED 项目不允许继续修改 Risk / Issue 等治理记录。

## Risk
- score = probability * impact。
- 风险等级由确定性规则计算，不由 AI 决定。
- Risk 状态：OPEN / MONITORING / OCCURRED / CLOSED。
- OPEN / MONITORING 风险可转换为 Issue；OCCURRED 之后只允许关闭，CLOSED 不允许重新打开。

## Issue
- Issue 是已经发生的问题。
- 主状态链：OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED。
- RESOLVED 可重新进入 IN_PROGRESS；CLOSED 为终态。
- 进入 RESOLVED 时必须填写 resolution。
- HIGH / CRITICAL 且未 RESOLVED/CLOSED 的 Issue 会阻断验收与结项。

## Decision Log
- 关键项目判断独立记录 title / context / decision / reason / decision maker。
- Decision 用于验收、复盘和 AI 项目总结，不代替 Change Control。

## Change
DRAFT -> SUBMITTED -> APPROVED / REJECTED -> APPLIED。

- APPROVED 与 APPLIED 分离。
- 一次 ChangeRequest 可包含多个 ChangeItem。
- 当前 Demo 正式支持 planned_end_date 和 current_budget / total_budget。
- 提交和应用时都会校验 before_value 与项目当前值一致，防止过期 Change 覆盖新计划。
- 新结束日期不得早于项目开始日期。
- 新预算必须 > 0，且不得低于已经发生的实际支出。
- APPLY 在单事务中更新当前计划，预算变化时生成新的 Budget Version，再创建 Baseline V(next)，更新 current_baseline_id，并将 Change 标记为 APPLIED。
