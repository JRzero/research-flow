# ResearchFlow V2 Design System

## Product profile
- Product: enterprise scientific research governance workspace
- Users: 科研用户、科研管理员、管理者
- Desktop: high-density operational UI
- Mobile: compatible, max 5 primary bottom-nav items
- Density: 8/10 desktop, 6/10 mobile
- Motion: restrained

## Visual direction
- Minimal / Swiss enterprise UI.
- Data tables, compact rows and tree structures before oversized cards.
- No decorative glassmorphism, neon AI gradients or empty hero sections.
- Light neutral background, white working surfaces, dark navigation shell.
- Status always uses text plus semantic color.

## Core tokens
```css
--rf-bg: #f4f7fb;
--rf-surface: #ffffff;
--rf-surface-subtle: #f8fafc;
--rf-border: #dfe5ec;
--rf-text: #172033;
--rf-text-secondary: #526174;
--rf-text-muted: #667085;
--rf-primary: #2563eb;
--rf-primary-soft: #eff6ff;
--rf-success: #15803d;
--rf-warning: #b45309;
--rf-danger: #b42318;
--rf-sidebar: #0b1220;
```

## Density rules
- Desktop sidebar: ~220px.
- Desktop product header: ~64px.
- Page spacing: 10–16px for primary operational regions.
- Card / panel padding: 8–16px.
- Desktop form controls: >=36px.
- Mobile primary controls / touch targets: >=44px.
- Metadata: 9–11px only when secondary; primary body remains readable.
- Tables are preferred for lists with 4+ comparable attributes.
- Project Workspace uses compact tabs, not top-level menu explosion.

## Information architecture
Primary navigation:
- 工作台
- 项目申请
- 科研项目
- 审批中心
- 风险与问题
- 数据概览

Project Workspace:
- 概览
- 计划
- 执行
- 经费
- 成果
- 资料
- 变更
- 流程

## Interaction
- Domain actions use explicit verbs: 提交评审、激活项目、应用变更、验收通过。
- Do not expose raw status editing.
- Async actions show loading/feedback.
- Hover is supplemental; keyboard/touch actions remain available.
- Visible focus states.
- Respect reduced-motion.
- Destructive or irreversible actions require confirmation.

## Responsive
- >=1200: 220px sidebar and high-density tables.
- 768–1199: 72px compact sidebar, content remains table-first where possible.
- <768: bottom navigation, max 5 visible entries; controls restore >=44px height; complex grids stack.
