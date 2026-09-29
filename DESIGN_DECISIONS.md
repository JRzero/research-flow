# ResearchFlow V2 设计决策

## 1. 重新建模，不兼容 V1 Schema
当前仍是 Demo，无生产数据迁移负担。V2 直接建立新 Schema 和 Seed，V1 表结构不再作为约束。

## 2. ResearchRecord 是统一身份，不是超级聚合
ResearchRecord 负责跨生命周期关联和导航；Proposal、Project、ChangeRequest、Budget 等保持独立事务边界。

## 3. Proposal / Award / Project 分离
Proposal 保存申请值；Award 保存批准值；Project 保存正式项目当前运行状态。避免一列数据在生命周期中被持续覆盖。

## 4. Immutable Baseline
项目激活生成 Baseline V1；批准变更应用后生成 V2/V3。Baseline 只读，计划偏差通过当前执行事实与基线快照比较。

## 5. WorkItem 统一 WBS
PHASE / WORK_PACKAGE / TASK / MILESTONE 使用一张 research_work_item 表，通过 parent_id 构造树，避免三套 CRUD 模型。

## 6. Governance 独立
Risk、Issue、Decision、ChangeRequest 都是一等业务对象。Risk 发生后可转换为 Issue；Change APPROVED 与 APPLIED 分离。

## 7. Finance 是项目预算执行，不是财务系统
保留 Budget Version、BudgetLine、Expense，不承担会计凭证、付款、发票和报销。

## 8. Document 元数据与存储解耦
research_document 保存业务归属与文件元数据，storage_provider 当前为 LOCAL。文件本体继续使用 RuoYi uploadPath + Docker Volume；未来可替换 MinIO/S3。

## 9. Workflow 与业务实体解耦
WorkflowInstance 使用 business_type + business_id 指向 Proposal、ChangeRequest、Acceptance 等对象。业务结果与流程动作审计分开。

## 10. Modular Monolith
继续 Spring Boot + RuoYi + MyBatis + MySQL。科研模块保持单体部署，但按领域组织服务和 API。

## 11. AI 使用结构化上下文
AI 从 ProjectContext 获取 Award、Baseline、WorkItem、Budget、Risk、Issue、Change、Outcome 等结构化信息，不以 RAG 作为默认前提。

## 12. UI 紧凑而非拥挤
桌面端面向科研管理人员，以扫描效率为目标。减少大 Hero 和过宽留白；保留明确层级、状态和可访问点击区域。
