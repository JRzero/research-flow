# 002 Project Planning & Baseline

## Flow
PLANNING Project -> ProjectMember -> WorkItem hierarchy -> Budget -> activate -> Baseline V1 -> ACTIVE.

## WorkItem
- 使用统一 WorkItem 表达 PHASE / WORK_PACKAGE / TASK / MILESTONE。
- parent_id 表达 WBS 父子关系，父节点必须属于同一项目，MILESTONE 不能作为父节点。
- TASK / MILESTONE 必须指定项目成员作为负责人，并填写计划开始/结束日期。
- 工作项计划周期不得超出项目批准周期，结束日期不得早于开始日期。
- weight 范围为 0-100。
- 项目工作空间以树形方式呈现 WBS。
- 项目激活前至少需要一个 WorkItem。

## Permissions
- PI / PROJECT_MANAGER 可维护计划、项目成员并激活项目。
- ACTIVE 后不得直接新增/改写基线计划，计划变化必须进入 Change Control。
- WorkItem 执行状态可由 PI、PROJECT_MANAGER 或该 WorkItem owner 更新。
- 普通项目成员不能修改正式计划。

## Activation checks
- 项目团队不为空。
- 所有 WorkItem 再次通过父级、负责人和日期校验。
- 当前 Budget 存在且 > 0。
- BudgetLine 合计必须等于当前 Budget total_amount。
- Project.current_budget 必须与当前 Budget Version 一致。

## Baseline
- Baseline 由系统生成，没有 CRUD Update/Delete API。
- V1 在项目激活时生成。
- Baseline 是不可变 Snapshot，包括批准范围/目标、ExpectedOutput、WorkItems、Budget 等。
- Project.current_baseline_id 指向当前生效版本。
