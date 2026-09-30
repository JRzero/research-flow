# 001 Proposal -> Review -> Award

## Flow
Create Proposal -> edit team / budget / expected outputs / documents -> validate -> submit -> review -> issue Award -> create PLANNING Project.

## Domain boundary
- Proposal 表达“申请事实”。
- Award 表达“批准事实”，不得覆盖 Proposal 原始申请值。
- Demo 中“批准 Proposal”在一个事务里签发 Award 并建立 PLANNING Project，但 Award 仍是独立持久化对象。
- 一个 Proposal 最多一个 Award，一个 Award 最多一个 Project。

## Submission rules
- 仅 DRAFT / REVISION_REQUIRED 可编辑和提交。
- 必须包含 title、objectives、researchContent、计划开始/结束日期。
- 计划结束日期不得早于计划开始日期。
- 至少一名 ProposalMember，创建人默认作为 PI。
- requestedBudget > 0。
- 至少一个预算明细，且预算明细合计必须等于 requestedBudget。
- 至少一项 ExpectedOutput，target_quantity >= 1。
- 申报附件当前为 warning，不作为阻断项。

## Award rules
- approvedBudget > 0，批准周期不能为空，且结束日期不得早于开始日期。
- approvedTitle / scope / objectives / dates / budget 可以与申请值不同。
- 当前 Demo 将结构化 ExpectedOutput 作为量化验收口径，在 Award 签发时冻结；Award.approved_outputs 保存该清单摘要，不另维护一套可编辑的成果口径。
- 批复预算与申报预算存在差额时，在正式项目 Budget V1 中增加“批复调整”行，使 BudgetLine 合计与 Award approvedBudget 一致。
- Proposal 详情保留申请值，可用于申请值 vs 批准值审计。
