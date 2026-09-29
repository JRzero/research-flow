# 002 Project Planning & Baseline

## Flow
PLANNING Project -> ProjectMember -> WorkItem hierarchy -> Budget -> activate -> Baseline V1 -> ACTIVE.

## WorkItem
- 使用统一 WorkItem 表达 PHASE / WORK_PACKAGE / TASK / MILESTONE。
- parent_id 表达 WBS 父子关系。
- owner_user_id 必须从项目团队中选择。
- 项目工作空间以树形方式呈现 WBS。
- 项目激活前至少需要一个 WorkItem。

## Permissions
- PI / PROJECT_MANAGER 可维护计划、项目成员并激活项目。
- ACTIVE 后不得直接新增/改写基线计划，计划变化必须进入 Change Control。
- WorkItem 执行状态可由 PI、PROJECT_MANAGER 或该 WorkItem owner 更新。
- 普通项目成员不能修改正式计划。

## Baseline
- Baseline 由系统生成，没有 CRUD Update/Delete API。
- V1 在项目激活时生成。
- Baseline 是不可变 Snapshot，包括批准范围/目标、WorkItems、Budget、ExpectedOutputs 等。
- Project.current_baseline_id 指向当前生效版本。
