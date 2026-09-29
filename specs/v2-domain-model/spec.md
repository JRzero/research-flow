# ResearchFlow V2 Domain Model

## Goal

ResearchFlow V2 models scientific research governance around a unified ResearchRecord while separating Proposal, Award, Project, Baseline and execution facts.

## Core lifecycle

ResearchCall -> ResearchRecord -> Proposal -> Review -> Award -> Project(PLANNING) -> Baseline -> Project(ACTIVE) -> Acceptance -> Closeout.

## Aggregate boundaries

- ResearchRecord: shared identity only.
- Proposal: application facts, team, requested budget and expected outputs.
- Award: immutable establishment decision.
- Project: current execution identity and membership.
- Baseline: immutable approved-plan snapshot.
- WorkItem: PHASE / WORK_PACKAGE / TASK / MILESTONE.
- Risk and Issue: separate governance facts; a Risk may be converted to an Issue.
- ChangeRequest: independently approved and applied. Applying creates a new Baseline.
- Budget: versioned budget with immutable historical versions.
- Outcome: actual research output linked to expected output.
- Acceptance and Closeout: acceptance result is distinct from administrative project closure.
- Document: business-linked metadata; storage provider is pluggable and Demo uses LOCAL.
- Workflow: generic business workflow audit, not tied to Project.

## Invariants

1. Proposal != Project.
2. Requested values, awarded values, current values and actual values are not overwritten into one field.
3. Baselines are immutable.
4. Project activation requires a plan, budget and project team.
5. Active-project plan changes go through ChangeRequest.
6. APPROVED ChangeRequest != APPLIED ChangeRequest.
7. Risk probability x impact determines score and level; AI never decides core risk state.
8. A project is CLOSED only after Acceptance is approved and Closeout is completed.
9. Submitted/approved business records are not physically deleted.
10. AI consumes structured ProjectContext and cannot mutate workflow, permissions, money or lifecycle state directly.

## Demo scope

The Demo implements the full minimal lifecycle with compact UI, but intentionally omits BPMN designer, accounting, procurement, expert-pool administration, advanced resource leveling and full EVM.
