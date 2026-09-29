# ResearchFlow Module Roadmap

## MVP implemented

- `ruoyi-research`: independent research-domain module.
- Project lifecycle: draft, approval, launch, execution, acceptance, completion.
- Project Workspace: milestones, progress, budget usage, deliverables, workflow history.
- Deterministic risk rules comparing schedule, progress, and budget execution.
- Product UI separated from the RuoYi Admin Console.
- Optional AI proposal copilot.
- Docker Compose runtime and seeded demonstration data.

## Validated extension points

### Workflow
Current: `WorkflowService -> SimpleWorkflowService`.

Future, only after real approval rules are validated: `WorkflowService -> FlowableWorkflowService` or another BPM implementation.

### Integration
Add an integration layer when requirements for OA, finance, e-signature, notifications, or other institute systems become concrete.

### Knowledge / RAG
Do not add RAG until real users need cross-document policy search, similar-project retrieval, or evidence-grounded Q&A.

### Agent runtime
Do not add autonomous agents until there are long-running or cross-system tasks that cannot be handled well by deterministic services plus AI assistance.
