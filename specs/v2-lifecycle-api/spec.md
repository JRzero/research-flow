# V2 Lifecycle API

## Proposal

- POST /research/proposals
- PUT /research/proposals/{id}
- GET /research/proposals/{id}/validation
- POST /research/proposals/{id}/submit
- POST /research/proposals/{id}/approve | reject
- POST /research/proposals/{id}/award

Submission is validated by deterministic completeness rules. Award issuance creates a PLANNING Project but does not activate it.

## Project

- POST /research/projects/{id}/work-items
- POST /research/projects/{id}/activate
- POST /research/projects/{id}/work-items/{workItemId}/start|complete|block
- POST /research/projects/{id}/progress-reports

Activation creates immutable Baseline V1.

## Governance

- POST /research/projects/{id}/risks
- POST /research/projects/{id}/risks/{riskId}/occur
- POST /research/projects/{id}/issues
- POST /research/projects/{id}/issues/{issueId}/resolve
- POST /research/projects/{id}/changes
- POST /research/projects/{id}/changes/{changeId}/submit|approve|reject|apply

Risk score = probability * impact. Risk occurrence creates an Issue. Change APPROVED and APPLIED are separate states; apply creates the next baseline.

## Closeout

- POST /research/projects/{id}/acceptance
- POST /research/projects/{id}/acceptance/approve|reject
- POST /research/projects/{id}/closeout

Acceptance approval does not close a project. Closeout requires checklist completion and no open HIGH/CRITICAL issues.
