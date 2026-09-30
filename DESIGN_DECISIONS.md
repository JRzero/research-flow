# ResearchFlow V2 设计决策

> 状态：已冻结为当前 Demo 架构基线  
> 更新日期：2026-09-30  
> 适用范围：ResearchFlow V2 服务端、数据库、前端工作空间与 Demo 部署

本文档记录 ResearchFlow 当前已经落地的关键产品与技术决策。除非出现明确的新业务约束，否则后续开发应优先遵循这些决策，而不是重新引入 V1 模型或绕过既有领域边界。

## 1. V2 重新建模，不兼容 V1 Schema

当前仍处于 Demo / 产品原型阶段，没有生产数据迁移负担，因此 V2 直接重新设计 Schema、Seed 和业务对象，不为 V1 表结构保留兼容层。

V1 只作为历史参考，不再约束新的领域模型。

## 2. ResearchRecord 是统一科研事项身份，不是超级聚合

ResearchRecord 用于把一个科研事项从申报、评审、立项、执行、验收到结项串联起来，并提供统一编号、当前阶段和跨生命周期导航。

Proposal、Award、Project、ChangeRequest、Budget、Acceptance 等仍保持独立事务边界，不允许把所有数据重新塞回一个超大 Project Aggregate。

## 3. Proposal / Award / Project 必须分离

三者表达不同事实：

- Proposal：申请人申请做什么。
- Award：组织最终批准做什么。
- Project：正式项目当前如何运行。

申请值与批准值不得互相覆盖。例如申请预算、批准预算、当前预算、实际支出必须分别保存，保证全过程可追溯。

## 4. ExpectedOutput 是当前 Demo 的统一量化验收口径

Proposal 使用结构化 ExpectedOutput 表达论文、专利、软件、报告等计划成果及数量。

当前 Demo 在 Award 签发时冻结这份结构化清单，Award 只保存摘要，不再维护另一套可编辑的“批准成果文本口径”。

Acceptance 始终以同一份结构化 ExpectedOutput 对比 ResearchOutcome，避免“申报成果、批复成果、验收成果”三套标准互相冲突。

如果未来需要在立项批复阶段逐项调整成果数量，应增加正式 AwardOutput / ProjectExpectedOutput 模型，而不是修改 Proposal 原始数据。

## 5. Baseline 是不可变的批准计划快照

项目从 PLANNING 激活为 ACTIVE 时生成 Baseline V1。

后续正式变更被批准并应用后，生成 Baseline V2 / V3 等新版本。

Baseline 一经生成不可修改、不可删除。计划偏差通过“当前执行事实 vs 历史 Baseline”计算，而不是回写旧 Baseline。

Project.current_baseline_id 始终指向当前有效版本。

## 6. WorkItem 统一表达 WBS

PHASE、WORK_PACKAGE、TASK、MILESTONE 共用 research_work_item，通过 parent_id 构造树形 WBS，避免分别维护三套 CRUD 模型。

关键约束：

- parent 必须属于同一项目；
- MILESTONE 不能作为父节点；
- TASK / MILESTONE 必须指定项目成员作为负责人；
- TASK / MILESTONE 必须有计划周期；
- 工作项周期不得超出项目批准周期；
- weight 范围为 0-100。

项目 ACTIVE 后，正式计划不得直接新增或改写，计划变化必须进入 Change Control。

## 7. Governance 是独立领域，并使用显式状态机

Risk、Issue、Decision、ChangeRequest 都是一等业务对象，不作为 ProgressReport 的附属字段存在。

Risk 与 Issue 语义分离：

- Risk：尚未发生的不确定事件；
- Issue：已经发生、需要解决的问题。

Risk、Issue、Proposal、Change、Acceptance 等对象使用明确状态迁移规则，禁止通过直接修改 status 绕过领域动作。

CLOSED / TERMINATED 项目不允许继续修改治理记录。

## 8. ChangeRequest 是 ACTIVE 项目修改正式计划的唯一正规入口

项目激活后，预算、周期等批准计划不得直接修改。

正式流程为：

ChangeRequest -> Submit -> Approve / Reject -> Apply -> Baseline V(next)

APPROVED 与 APPLIED 必须分离，表示“审批同意”和“实际写入正式计划”是两个不同事实。

提交和应用时都必须校验 before_value 与项目当前值一致，防止过期 Change 覆盖更新后的项目状态。

当前 Demo 正式支持：

- planned_end_date
- current_budget / total_budget

新增变更类型时应扩展领域模型，而不是绕过 Change Control。

## 9. Finance 只管理项目预算执行，不做会计系统

ResearchFlow 保留：

- Budget Version
- BudgetLine
- Expense

但不承担会计凭证、付款、发票、报销、税务等财务核算能力。

预算采用版本化设计：

- Change 调整预算时生成新的 Budget Version；
- 历史 Budget 不覆盖；
- Expense 保留发生时关联的 BudgetLine；
- 当前科目“累计已使用”按 project + category 跨预算版本汇总；
- 新预算不得低于已经发生的实际支出；
- 单笔支出不得超过项目总预算和对应预算科目额度。

## 10. ExpectedOutput 与 ResearchOutcome 分离

ExpectedOutput 表达“计划产出什么”，ResearchOutcome 表达“实际产出了什么”。

二者可以关联，但不能合并成同一实体。

Outcome 关联 ExpectedOutput 时，该 ExpectedOutput 必须属于当前项目，禁止跨项目关联。

验收准备度通过 ExpectedOutput 与已完成 Outcome 的结构化比较计算。

## 11. Acceptance 与 Closeout 分离

Acceptance 回答“项目成果是否达到验收要求”。

Closeout 回答“项目在行政和资料层面是否可以正式关闭”。

Acceptance APPROVED 后，Project 进入 CLOSING，而不是直接 CLOSED。

Closeout 完成后才执行：

- Closeout -> COMPLETED
- Project -> CLOSED
- ResearchRecord -> CLOSED

验收退回后允许基于同一 Acceptance 记录和编号重新提交，并创建新的 WorkflowInstance 保留流程历史。

## 12. 验收和结项使用确定性阻断规则

验收与结项的 Ready / Not Ready 不由 AI 决定。

系统通过确定性规则检查：

- TASK / MILESTONE 是否全部完成；
- HIGH / CRITICAL Issue 是否仍未解决；
- ExpectedOutput 是否仍有缺口；
- Expense 是否超过当前预算；
- 结项资料是否完整；
- Acceptance 是否已经批准。

AI 可以解释阻断原因或生成总结，但不能覆盖这些业务规则。

## 13. Document 元数据与物理存储解耦

research_document 保存：

- 业务归属
- 文件名
- category
- storage_provider
- storage_key
- MIME
- 文件大小
- 上传人
- 版本

文件本体当前使用本地文件存储目录 + Docker Volume。

业务层只依赖 Document / Storage 抽象，未来可以替换为 MinIO、S3、OSS 等对象存储，而不改变科研领域模型。

当前 Demo 的本地静态文件访问满足演示要求；生产版本应升级为私有对象存储或带鉴权的 Document Download API。

## 14. Workflow 与业务实体解耦

WorkflowInstance 通过 business_type + business_id 指向 Proposal、ChangeRequest、Acceptance 等业务对象。

业务结果和流程审计必须分开：

- Review / Acceptance / Change 保存业务结果；
- WorkflowAction 保存谁、何时、执行了什么流程动作。

上层业务只依赖 WorkflowService 抽象，当前使用轻量确定性实现，未来可替换为 Flowable 等流程引擎。

## 15. 权限 = System Role × Project Role × Business State

ResearchFlow 不使用单一菜单 RBAC 决定全部业务权限。

最终权限由三层共同决定：

### System Role

- admin：系统超级管理员；
- research_admin：科研管理员，可跨项目审批和治理；
- research_owner：科研业务用户；
- research_manager：全局只读管理者。

### Project Role

- PI
- PROJECT_MANAGER
- TECHNICAL_LEAD
- RESEARCHER
- FINANCE_CONTACT
- SPONSOR
- MEMBER

### Business State

例如 PLANNING、ACTIVE、CLOSING、CLOSED 会进一步限制可执行动作。

典型规则：

- PI / PROJECT_MANAGER：维护正式计划、团队、Change、验收申请；
- 项目成员：Progress / Risk / Issue / Decision / Outcome / Document；
- FINANCE_CONTACT：Expense；
- WorkItem owner：更新自己负责的 WorkItem 执行状态；
- research_manager：跨项目只读；
- 审批中心仅对 admin / research_admin 开放。

前端负责隐藏不可用操作，后端始终作为最终安全边界。

## 16. 采用 Modular Monolith，不提前拆微服务

当前技术架构保持：

- Spring Boot
- MyBatis
- MySQL
- Redis
- Vue
- Docker Compose

科研业务仍部署为一个模块化单体，但代码按领域组织。

当前阶段不引入微服务、消息队列、分布式事务等额外复杂度。只有当团队规模、部署隔离或负载边界明确出现时，再评估拆分。

## 17. AI 使用结构化业务上下文，而不是默认依赖 RAG

AI 通过 ProjectContext 获取：

- Award
- Current Baseline
- WorkItem
- ProgressReport
- Budget
- Risk
- Issue
- Decision
- Change
- Outcome

等结构化业务数据。

AI 的定位是 Copilot：

- 申报草稿
- 项目摘要
- 风险解释
- 进展报告草稿
- 变更影响分析
- 验收总结

AI 不直接决定权限、金额、状态迁移、审批结果、Risk Level 或验收结论。

当前不把 RAG 作为默认基础设施；只有出现明确的非结构化资料检索场景时再引入。

## 18. UI 追求高信息密度，而不是简单缩小字号

ResearchFlow 面向科研管理与项目治理场景，桌面端优先提高扫描效率。

当前约束：

- Sidebar 约 220px；
- Header / Brand 56px；
- Workspace Tab 40px；
- 常用 gap 约 7-10px；
- Card padding 约 8-12px；
- 表格、树形 WBS、compact row 优先于大 Hero / 大面积留白；
- 操作按钮保持可点击性，不通过极小字号换取“紧凑”；
- 项目概览优先使用信息分组和摘要层级，而不是堆叠大量小卡片；
- 移动端保留不超过 5 个主导航入口。

## 19. ResearchFlow 产品品牌与底层框架实现解耦

用户可见的产品名称、Logo、菜单、通知、默认用户、帮助入口和演示数据统一使用 ResearchFlow 品牌，不暴露底层后台框架品牌。

底层历史 package、配置 namespace、框架工具类等内部实现可以继续保留，只要不会进入产品界面，也不会影响领域边界。

不为“代码表面统一”进行高风险、低收益的大规模框架重命名。

## 20. Demo 与生产版本边界明确

当前 V2 的目标是“可运行、可演示、业务闭环完整的科研项目治理 Demo”，不是一次性实现生产级科研管理平台。

当前明确不作为 Demo 阻断项的能力包括：

- 浏览器级 Playwright E2E；
- 私有对象存储和受鉴权下载；
- 高并发业务编号服务；
- orphan file 自动清理；
- BPMN Designer / 会签 / 加签 / 委托；
- Award 阶段逐项调整 ExpectedOutput；
- 完整科研绩效、合同、知识产权、专家库等扩展域。

这些能力进入后续 Roadmap，而不是继续膨胀当前 Demo。

## 21. CI 必须验证领域不变量，而不只验证“能编译”

CI 除了 Backend Test、Frontend Build、Docker Compose 和 MySQL Schema 初始化，还必须验证关键数据不变量。

当前至少检查：

- Project.current_budget 与 ACTIVE Budget Version 一致；
- BudgetLine 合计等于 Budget total_amount；
- WorkItem parent 不跨项目；
- TASK / MILESTONE owner 属于项目；
- current_baseline_id 属于当前项目；
- ACTIVE / CLOSING 项目实际支出不超过当前预算；
- CLOSED 项目拥有 APPROVED Acceptance 和 COMPLETED Closeout；
- UNDER_REVIEW Proposal 拥有 RUNNING Workflow；
- Seed 数据不重新引入已清理的旧框架品牌信息。

设计决策应尽量转化为自动化测试或 CI invariant，避免文档与实现长期漂移。
