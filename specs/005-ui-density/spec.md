# 005 Compact Research UI

## Desktop shell
- Sidebar：220px。
- Brand / top header：56px。
- 页面主内容 padding：约 12px 16px。
- 页面/卡片常用 gap：7-10px。
- Project Workspace Tab 高度：40px。
- Card padding：约 8-12px。
- 优先使用 compact rows / table / tree / inline metrics，避免大面积 Hero 和重复卡片层级。
- 数字和状态集中展示，说明文字降级为 9-11px 辅助信息。

## Navigation
工作台 / 项目申请 / 科研项目 / 审批中心 / 风险问题 / 数据概览。
- 审批中心只对 research_admin / admin 显示。
- 管理者保持全局只读。
- 科研用户进入项目后根据 ProjectMember role 获得上下文权限。

## Project Workspace
Overview / Plan / Execution / Finance / Outcomes / Documents / Changes / Workflow。

- Plan 使用树形 WBS，支持 parent / owner / weight。
- Execution 将 Progress / Risk / Issue / Decision 组织在同一高密度页面。
- Proposal 审批采用紧凑 Award 批复弹窗，清晰区分申请值与批准值。

## Mobile
- 保持最多 5 个底部主导航。
- 次级能力放进 Project Workspace。
- 小屏改为单列，但不通过压缩点击目标牺牲可用性。
