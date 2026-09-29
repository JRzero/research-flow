package com.ruoyi.research.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.research.domain.ResearchAcceptance;
import com.ruoyi.research.domain.ResearchDeliverable;
import com.ruoyi.research.domain.ResearchExpense;
import com.ruoyi.research.domain.ResearchMilestone;
import com.ruoyi.research.domain.ResearchProgress;
import com.ruoyi.research.domain.ResearchProject;
import com.ruoyi.research.domain.ResearchProjectStatus;
import com.ruoyi.research.mapper.ResearchAcceptanceMapper;
import com.ruoyi.research.mapper.ResearchApprovalMapper;
import com.ruoyi.research.mapper.ResearchDeliverableMapper;
import com.ruoyi.research.mapper.ResearchExpenseMapper;
import com.ruoyi.research.mapper.ResearchMilestoneMapper;
import com.ruoyi.research.mapper.ResearchProgressMapper;
import com.ruoyi.research.mapper.ResearchProjectMapper;
import com.ruoyi.research.service.IResearchProjectService;
import com.ruoyi.research.workflow.WorkflowService;

@Service
public class ResearchProjectServiceImpl implements IResearchProjectService {
    @Autowired private ResearchProjectMapper projectMapper;
    @Autowired private ResearchMilestoneMapper milestoneMapper;
    @Autowired private ResearchProgressMapper progressMapper;
    @Autowired private ResearchExpenseMapper expenseMapper;
    @Autowired private ResearchDeliverableMapper deliverableMapper;
    @Autowired private ResearchApprovalMapper approvalMapper;
    @Autowired private ResearchAcceptanceMapper acceptanceMapper;
    @Autowired private WorkflowService workflowService;

    @Override
    public List<ResearchProject> listProjects(Long userId, boolean canViewAll, String status, String keyword) {
        List<ResearchProject> projects = projectMapper.selectProjectList(canViewAll ? null : userId, status, keyword);
        projects.forEach(this::enrich);
        return projects;
    }

    @Override
    public Map<String, Object> getDashboard(Long userId, boolean canViewAll) {
        List<ResearchProject> projects = listProjects(userId, canViewAll, null, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProjects", projects.size());
        result.put("pendingApproval", count(projects, ResearchProjectStatus.PENDING_APPROVAL));
        result.put("inProgress", count(projects, ResearchProjectStatus.IN_PROGRESS));
        result.put("pendingAcceptance", count(projects, ResearchProjectStatus.PENDING_ACCEPTANCE));
        result.put("completed", count(projects, ResearchProjectStatus.COMPLETED));
        result.put("riskCount", projects.stream().filter(p -> !"NONE".equals(p.getRiskLevel())).count());
        BigDecimal totalBudget = projects.stream().map(p -> nz(p.getTotalBudget())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usedBudget = projects.stream().map(p -> nz(p.getUsedBudget())).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalBudget", totalBudget);
        result.put("usedBudget", usedBudget);
        result.put("budgetExecutionRate", percent(usedBudget, totalBudget));
        result.put("averageProgress", projects.isEmpty() ? 0 : Math.round(projects.stream().mapToInt(p -> p.getProgress() == null ? 0 : p.getProgress()).average().orElse(0)));
        result.put("recentProjects", projects.stream().limit(5).collect(Collectors.toList()));
        result.put("riskProjects", projects.stream().filter(p -> !"NONE".equals(p.getRiskLevel())).limit(5).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> getProjectDetail(Long projectId, Long userId, boolean canViewAll) {
        ResearchProject project = required(projectId);
        checkAccess(project, userId, canViewAll);
        enrich(project);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("project", project);
        detail.put("milestones", milestoneMapper.selectByProjectId(projectId));
        detail.put("progressRecords", progressMapper.selectByProjectId(projectId));
        detail.put("expenses", expenseMapper.selectByProjectId(projectId));
        detail.put("deliverables", deliverableMapper.selectByProjectId(projectId));
        detail.put("approvals", approvalMapper.selectByProjectId(projectId));
        detail.put("acceptance", acceptanceMapper.selectByProjectId(projectId));
        return detail;
    }

    @Override
    @Transactional
    public ResearchProject createProject(ResearchProject project, Long userId, Long deptId, String username) {
        if (project == null || StringUtils.isEmpty(project.getProjectName())) {
            throw new ServiceException("项目名称不能为空");
        }
        if (project.getPlannedEndDate() == null) {
            throw new ServiceException("计划结束时间不能为空");
        }
        project.setProjectNo(StringUtils.isEmpty(project.getProjectNo()) ? nextProjectNo() : project.getProjectNo());
        project.setOwnerUserId(userId);
        project.setDeptId(deptId);
        project.setStatus(ResearchProjectStatus.DRAFT);
        project.setProgress(0);
        project.setCreateBy(username);
        project.setUpdateBy(username);
        if (project.getTotalBudget() == null) project.setTotalBudget(BigDecimal.ZERO);
        projectMapper.insertProject(project);
        return projectMapper.selectProjectById(project.getProjectId());
    }

    @Override
    @Transactional
    public void updateDraft(ResearchProject project, Long userId, boolean canManageAll, String username) {
        ResearchProject current = required(project.getProjectId());
        checkOwner(current, userId, canManageAll);
        if (!ResearchProjectStatus.DRAFT.equals(current.getStatus()) && !ResearchProjectStatus.REJECTED.equals(current.getStatus())) {
            throw new ServiceException("仅草稿或已驳回项目可以编辑申报信息");
        }
        project.setUpdateBy(username);
        projectMapper.updateProject(project);
    }

    @Override
    public void submitProject(Long projectId, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        workflowService.submitProject(projectId, userId, username);
    }

    @Override public void approveProject(Long projectId, Long userId, String username, String comment) { workflowService.approveProject(projectId, userId, username, comment); }
    @Override public void rejectProject(Long projectId, Long userId, String username, String comment) { workflowService.rejectProject(projectId, userId, username, comment); }
    @Override public void startProject(Long projectId, Long userId, String username) { workflowService.startProject(projectId, userId, username); }

    @Override
    @Transactional
    public void addMilestone(Long projectId, ResearchMilestone milestone, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        ensureExecutionEditable(project);
        if (milestone == null || StringUtils.isEmpty(milestone.getTitle())) throw new ServiceException("里程碑名称不能为空");
        milestone.setProjectId(projectId);
        milestone.setStatus("PENDING");
        milestone.setCreateBy(username);
        milestoneMapper.insertMilestone(milestone);
    }

    @Override
    @Transactional
    public void completeMilestone(Long projectId, Long milestoneId, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        ensureExecutionEditable(project);
        ResearchMilestone milestone = milestoneMapper.selectById(milestoneId);
        if (milestone == null || !projectId.equals(milestone.getProjectId())) throw new ServiceException("里程碑不存在");
        milestoneMapper.completeMilestone(milestoneId, username);
    }

    @Override
    @Transactional
    public void addProgress(Long projectId, ResearchProgress progress, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        ensureExecutionEditable(project);
        if (progress == null || progress.getProgressPercent() == null || progress.getProgressPercent() < 0 || progress.getProgressPercent() > 100) {
            throw new ServiceException("项目进度必须在 0-100 之间");
        }
        progress.setProjectId(projectId);
        progress.setRecorderUserId(userId);
        progress.setCreateBy(username);
        if (progress.getRecordDate() == null) progress.setRecordDate(new Date());
        progressMapper.insertProgress(progress);
        projectMapper.updateProjectProgress(projectId, progress.getProgressPercent(), username);
    }

    @Override
    @Transactional
    public void addExpense(Long projectId, ResearchExpense expense, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        ensureExecutionEditable(project);
        if (expense == null || expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("支出金额必须大于 0");
        }
        BigDecimal used = nz(expenseMapper.sumByProjectId(projectId));
        if (project.getTotalBudget() != null && used.add(expense.getAmount()).compareTo(project.getTotalBudget()) > 0) {
            throw new ServiceException("本次支出将超过项目总预算");
        }
        expense.setProjectId(projectId);
        expense.setCreateBy(username);
        if (expense.getExpenseDate() == null) expense.setExpenseDate(new Date());
        expenseMapper.insertExpense(expense);
    }

    @Override
    @Transactional
    public void addDeliverable(Long projectId, ResearchDeliverable deliverable, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        ensureExecutionEditable(project);
        if (deliverable == null || StringUtils.isEmpty(deliverable.getName())) throw new ServiceException("成果名称不能为空");
        deliverable.setProjectId(projectId);
        deliverable.setCreateBy(username);
        if (deliverable.getCompletedDate() == null) deliverable.setCompletedDate(new Date());
        deliverableMapper.insertDeliverable(deliverable);
    }

    @Override
    @Transactional
    public void submitAcceptance(Long projectId, ResearchAcceptance acceptance, Long userId, boolean canManageAll, String username) {
        ResearchProject project = required(projectId);
        checkOwner(project, userId, canManageAll);
        if (!ResearchProjectStatus.IN_PROGRESS.equals(project.getStatus())) throw new ServiceException("仅执行中的项目可以提交验收");
        if (acceptance == null || StringUtils.isEmpty(acceptance.getProjectSummary())) throw new ServiceException("项目总结不能为空");

        ResearchAcceptance existing = acceptanceMapper.selectByProjectId(projectId);
        if (existing != null && !"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException("该项目已存在待处理或已完成的验收申请");
        }

        acceptance.setProjectId(projectId);
        acceptance.setStatus("PENDING");
        if (existing == null) {
            acceptance.setCreateBy(username);
            acceptanceMapper.insertAcceptance(acceptance);
        } else {
            acceptance.setUpdateBy(username);
            acceptanceMapper.resubmitAcceptance(acceptance);
        }
        workflowService.submitAcceptance(projectId, userId, username);
    }

    @Override
    @Transactional
    public void reviewAcceptance(Long projectId, Long userId, String username, boolean approved, String comment) {
        ResearchAcceptance acceptance = acceptanceMapper.selectByProjectId(projectId);
        if (acceptance == null || !"PENDING".equals(acceptance.getStatus())) throw new ServiceException("当前没有待处理的验收申请");
        acceptance.setReviewerId(userId);
        acceptance.setReviewComment(comment);
        acceptance.setStatus(approved ? "APPROVED" : "REJECTED");
        acceptance.setUpdateBy(username);
        acceptanceMapper.updateAcceptanceReview(acceptance);
        workflowService.reviewAcceptance(projectId, userId, username, approved, comment);
    }

    @Override
    public List<ResearchProject> listRisks(Long userId, boolean canViewAll) {
        return listProjects(userId, canViewAll, null, null).stream()
                .filter(p -> !"NONE".equals(p.getRiskLevel()))
                .collect(Collectors.toList());
    }

    private ResearchProject required(Long projectId) {
        ResearchProject project = projectMapper.selectProjectById(projectId);
        if (project == null) throw new ServiceException("项目不存在");
        return project;
    }

    private void checkAccess(ResearchProject project, Long userId, boolean canViewAll) {
        if (!canViewAll && !userId.equals(project.getOwnerUserId())) throw new ServiceException("无权访问该项目");
    }

    private void checkOwner(ResearchProject project, Long userId, boolean canManageAll) {
        if (!canManageAll && !userId.equals(project.getOwnerUserId())) throw new ServiceException("仅项目负责人可以执行该操作");
    }

    private void ensureExecutionEditable(ResearchProject project) {
        if (!ResearchProjectStatus.IN_PROGRESS.equals(project.getStatus())) throw new ServiceException("当前项目不在执行中状态");
    }

    private long count(List<ResearchProject> projects, String status) {
        return projects.stream().filter(p -> status.equals(p.getStatus())).count();
    }

    private void enrich(ResearchProject project) {
        BigDecimal total = nz(project.getTotalBudget());
        BigDecimal used = nz(project.getUsedBudget());
        project.setRemainingBudget(total.subtract(used));
        project.setRiskLevel("NONE");
        project.setRiskReason("");
        if (!ResearchProjectStatus.IN_PROGRESS.equals(project.getStatus()) && !ResearchProjectStatus.PENDING_ACCEPTANCE.equals(project.getStatus())) return;
        int progress = project.getProgress() == null ? 0 : project.getProgress();
        List<String> reasons = new ArrayList<>();
        String level = "NONE";
        int timePercent = timePercent(project.getStartDate(), project.getPlannedEndDate());
        int timeGap = timePercent - progress;
        if (timeGap >= 20) {
            reasons.add("时间进度 " + timePercent + "% 已明显快于任务完成度 " + progress + "%");
            level = timeGap >= 35 ? "HIGH" : "MEDIUM";
        }
        int budgetPercent = percent(used, total);
        int budgetGap = budgetPercent - progress;
        if (budgetGap >= 25) {
            reasons.add("预算执行 " + budgetPercent + "% 明显快于任务完成度 " + progress + "%");
            if (budgetGap >= 40) level = "HIGH"; else if ("NONE".equals(level)) level = "MEDIUM";
        }
        if (project.getPlannedEndDate() != null && project.getPlannedEndDate().before(new Date()) && progress < 100) {
            reasons.add("项目已超过计划结束时间但尚未完成");
            level = "HIGH";
        }
        project.setRiskLevel(level);
        project.setRiskReason(String.join("；", reasons));
    }

    private int timePercent(Date start, Date end) {
        if (start == null || end == null || !end.after(start)) return 0;
        Instant now = Instant.now();
        long total = Math.max(1, Duration.between(start.toInstant(), end.toInstant()).toMillis());
        long elapsed = Duration.between(start.toInstant(), now).toMillis();
        return (int) Math.max(0, Math.min(100, Math.round(elapsed * 100.0 / total)));
    }

    private int percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private String nextProjectNo() {
        return "RF-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
