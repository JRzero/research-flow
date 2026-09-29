# 003 Risk / Issue / Decision / Change

## Permissions
- PI / PROJECT_MANAGER：可发起和应用正式 Change Request。
- 项目成员：可提交 Progress Report，登记 Risk / Issue / Decision / Outcome / Document。
- FINANCE_CONTACT：可维护 Expense。
- 系统 research_admin 拥有跨项目管理权限。

## Risk
- score = probability * impact。
- 风险等级由确定性规则计算，不由 AI 决定。
- Risk 状态：OPEN / MONITORING / OCCURRED / CLOSED。
- Risk OCCURRED 可以在一个事务中生成 Issue，并保留 source_risk_id。

## Issue
- Issue 是已经发生的问题。
- 状态：OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED。
- HIGH / CRITICAL 且未 RESOLVED/CLOSED 的 Issue 会阻断验收与结项。

## Decision Log
- 关键项目判断独立记录 title / context / decision / reason / decision maker。
- Decision 用于验收、复盘和 AI 项目总结，不代替 Change Control。

## Change
DRAFT -> SUBMITTED -> APPROVED / REJECTED -> APPLIED。

- APPROVED 与 APPLIED 分离。
- 一次 ChangeRequest 可包含多个 ChangeItem。
- 当前 Demo 支持正式修改计划结束日期和当前预算。
- APPLY 在单事务中更新当前计划，预算变化时生成新的 Budget Version，再创建 Baseline V(next)，更新 current_baseline_id，并将 Change 标记为 APPLIED。
