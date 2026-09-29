# ResearchFlow V2 设计决策说明

## 背景

V1 用 `research_project` 同时承载申报、立项、执行和验收，适合快速验证，但会混淆“申请事实、批准事实、计划事实和实际执行事实”。V2 允许重建 Demo Schema，因此选择重新建立领域边界，而不是继续兼容错误抽象。

## 1. ResearchRecord 只是统一身份，不是巨型 Aggregate

ResearchRecord 让一个科研事项从申请到结项保持统一追踪 ID，但 Proposal、Award、Project、ChangeRequest、Budget、Acceptance 均有独立事务边界。

## 2. Proposal 与 Project 分离

Proposal 表达“申请做什么”；Project 表达“正式获批并正在治理什么”。

批准 Proposal 不直接修改 Proposal 的申请预算或周期，而是产生 Award 保存批准事实。

## 3. Award 保存正式批准事实

申请预算、批准预算、当前预算和实际支出必须分开。

同样，申请周期、批准周期、Baseline 周期和实际完成日期不能共用一个字段反复覆盖。

## 4. Baseline 不可变

项目启动时生成 Baseline V1。

正式项目执行后，计划变化必须进入 ChangeRequest。批准变更被实际应用后生成 Baseline V2/V3，旧 Baseline 永不修改。

## 5. WorkItem 统一 WBS

PHASE、WORK_PACKAGE、TASK、MILESTONE 使用统一树形 WorkItem 模型，避免维护三套重复 CRUD，同时保留未来树形 WBS、看板和甘特视图扩展空间。

## 6. Risk 与 Issue 分离

Risk 是未来不确定事件，Issue 是已经发生的问题。

正式风险等级由 probability × impact 的确定性规则计算。AI 可以解释和建议，但不能直接确定正式等级或状态。

## 7. Change Approval 与 Apply 分离

“同意变更”与“真正修改项目计划”是两个动作。

这样可以审计谁批准、何时应用、产生哪个 Baseline，并避免审批动作直接覆盖计划数据。

## 8. Acceptance 与 Closeout 分离

验收通过只说明科研成果通过评价；项目还要完成最终报告、经费、成果、问题和档案检查，才可进入 CLOSED。

## 9. Document 元数据与存储解耦

ResearchFlow 保存正式的 research_document 元数据。

文件存储通过 storage_provider / storage_key 抽象。Demo 使用 LOCAL + Docker Volume，不启动 MinIO；未来可以切换 MinIO、S3、OSS 等对象存储而不改变领域模型。

## 10. Workflow 是通用业务审计

Workflow 不再强绑定 Project，可关联 Proposal、ChangeRequest、Acceptance 等业务对象。

当前保持轻量 WorkflowInstance / WorkflowAction；未来出现多级审批、会签和条件分支时再替换为 Flowable。

## 11. 模块化单体优先

继续采用 Spring Boot + RuoYi + MyBatis + MySQL 的模块化单体。

当前不引入微服务、服务注册、MQ 和分布式事务，因为这些不会增加 Demo 的业务判断质量。

## 12. Application Service 管用例，Mapper 只负责持久化

Controller 保持薄层；领域动作使用明确 API，例如 submit、activate、occur、apply、closeout，而不是通用 status 更新接口。

V2 采用 CQRS Lite 思路，但不引入 CQRS/Event Sourcing 框架。

## 13. AI 只消费结构化业务上下文

AI 不直接访问 Mapper，也不承担 Workflow Engine、Permission System 或 Accounting System 的职责。

未来通过 ProjectContextAssembler 将 Award、Baseline、WorkItem、Progress、Budget、Risk、Issue、Change、Outcome 等结构化信息提供给 Copilot。

## 14. Demo 数据直接重建，不做 V1 兼容迁移

目前没有生产数据负担，因此 V2 选择重建科研业务 Schema 和 Demo Seed，而不是维护大量 ALTER/兼容代码。

升级本地 Demo 时应执行：

```bash
docker compose down -v
docker compose up -d --build
```

## 15. 前端信息密度提高

科研管理是桌面生产力场景。V2 将大面积卡片式布局调整为：

- 220px 紧凑侧栏。
- 64px Header。
- 紧凑 Toolbar。
- 表格优先。
- 单行 Project Header + KPI Strip。
- 7 个 Project Workspace Tab。

移动端继续保留最大 5 项底部导航。
