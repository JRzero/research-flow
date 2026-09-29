# 000 ResearchFlow V2 Architecture

## Goal
重建科研项目全过程领域模型，V1 Schema 不兼容迁移。

## Invariants
1. Proposal、Award、Project 分离。
2. Baseline 不可变。
3. Risk 与 Issue 分离。
4. Change APPROVED 后必须显式 APPLIED 才改变计划。
5. Acceptance 与 Closeout 分离。
6. 核心业务关闭 AI 后必须完整可运行。

## Acceptance
- MySQL V2 Schema 可全新初始化。
- Demo seed 覆盖 REVIEW / ACTIVE / RISK / CLOSED。
- 后端和前端不再调用 V1 Milestone/Deliverable/Approval 模型。
