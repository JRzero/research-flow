# 004 Finance / Outcome / Acceptance / Closeout

## Finance
- Budget 版本化，Expense 只表达项目预算执行，不替代会计或报销系统。
- Expense 必须关联当前 Budget Version 的 BudgetLine。
- 单笔新增后不得超过项目总预算，也不得超过对应 BudgetLine 额度。
- Budget Version 切换后，科目累计支出按 project + category 跨版本汇总，避免历史支出在新版本中显示为 0。
- Change 调整预算后生成新 Budget Version，历史 Budget 不覆盖。

## Outcome
- ExpectedOutput 来自已提交 Proposal，并在 Award 签发时作为当前 Demo 的量化成果口径冻结。
- ResearchOutcome 表达实际成果，可关联 ExpectedOutput。
- Outcome 若关联 ExpectedOutput，该 ExpectedOutput 必须属于当前项目。
- 验收前要求所有 ExpectedOutput 的 COMPLETED Outcome 数量达到 target_quantity。

## Acceptance
- 仅 ACTIVE 项目可提交。
- 以下情况阻断提交：
  - HIGH / CRITICAL Issue 未解决；
  - TASK / MILESTONE 尚未 DONE；
  - ExpectedOutput 尚有缺口。
- Acceptance 状态支持 SUBMITTED -> RETURNED -> 再次 SUBMITTED。
- 退回重提复用同一 Acceptance 业务记录和编号，但创建新的 WorkflowInstance。
- APPROVED 后 Project 进入 CLOSING。

## Closeout
只有 Acceptance 已 APPROVED 才能结项，并再次检查：
- 无 HIGH / CRITICAL 未解决 Issue；
- TASK / MILESTONE 全部完成；
- ExpectedOutput 全部满足；
- 至少一份 Project Document 用于归档；
- Expense 合计未超过当前预算；
- Closeout 仍为 PENDING；
- 必须填写结项结论。

检查通过后 Closeout -> COMPLETED，Project -> CLOSED，ResearchRecord -> CLOSED。
