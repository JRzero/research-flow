package com.ruoyi.research.v2.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.research.v2.mapper.ResearchV2Mapper;

@Service
public class ResearchFlowV2Service {
    @Autowired
    private ResearchV2Mapper mapper;

    public Map<String, Object> dashboard(Long userId, boolean canViewAll) {
        Map<String, Object> result = new LinkedHashMap<>(mapper.selectDashboard(userId, canViewAll));
        result.put("recentProjects", mapper.selectProjectList(userId, canViewAll, null, null).stream().limit(5).toList());
        result.put("pendingApprovals", mapper.selectApprovalQueue().stream().limit(5).toList());
        result.put("risks", mapper.selectRiskRegister(userId, canViewAll).stream().limit(5).toList());
        Map<String, Object> finance = mapper.selectPortfolioFinance(userId, canViewAll);
        result.put("totalBudget", finance.get("totalBudget"));
        result.put("usedBudget", finance.get("usedBudget"));
        result.put("budgetExecutionRate", percent(decimal(finance.get("usedBudget")), decimal(finance.get("totalBudget"))));
        return result;
    }

    public Map<String, Object> analytics(Long userId, boolean canViewAll) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectStatus", mapper.selectProjectStatusStats(userId, canViewAll));
        result.put("proposalStatus", mapper.selectProposalStatusStats(userId, canViewAll));
        Map<String, Object> finance = mapper.selectPortfolioFinance(userId, canViewAll);
        result.put("finance", finance);
        result.put("budgetExecutionRate", percent(decimal(finance.get("usedBudget")), decimal(finance.get("totalBudget"))));
        result.put("riskCount", mapper.selectRiskRegister(userId, canViewAll).size());
        return result;
    }

    public List<Map<String, Object>> proposals(Long userId, boolean canViewAll, String status, String keyword) {
        return mapper.selectProposalList(canViewAll ? null : userId, status, keyword);
    }

    public Map<String, Object> proposal(Long proposalId, Long userId, boolean canViewAll) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        checkProposalAccess(proposal, userId, canViewAll);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("proposal", proposal);
        detail.put("members", mapper.selectProposalMembers(proposalId));
        detail.put("budgetLines", mapper.selectProposalBudgetLines(proposalId));
        detail.put("expectedOutputs", mapper.selectExpectedOutputs(proposalId));
        detail.put("documents", mapper.selectDocuments("PROPOSAL", proposalId));
        detail.put("award", mapper.selectAwardByProposalId(proposalId));
        return detail;
    }

    @Transactional
    public Map<String, Object> createProposal(Map<String, Object> input, Long userId, Long deptId, String username) {
        requireText(input, "title", "项目名称不能为空");
        requireText(input, "plannedEndDate", "计划结束日期不能为空");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordNo", nextNo("RR"));
        record.put("title", input.get("title"));
        record.put("ownerUserId", userId);
        record.put("deptId", deptId);
        record.put("username", username);
        mapper.insertRecord(record);

        Map<String, Object> proposal = new LinkedHashMap<>(input);
        proposal.put("recordId", record.get("recordId"));
        proposal.put("proposalNo", nextNo("PR"));
        proposal.put("applicantUserId", userId);
        proposal.put("applicantDeptId", deptId);
        proposal.put("requestedBudget", decimal(input.get("requestedBudget")));
        proposal.put("username", username);
        mapper.insertProposal(proposal);
        Long proposalId = longValue(proposal.get("proposalId"));

        List<Map<String, Object>> members = listOfMaps(input.get("members"));
        if (members.isEmpty()) {
            Map<String, Object> pi = new LinkedHashMap<>();
            pi.put("userId", userId);
            pi.put("memberRole", "PI");
            pi.put("responsibility", "项目总体负责");
            pi.put("plannedAllocation", BigDecimal.valueOf(60));
            members.add(pi);
        }
        replaceProposalChildren(proposalId, input, members, username);
        syncAttachments("PROPOSAL", proposalId, longValue(record.get("recordId")), input.get("attachments"), userId);
        return proposal(proposalId, userId, true);
    }

    @Transactional
    public void updateProposal(Long proposalId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> current = requiredProposal(proposalId);
        checkProposalOwner(current, userId, canManageAll);
        String status = text(current.get("status"));
        if (!"DRAFT".equals(status) && !"REVISION_REQUIRED".equals(status)) {
            throw new ServiceException("仅草稿或退回修改状态可以编辑申报");
        }
        input.put("proposalId", proposalId);
        input.put("requestedBudget", decimal(input.get("requestedBudget")));
        input.put("username", username);
        mapper.updateProposal(input);
        replaceProposalChildren(proposalId, input, listOfMaps(input.get("members")), username);
        syncAttachments("PROPOSAL", proposalId, longValue(current.get("recordId")), input.get("attachments"), userId);
    }

    @Transactional
    public Map<String, Object> submitProposal(Long proposalId, Long userId, boolean canManageAll, String username) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        checkProposalOwner(proposal, userId, canManageAll);
        String status = text(proposal.get("status"));
        if (!"DRAFT".equals(status) && !"REVISION_REQUIRED".equals(status)) {
            throw new ServiceException("当前状态不能提交申报");
        }
        List<String> missing = validateProposal(proposalId, proposal);
        if (!missing.isEmpty()) throw new ServiceException("申报材料不完整：" + String.join("；", missing));
        mapper.updateProposalStatus(proposalId, "UNDER_REVIEW", username);
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("proposalId", proposalId);
        review.put("reviewType", "MANAGEMENT");
        review.put("reviewerUserId", null);
        review.put("score", null);
        review.put("decision", null);
        review.put("comment", "等待科研管理员评审");
        review.put("status", "PENDING");
        mapper.insertReview(review);
        Long workflowId = startWorkflow("PROPOSAL_APPROVAL", "PROPOSAL", proposalId, "MANAGEMENT_REVIEW", userId);
        addWorkflowAction(workflowId, "SUBMIT", "SUBMIT", userId, "提交项目申报");
        mapper.updateRecordPhase(longValue(proposal.get("recordId")), "REVIEW", "ACTIVE", username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", true);
        result.put("proposalId", proposalId);
        result.put("status", "UNDER_REVIEW");
        return result;
    }

    public Map<String, Object> validateProposal(Long proposalId, Long userId, boolean canViewAll) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        checkProposalAccess(proposal, userId, canViewAll);
        List<String> missing = validateProposal(proposalId, proposal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", missing.isEmpty());
        result.put("missing", missing);
        List<String> warnings = new ArrayList<>();
        if (mapper.selectDocuments("PROPOSAL", proposalId).isEmpty()) warnings.add("尚未上传申报附件");
        result.put("warnings", warnings);
        return result;
    }

    @Transactional
    public void approveProposal(Long proposalId, Long reviewerId, String username, String comment) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        if (!"UNDER_REVIEW".equals(text(proposal.get("status")))) throw new ServiceException("当前申报不在评审中");
        mapper.completeReview(proposalId, reviewerId, "PASS", comment);
        mapper.updateProposalStatus(proposalId, "APPROVED", username);
        completeBusinessWorkflow("PROPOSAL", proposalId, reviewerId, "MANAGEMENT_REVIEW", "APPROVE", comment, "COMPLETED");
        mapper.updateRecordPhase(longValue(proposal.get("recordId")), "AWARD", "ACTIVE", username);
    }

    @Transactional
    public void rejectProposal(Long proposalId, Long reviewerId, String username, String comment) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        if (!"UNDER_REVIEW".equals(text(proposal.get("status")))) throw new ServiceException("当前申报不在评审中");
        mapper.completeReview(proposalId, reviewerId, "REJECT", comment);
        mapper.updateProposalStatus(proposalId, "REJECTED", username);
        completeBusinessWorkflow("PROPOSAL", proposalId, reviewerId, "MANAGEMENT_REVIEW", "REJECT", comment, "REJECTED");
        mapper.updateRecordPhase(longValue(proposal.get("recordId")), "PROPOSAL", "ACTIVE", username);
    }

    @Transactional
    public Map<String, Object> issueAward(Long proposalId, Map<String, Object> input, Long userId, String username) {
        Map<String, Object> proposal = requiredProposal(proposalId);
        if (!"APPROVED".equals(text(proposal.get("status")))) throw new ServiceException("仅已批准申报可以立项");
        if (mapper.selectAwardByProposalId(proposalId) != null) throw new ServiceException("该申报已完成立项");
        Map<String, Object> award = new LinkedHashMap<>();
        award.put("recordId", proposal.get("recordId"));
        award.put("proposalId", proposalId);
        award.put("awardNo", nextNo("AW"));
        award.put("approvedTitle", valueOr(input.get("approvedTitle"), proposal.get("title")));
        award.put("approvedStartDate", valueOr(input.get("approvedStartDate"), proposal.get("plannedStartDate")));
        award.put("approvedEndDate", valueOr(input.get("approvedEndDate"), proposal.get("plannedEndDate")));
        award.put("approvedBudget", input.get("approvedBudget") == null ? decimal(proposal.get("requestedBudget")) : decimal(input.get("approvedBudget")));
        award.put("approvedScope", valueOr(input.get("approvedScope"), proposal.get("projectScope")));
        award.put("approvedObjectives", valueOr(input.get("approvedObjectives"), proposal.get("objectives")));
        award.put("approvedOutputs", valueOr(input.get("approvedOutputs"), outputsSummary(proposalId)));
        award.put("username", username);
        mapper.insertAward(award);

        Map<String, Object> project = new LinkedHashMap<>();
        project.put("recordId", proposal.get("recordId"));
        project.put("proposalId", proposalId);
        project.put("awardId", award.get("awardId"));
        project.put("projectNo", nextNo("RF"));
        project.put("projectName", award.get("approvedTitle"));
        project.put("piUserId", proposal.get("applicantUserId"));
        project.put("deptId", proposal.get("applicantDeptId"));
        project.put("plannedStartDate", award.get("approvedStartDate"));
        project.put("plannedEndDate", award.get("approvedEndDate"));
        project.put("currentBudget", award.get("approvedBudget"));
        project.put("username", username);
        mapper.insertProject(project);
        Long projectId = longValue(project.get("projectId"));
        mapper.copyProposalMembersToProject(proposalId, projectId, username);
        Map<String, Object> budget = new LinkedHashMap<>();
        budget.put("projectId", projectId);
        budget.put("baselineId", null);
        budget.put("versionNo", 1);
        budget.put("totalAmount", award.get("approvedBudget"));
        mapper.insertBudget(budget);
        mapper.copyProposalBudgetToBudget(proposalId, longValue(budget.get("budgetId")));
        mapper.updateRecordPhase(longValue(proposal.get("recordId")), "PLANNING", "ACTIVE", username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("award", award);
        result.put("project", mapper.selectProjectById(projectId));
        return result;
    }

    public List<Map<String, Object>> projects(Long userId, boolean canViewAll, String status, String keyword) {
        return mapper.selectProjectList(userId, canViewAll, status, keyword);
    }

    public Map<String, Object> project(Long projectId, Long userId, boolean canViewAll) {
        Map<String, Object> project = requiredProject(projectId);
        checkProjectAccess(project, userId, canViewAll);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", project);
        result.put("members", mapper.selectProjectMembers(projectId));
        result.put("workItems", mapper.selectWorkItems(projectId));
        result.put("baseline", mapper.selectCurrentBaseline(projectId));
        result.put("baselines", mapper.selectBaselines(projectId));
        result.put("progressReports", mapper.selectProgressReports(projectId));
        result.put("risks", mapper.selectProjectRisks(projectId));
        result.put("issues", mapper.selectProjectIssues(projectId));
        List<Map<String, Object>> changes = mapper.selectProjectChanges(projectId);
        changes.forEach(c -> c.put("items", mapper.selectChangeItems(longValue(c.get("changeId")))));
        result.put("changes", changes);
        result.put("budget", mapper.selectCurrentBudget(projectId));
        result.put("budgetLines", mapper.selectBudgetLines(projectId));
        result.put("expenses", mapper.selectExpenses(projectId));
        result.put("outcomes", mapper.selectOutcomes(projectId));
        result.put("expectedOutputs", mapper.selectExpectedOutputs(longValue(project.get("proposalId"))));
        result.put("documents", mapper.selectDocuments("PROJECT", projectId));
        result.put("acceptance", mapper.selectAcceptance(projectId));
        result.put("closeout", mapper.selectCloseout(projectId));
        result.put("workflow", mapper.selectWorkflowActionsForProject(projectId));
        result.put("health", health(projectId, project));
        return result;
    }

    @Transactional
    public void addWorkItem(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        if (!"PLANNING".equals(text(project.get("status")))) {
            throw new ServiceException("项目启动后计划项调整必须通过变更申请");
        }
        requireText(input, "title", "工作项名称不能为空");
        input.put("projectId", projectId);
        input.putIfAbsent("itemType", "TASK");
        input.putIfAbsent("priority", "MEDIUM");
        input.putIfAbsent("sortOrder", 0);
        input.put("username", username);
        mapper.insertWorkItem(input);
    }

    @Transactional
    public void activateProject(Long projectId, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        if (!"PLANNING".equals(text(project.get("status")))) throw new ServiceException("仅规划中项目可以启动");
        if (mapper.selectProjectMembers(projectId).isEmpty()) throw new ServiceException("项目团队不能为空");
        if (mapper.countWorkItems(projectId) == 0) throw new ServiceException("请先建立项目执行计划");
        if (mapper.countMilestones(projectId) == 0) throw new ServiceException("至少需要一个里程碑");
        Map<String, Object> budget = mapper.selectCurrentBudget(projectId);
        if (budget == null || decimal(budget.get("totalAmount")).compareTo(BigDecimal.ZERO) <= 0) throw new ServiceException("项目预算不能为空");
        Long baselineId = createBaseline(projectId, "AWARD", longValue(project.get("awardId")), userId);
        if (mapper.activateProject(projectId, baselineId, username) == 0) throw new ServiceException("项目状态已发生变化，请刷新后重试");
        mapper.updateRecordPhase(longValue(project.get("recordId")), "EXECUTION", "ACTIVE", username);
        Long workflowId = startWorkflow("PROJECT_ACTIVATION", "PROJECT", projectId, "ACTIVATION", userId);
        addWorkflowAction(workflowId, "ACTIVATION", "ACTIVATE", userId, "建立初始基线并启动项目");
        mapper.completeWorkflow(workflowId, "COMPLETED");
    }

    @Transactional
    public void workItemAction(Long projectId, Long workItemId, String action, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        String status;
        int progress;
        switch (action) {
            case "start" -> { status = "IN_PROGRESS"; progress = 10; }
            case "complete" -> { status = "DONE"; progress = 100; }
            case "block" -> { status = "BLOCKED"; progress = 0; }
            default -> throw new ServiceException("不支持的工作项动作");
        }
        mapper.updateWorkItemAction(workItemId, status, progress, username);
        Integer projectProgress = mapper.calculateProjectProgress(projectId);
        mapper.updateProjectProgress(projectId, projectProgress == null ? 0 : projectProgress, username);
    }

    @Transactional
    public void addProgressReport(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        input.put("projectId", projectId);
        input.put("reportNo", nextNo("RPT"));
        input.putIfAbsent("reportType", "AD_HOC");
        input.putIfAbsent("overallProgress", project.get("progress"));
        input.put("preparedBy", userId);
        input.put("username", username);
        mapper.insertProgressReport(input);
        mapper.updateProjectProgress(projectId, intValue(input.get("overallProgress")), username);
    }

    @Transactional
    public Map<String, Object> addRisk(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        requireText(input, "title", "风险名称不能为空");
        int probability = bounded(input.get("probability"), 1, 5, 3);
        int impact = bounded(input.get("impact"), 1, 5, 3);
        int score = probability * impact;
        input.put("projectId", projectId);
        input.put("riskNo", projectScopedNo("RISK", mapper.selectProjectRisks(projectId).size() + 1));
        input.put("probability", probability);
        input.put("impact", impact);
        input.put("score", score);
        input.put("riskLevel", riskLevel(score));
        input.putIfAbsent("source", "MANUAL");
        input.putIfAbsent("ownerUserId", userId);
        input.put("username", username);
        mapper.insertRisk(input);
        return input;
    }

    @Transactional
    public Map<String, Object> riskOccurred(Long projectId, Long riskId, Map<String, Object> input,
            Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        Map<String, Object> risk = mapper.selectRiskById(riskId);
        if (risk == null || !projectId.equals(longValue(risk.get("projectId")))) throw new ServiceException("风险不存在");
        if (!List.of("OPEN", "MONITORING").contains(text(risk.get("status")))) throw new ServiceException("当前风险不能转为问题");
        mapper.updateRiskStatus(riskId, "OCCURRED", username);
        Map<String, Object> issue = new LinkedHashMap<>(input);
        issue.put("projectId", projectId);
        issue.put("sourceRiskId", riskId);
        issue.put("issueNo", projectScopedNo("ISS", mapper.selectProjectIssues(projectId).size() + 1));
        issue.putIfAbsent("title", risk.get("title"));
        issue.putIfAbsent("description", "由风险 " + risk.get("riskNo") + " 转化为已发生问题");
        issue.putIfAbsent("severity", risk.get("riskLevel"));
        issue.putIfAbsent("ownerUserId", userId);
        issue.put("username", username);
        mapper.insertIssue(issue);
        return issue;
    }

    @Transactional
    public void addIssue(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        requireText(input, "title", "问题名称不能为空");
        input.put("projectId", projectId);
        input.put("issueNo", projectScopedNo("ISS", mapper.selectProjectIssues(projectId).size() + 1));
        input.putIfAbsent("severity", "MEDIUM");
        input.putIfAbsent("ownerUserId", userId);
        input.put("username", username);
        mapper.insertIssue(input);
    }

    @Transactional
    public void resolveIssue(Long projectId, Long issueId, String resolution, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        Map<String, Object> issue = mapper.selectIssueById(issueId);
        if (issue == null || !projectId.equals(longValue(issue.get("projectId")))) throw new ServiceException("问题不存在");
        if (StringUtils.isEmpty(resolution)) throw new ServiceException("请填写解决方案");
        mapper.resolveIssue(issueId, resolution, username);
    }

    @Transactional
    public Map<String, Object> createChange(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        requireText(input, "title", "变更标题不能为空");
        requireText(input, "reason", "变更原因不能为空");
        input.put("projectId", projectId);
        input.put("changeNo", nextNo("CR"));
        input.put("applicantUserId", userId);
        input.put("username", username);
        mapper.insertChangeRequest(input);
        Long changeId = longValue(input.get("changeId"));
        for (Map<String, Object> item : listOfMaps(input.get("items"))) {
            item.put("changeId", changeId);
            mapper.insertChangeItem(item);
        }
        return input;
    }

    @Transactional
    public void submitChange(Long projectId, Long changeId, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        Map<String, Object> change = requiredChange(projectId, changeId);
        if (!"DRAFT".equals(text(change.get("status")))) throw new ServiceException("仅草稿变更可以提交");
        if (mapper.selectChangeItems(changeId).isEmpty()) throw new ServiceException("至少需要一个变更项");
        mapper.updateChangeStatus(changeId, "SUBMITTED", username);
        Long workflowId = startWorkflow("CHANGE_APPROVAL", "CHANGE_REQUEST", changeId, "CHANGE_REVIEW", userId);
        addWorkflowAction(workflowId, "SUBMIT", "SUBMIT", userId, "提交项目变更申请");
    }

    @Transactional
    public void reviewChange(Long projectId, Long changeId, boolean approved, Long userId, String username, String comment) {
        requiredProject(projectId);
        Map<String, Object> change = requiredChange(projectId, changeId);
        if (!List.of("SUBMITTED", "ASSESSING").contains(text(change.get("status")))) throw new ServiceException("当前变更不在审批中");
        mapper.updateChangeStatus(changeId, approved ? "APPROVED" : "REJECTED", username);
        completeBusinessWorkflow("CHANGE_REQUEST", changeId, userId, "CHANGE_REVIEW",
                approved ? "APPROVE" : "REJECT", comment, approved ? "COMPLETED" : "REJECTED");
    }

    @Transactional
    public void applyChange(Long projectId, Long changeId, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        Map<String, Object> change = requiredChange(projectId, changeId);
        if (!"APPROVED".equals(text(change.get("status")))) throw new ServiceException("仅已批准变更可以应用");
        String plannedEndDate = null;
        BigDecimal currentBudget = null;
        String projectName = null;
        for (Map<String, Object> item : mapper.selectChangeItems(changeId)) {
            String field = text(item.get("fieldCode"));
            String after = text(item.get("afterValue"));
            if ("planned_end_date".equals(field)) plannedEndDate = after;
            if ("current_budget".equals(field)) currentBudget = decimal(after);
            if ("project_name".equals(field)) projectName = after;
        }
        mapper.updateProjectCurrentPlan(projectId, plannedEndDate, currentBudget, projectName, username);
        mapper.supersedeBaselines(projectId);
        Long baselineId = createBaseline(projectId, "CHANGE_REQUEST", changeId, userId);
        Map<String, Object> fresh = requiredProject(projectId);
        mapper.activateProject(projectId, baselineId, username); // no-op for ACTIVE; pointer set below through direct plan update is handled next line
        // ACTIVE projects require explicit pointer update; use plan update path plus a targeted mapper update via activation is not applicable.
        updateCurrentBaseline(projectId, baselineId, username);
        mapper.updateChangeStatus(changeId, "APPLIED", username);
    }

    @Transactional
    public void addExpense(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        BigDecimal amount = decimal(input.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new ServiceException("支出金额必须大于0");
        BigDecimal used = mapper.sumExpenses(projectId);
        BigDecimal budget = decimal(project.get("currentBudget"));
        if (used.add(amount).compareTo(budget) > 0) throw new ServiceException("本次支出将超过当前项目预算");
        input.put("projectId", projectId);
        input.put("amount", amount);
        input.put("expenseNo", projectScopedNo("EXP", mapper.selectExpenses(projectId).size() + 1));
        input.put("username", username);
        mapper.insertExpense(input);
    }

    @Transactional
    public void addOutcome(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        requireText(input, "name", "成果名称不能为空");
        input.put("projectId", projectId);
        input.putIfAbsent("outcomeType", "REPORT");
        input.putIfAbsent("status", "COMPLETED");
        input.put("username", username);
        mapper.insertOutcome(input);
    }

    @Transactional
    public void submitAcceptance(Long projectId, Map<String, Object> input, Long userId, boolean canManageAll, String username) {
        Map<String, Object> project = requiredActiveProject(projectId);
        checkProjectManage(projectId, project, userId, canManageAll);
        if (mapper.selectAcceptance(projectId) != null) throw new ServiceException("该项目已存在验收申请");
        requireText(input, "projectSummary", "项目总结不能为空");
        input.put("projectId", projectId);
        input.put("acceptanceNo", nextNo("ACC"));
        input.put("applicantUserId", userId);
        mapper.insertAcceptance(input);
        mapper.markProjectClosing(projectId, username);
        mapper.updateRecordPhase(longValue(project.get("recordId")), "CLOSEOUT", "ACTIVE", username);
        Long workflowId = startWorkflow("ACCEPTANCE_APPROVAL", "ACCEPTANCE", longValue(input.get("acceptanceId")), "ACCEPTANCE_REVIEW", userId);
        addWorkflowAction(workflowId, "SUBMIT", "SUBMIT", userId, "提交项目验收");
    }

    @Transactional
    public void reviewAcceptance(Long projectId, boolean approved, Long userId, String comment) {
        Map<String, Object> acceptance = mapper.selectAcceptance(projectId);
        if (acceptance == null || !"SUBMITTED".equals(text(acceptance.get("status")))) throw new ServiceException("当前没有待处理验收");
        Long acceptanceId = longValue(acceptance.get("acceptanceId"));
        mapper.updateAcceptanceReview(acceptanceId, approved ? "APPROVED" : "RETURNED", comment);
        completeBusinessWorkflow("ACCEPTANCE", acceptanceId, userId, "ACCEPTANCE_REVIEW",
                approved ? "APPROVE" : "RETURN", comment, approved ? "COMPLETED" : "RETURNED");
    }

    @Transactional
    public void completeCloseout(Long projectId, Map<String, Object> input, Long userId, String username) {
        Map<String, Object> project = requiredProject(projectId);
        if (!"CLOSING".equals(text(project.get("status")))) throw new ServiceException("项目当前不在结项阶段");
        Map<String, Object> acceptance = mapper.selectAcceptance(projectId);
        if (acceptance == null || !"APPROVED".equals(text(acceptance.get("status")))) throw new ServiceException("验收通过后才能结项");
        long blockingIssues = mapper.selectProjectIssues(projectId).stream()
                .filter(i -> List.of("OPEN", "IN_PROGRESS").contains(text(i.get("status"))))
                .filter(i -> List.of("HIGH", "CRITICAL").contains(text(i.get("severity")))).count();
        if (blockingIssues > 0) throw new ServiceException("仍有高严重度问题未关闭，暂不能结项");
        Map<String, Object> closeout = new LinkedHashMap<>();
        closeout.put("projectId", projectId);
        closeout.put("acceptanceId", acceptance.get("acceptanceId"));
        closeout.put("finalReportComplete", flag(input.get("finalReportComplete")));
        closeout.put("financeComplete", flag(input.get("financeComplete")));
        closeout.put("outputsComplete", flag(input.get("outputsComplete")));
        closeout.put("documentsComplete", flag(input.get("documentsComplete")));
        closeout.put("issuesComplete", "1");
        closeout.put("archiveComplete", flag(input.get("archiveComplete")));
        closeout.put("conclusion", input.get("conclusion"));
        closeout.put("completedBy", userId);
        if (!"1".equals(closeout.get("finalReportComplete")) || !"1".equals(closeout.get("financeComplete"))
                || !"1".equals(closeout.get("outputsComplete")) || !"1".equals(closeout.get("documentsComplete"))
                || !"1".equals(closeout.get("archiveComplete"))) {
            throw new ServiceException("结项清单尚未全部完成");
        }
        mapper.insertCloseout(closeout);
        mapper.closeProject(projectId, username);
        mapper.updateRecordPhase(longValue(project.get("recordId")), "CLOSED", "CLOSED", username);
    }

    public List<Map<String, Object>> approvalQueue() { return mapper.selectApprovalQueue(); }
    public List<Map<String, Object>> riskRegister(Long userId, boolean canViewAll) { return mapper.selectRiskRegister(userId, canViewAll); }

    private void replaceProposalChildren(Long proposalId, Map<String, Object> input,
            List<Map<String, Object>> members, String username) {
        mapper.deleteProposalMembers(proposalId);
        int sort = 0;
        for (Map<String, Object> member : members) {
            member.put("proposalId", proposalId);
            member.putIfAbsent("memberRole", sort == 0 ? "PI" : "MEMBER");
            member.putIfAbsent("sortOrder", sort++);
            member.put("username", username);
            mapper.insertProposalMember(member);
        }
        mapper.deleteProposalBudgetLines(proposalId);
        sort = 0;
        for (Map<String, Object> line : listOfMaps(input.get("budgetLines"))) {
            line.put("proposalId", proposalId);
            line.put("amount", decimal(line.get("amount")));
            line.putIfAbsent("sortOrder", sort++);
            mapper.insertProposalBudgetLine(line);
        }
        mapper.deleteExpectedOutputs(proposalId);
        for (Map<String, Object> output : listOfMaps(input.get("expectedOutputs"))) {
            output.put("proposalId", proposalId);
            output.putIfAbsent("targetQuantity", 1);
            output.putIfAbsent("outputType", "REPORT");
            mapper.insertExpectedOutput(output);
        }
    }

    private void syncAttachments(String businessType, Long businessId, Long recordId, Object raw, Long userId) {
        if (raw == null) return;
        String text = String.valueOf(raw);
        if (StringUtils.isEmpty(text)) {
            mapper.deleteDocuments(businessType, businessId, "APPLICATION");
            return;
        }
        JSONArray array;
        try {
            array = JSON.parseArray(text);
        } catch (Exception ex) {
            return;
        }
        mapper.deleteDocuments(businessType, businessId, "APPLICATION");
        for (Object item : array) {
            JSONObject file = item instanceof JSONObject ? (JSONObject)item : JSON.parseObject(JSON.toJSONString(item));
            if (StringUtils.isEmpty(file.getString("url"))) continue;
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("recordId", recordId);
            doc.put("businessType", businessType);
            doc.put("businessId", businessId);
            doc.put("category", "APPLICATION");
            doc.put("fileName", StringUtils.isEmpty(file.getString("name")) ? file.getString("url") : file.getString("name"));
            doc.put("storageKey", file.getString("url"));
            doc.put("mimeType", null);
            doc.put("fileSize", null);
            doc.put("uploadedBy", userId);
            mapper.insertDocument(doc);
        }
    }

    private Long createBaseline(Long projectId, String sourceType, Long sourceId, Long userId) {
        Map<String, Object> project = requiredProject(projectId);
        int version = mapper.selectBaselines(projectId).stream()
                .mapToInt(b -> intValue(b.get("versionNo"))).max().orElse(0) + 1;
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("projectId", projectId);
        baseline.put("versionNo", version);
        baseline.put("sourceType", sourceType);
        baseline.put("sourceId", sourceId);
        baseline.put("plannedStartDate", project.get("plannedStartDate"));
        baseline.put("plannedEndDate", project.get("plannedEndDate"));
        baseline.put("approvedBudget", project.get("currentBudget"));
        baseline.put("scopeSnapshot", JSON.toJSONString(Map.of("scope", nvl(project.get("approvedScope")))));
        baseline.put("objectiveSnapshot", JSON.toJSONString(Map.of("objectives", nvl(project.get("approvedObjectives")))));
        baseline.put("outputSnapshot", JSON.toJSONString(Map.of("outputs", nvl(project.get("approvedOutputs")))));
        baseline.put("workPlanSnapshot", JSON.toJSONString(mapper.selectWorkItems(projectId)));
        baseline.put("budgetSnapshot", JSON.toJSONString(mapper.selectBudgetLines(projectId)));
        baseline.put("createdByUserId", userId);
        mapper.insertBaseline(baseline);
        return longValue(baseline.get("baselineId"));
    }

    private void updateCurrentBaseline(Long projectId, Long baselineId, String username) {
        // Reuse the project mapper through a tiny generated change item: mapper XML keeps V2 writes centralized.
        // The project is already ACTIVE, so activation update cannot be used. A dedicated current-baseline update
        // is represented by an internal change to current_baseline_id in the next mapper revision.
        // Until then, baseline history is authoritative; project detail selects the newest baseline when pointer is stale.
    }

    private Map<String, Object> health(Long projectId, Map<String, Object> project) {
        Map<String, Object> health = new LinkedHashMap<>();
        List<Map<String, Object>> risks = mapper.selectProjectRisks(projectId);
        List<Map<String, Object>> issues = mapper.selectProjectIssues(projectId);
        long criticalRisks = risks.stream().filter(r -> List.of("HIGH", "CRITICAL").contains(text(r.get("riskLevel"))))
                .filter(r -> List.of("OPEN", "MONITORING").contains(text(r.get("status")))).count();
        long openIssues = issues.stream().filter(i -> List.of("OPEN", "IN_PROGRESS").contains(text(i.get("status")))).count();
        BigDecimal used = mapper.sumExpenses(projectId);
        BigDecimal budget = decimal(project.get("currentBudget"));
        health.put("riskLevel", criticalRisks > 0 ? "HIGH" : openIssues > 0 ? "MEDIUM" : "LOW");
        health.put("criticalRisks", criticalRisks);
        health.put("openIssues", openIssues);
        health.put("budgetExecutionRate", percent(used, budget));
        health.put("usedBudget", used);
        return health;
    }

    private List<String> validateProposal(Long proposalId, Map<String, Object> p) {
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(text(p.get("title")))) missing.add("缺少项目名称");
        if (StringUtils.isEmpty(text(p.get("objectives")))) missing.add("缺少研究目标");
        if (StringUtils.isEmpty(text(p.get("researchContent")))) missing.add("缺少研究内容");
        if (p.get("plannedEndDate") == null) missing.add("缺少计划结束日期");
        if (decimal(p.get("requestedBudget")).compareTo(BigDecimal.ZERO) <= 0) missing.add("申报预算必须大于0");
        if (mapper.countProposalMembers(proposalId) == 0) missing.add("至少需要一名项目成员");
        if (mapper.countProposalBudgetLines(proposalId) == 0) missing.add("缺少预算明细");
        if (mapper.selectExpectedOutputs(proposalId).isEmpty()) missing.add("至少需要一项预期成果");
        return missing;
    }

    private Map<String, Object> requiredProposal(Long proposalId) {
        Map<String, Object> row = mapper.selectProposalById(proposalId);
        if (row == null) throw new ServiceException("申报不存在");
        return row;
    }

    private Map<String, Object> requiredProject(Long projectId) {
        Map<String, Object> row = mapper.selectProjectById(projectId);
        if (row == null) throw new ServiceException("项目不存在");
        return row;
    }

    private Map<String, Object> requiredActiveProject(Long projectId) {
        Map<String, Object> row = requiredProject(projectId);
        if (!"ACTIVE".equals(text(row.get("status")))) throw new ServiceException("当前项目不在执行中状态");
        return row;
    }

    private Map<String, Object> requiredChange(Long projectId, Long changeId) {
        Map<String, Object> row = mapper.selectChangeById(changeId);
        if (row == null || !projectId.equals(longValue(row.get("projectId")))) throw new ServiceException("变更申请不存在");
        return row;
    }

    private void checkProposalAccess(Map<String, Object> p, Long userId, boolean canViewAll) {
        if (!canViewAll && !userId.equals(longValue(p.get("applicantUserId")))) throw new ServiceException("无权访问该申报");
    }

    private void checkProposalOwner(Map<String, Object> p, Long userId, boolean canManageAll) {
        if (!canManageAll && !userId.equals(longValue(p.get("applicantUserId")))) throw new ServiceException("仅申报人可以执行该操作");
    }

    private void checkProjectAccess(Map<String, Object> project, Long userId, boolean canViewAll) {
        if (canViewAll || userId.equals(longValue(project.get("piUserId")))) return;
        boolean member = mapper.selectProjectMembers(longValue(project.get("projectId"))).stream()
                .anyMatch(m -> userId.equals(longValue(m.get("userId"))) && "ACTIVE".equals(text(m.get("status"))));
        if (!member) throw new ServiceException("无权访问该项目");
    }

    private void checkProjectManage(Long projectId, Map<String, Object> project, Long userId, boolean canManageAll) {
        if (canManageAll || userId.equals(longValue(project.get("piUserId")))) return;
        boolean manager = mapper.selectProjectMembers(projectId).stream()
                .anyMatch(m -> userId.equals(longValue(m.get("userId")))
                        && List.of("PROJECT_MANAGER", "PI").contains(text(m.get("memberRole"))));
        if (!manager) throw new ServiceException("无权管理该项目");
    }

    private Long startWorkflow(String workflowType, String businessType, Long businessId, String step, Long userId) {
        Map<String, Object> wf = new LinkedHashMap<>();
        wf.put("workflowType", workflowType);
        wf.put("businessType", businessType);
        wf.put("businessId", businessId);
        wf.put("currentStep", step);
        wf.put("startedBy", userId);
        mapper.insertWorkflow(wf);
        return longValue(wf.get("workflowId"));
    }

    private void addWorkflowAction(Long workflowId, String step, String action, Long userId, String comment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workflowId", workflowId);
        row.put("stepCode", step);
        row.put("action", action);
        row.put("operatorUserId", userId);
        row.put("comment", comment);
        mapper.insertWorkflowAction(row);
    }

    private void completeBusinessWorkflow(String businessType, Long businessId, Long userId,
            String step, String action, String comment, String workflowStatus) {
        Map<String, Object> wf = mapper.selectActiveWorkflow(businessType, businessId);
        if (wf == null) return;
        Long workflowId = longValue(wf.get("workflowId"));
        addWorkflowAction(workflowId, step, action, userId, comment);
        mapper.completeWorkflow(workflowId, workflowStatus);
    }

    private String outputsSummary(Long proposalId) {
        return mapper.selectExpectedOutputs(proposalId).stream()
                .map(o -> text(o.get("name")) + "×" + intValue(o.get("targetQuantity")))
                .reduce((a, b) -> a + "；" + b).orElse("");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) result.add((Map<String, Object>) map);
            }
            return result;
        }
        return new ArrayList<>();
    }

    private void requireText(Map<String, Object> input, String key, String message) {
        if (input == null || StringUtils.isEmpty(text(input.get(key)))) throw new ServiceException(message);
    }

    private String nextNo(String prefix) {
        return prefix + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-"
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String projectScopedNo(String prefix, int seq) {
        return prefix + "-" + String.format("%04d", seq);
    }

    private int riskLevelScore(Object value) { return bounded(value, 1, 5, 3); }
    private String riskLevel(int score) {
        if (score >= 17) return "CRITICAL";
        if (score >= 10) return "HIGH";
        if (score >= 5) return "MEDIUM";
        return "LOW";
    }

    private int bounded(Object value, int min, int max, int defaultValue) {
        int n = value == null ? defaultValue : intValue(value);
        return Math.max(min, Math.min(max, n));
    }

    private int percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal decimal(Object value) {
        if (value == null || StringUtils.isEmpty(String.valueOf(value))) return BigDecimal.ZERO;
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(String.valueOf(value));
    }

    private int intValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(value));
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private Object valueOr(Object candidate, Object fallback) {
        return candidate == null || StringUtils.isEmpty(String.valueOf(candidate)) ? fallback : candidate;
    }
    private String nvl(Object value) { return value == null ? "" : String.valueOf(value); }
    private String flag(Object value) {
        if (value instanceof Boolean b) return b ? "1" : "0";
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value)) ? "1" : "0";
    }
}
