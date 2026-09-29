# ResearchFlow Design System

> 基于 UI UX Pro Max 的系统级工作流，为科研项目全生命周期管理场景生成并落地。

## Product profile

- Product: enterprise research project management / internal operations dashboard
- Primary users: 项目负责人、科研管理员、管理者
- Interaction model: desktop-first productivity, mobile-compatible
- Density: 7/10，偏数据密集但不压缩可点击区域
- Motion: 3/10，仅保留状态反馈、hover/press、页面层级必要过渡
- Variance: 4/10，保持企业级秩序与稳定性

## Visual direction

- Style: Minimal / Swiss + Bento dashboard
- Avoid: decorative glassmorphism, neon AI gradients, oversized empty hero blocks, tiny 10px body text, color-only state expression
- Surface: light neutral background + white panels + low elevation
- Navigation: dark stable shell, clear current-location indicator
- Data: tabular figures for numeric values, semantic colors for risk/status, text/icon accompanies color

## Tokens

```css
--rf-bg: #f4f7fb;
--rf-surface: #ffffff;
--rf-surface-subtle: #f8fafc;
--rf-border: #dfe5ec;
--rf-border-strong: #cbd5e1;
--rf-text: #172033;
--rf-text-secondary: #526174;
--rf-text-muted: #667085;
--rf-primary: #2563eb;
--rf-primary-hover: #1d4ed8;
--rf-primary-soft: #eff6ff;
--rf-success: #15803d;
--rf-warning: #b45309;
--rf-danger: #b42318;
--rf-sidebar: #0b1220;
--rf-radius-sm: 8px;
--rf-radius-md: 12px;
--rf-radius-lg: 16px;
--rf-shadow-sm: 0 1px 2px rgba(16,24,40,.05);
--rf-shadow-md: 0 8px 24px rgba(16,24,40,.07);
```

## Type scale

- 12: metadata / secondary labels only
- 14: default UI body / buttons / inputs
- 16: emphasized body / card title
- 20: page title
- 28: hero / primary metric
- Body line-height: 1.5–1.65

Chinese font stack stays system-native to avoid external font loading and keep rendering reliable.

## Spacing

Use 4/8-based rhythm: 4, 8, 12, 16, 24, 32, 40, 48.

## Interaction rules

- All primary pointer targets >= 44px.
- One primary CTA per page region.
- Hover is supplemental; click/tap is always sufficient.
- Buttons/links/cards have visible focus states.
- Async actions keep loading feedback.
- No generic clickable div for primary navigation; use button/link semantics.
- Respect `prefers-reduced-motion`.

## Responsive

- >= 1200: full sidebar, 3/4-column dashboard layouts.
- 768–1199: compact desktop/tablet layouts.
- < 768: bottom navigation with max 5 items; content-first stacking; no horizontal overflow.
- Verify at 375 / 768 / 1024 / 1440 widths.

## Page intent

- Login: trust + clear authentication, visible labels, demo context.
- Dashboard: decision-first, prioritize pending work/risk over decorative hero.
- Projects: strong search/filter hierarchy, cards optimized for scanability.
- Project Workspace: lifecycle context first, then execution tabs.
- Approvals: action queue with decision confidence and destructive separation.
- Risks: explainable risk evidence, no color-only severity.
- Analytics: compact executive summary + readable lifecycle/health/budget views.
