# ResearchFlow MVP Spec

## Goal
Provide a runnable research-project lifecycle MVP that turns an ambiguous management requirement into a concrete, testable business model.

## Core lifecycle
DRAFT -> PENDING_APPROVAL -> APPROVED -> IN_PROGRESS -> PENDING_ACCEPTANCE -> COMPLETED.
REJECTED may return to PENDING_APPROVAL after edits.

## Product surfaces
- Research dashboard
- Project collection
- Project Workspace
- Approval center
- Risk projects
- Portfolio analytics
- Separate RuoYi admin console for infrastructure administration

## Rules
- Project owners create, submit, and execute their projects.
- Research administrators approve applications, start projects, and review acceptance.
- Managers have portfolio read access.
- Core state transitions are deterministic and audited.
- AI is assistive only and cannot mutate approvals, permissions, budgets, or lifecycle state.
