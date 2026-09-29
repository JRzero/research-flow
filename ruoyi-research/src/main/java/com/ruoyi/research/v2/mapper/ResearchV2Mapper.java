package com.ruoyi.research.v2.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface ResearchV2Mapper {
    List<Map<String, Object>> selectProposalList(@Param("ownerUserId") Long ownerUserId,
            @Param("status") String status, @Param("keyword") String keyword);
    Map<String, Object> selectProposalById(Long proposalId);
    List<Map<String, Object>> selectProposalMembers(Long proposalId);
    List<Map<String, Object>> selectProposalBudgetLines(Long proposalId);
    List<Map<String, Object>> selectExpectedOutputs(Long proposalId);
    int countProposalMembers(Long proposalId);
    int countProposalBudgetLines(Long proposalId);
    int insertRecord(Map<String, Object> row);
    int insertProposal(Map<String, Object> row);
    int updateProposal(Map<String, Object> row);
    int updateProposalStatus(@Param("proposalId") Long proposalId, @Param("status") String status,
            @Param("username") String username);
    int deleteProposalMembers(Long proposalId);
    int insertProposalMember(Map<String, Object> row);
    int deleteProposalBudgetLines(Long proposalId);
    int insertProposalBudgetLine(Map<String, Object> row);
    int deleteExpectedOutputs(Long proposalId);
    int insertExpectedOutput(Map<String, Object> row);
    int insertReview(Map<String, Object> row);
    int completeReview(@Param("proposalId") Long proposalId, @Param("reviewerId") Long reviewerId,
            @Param("decision") String decision, @Param("comment") String comment);
    Map<String, Object> selectAwardByProposalId(Long proposalId);
    int insertAward(Map<String, Object> row);
    int insertProject(Map<String, Object> row);
    int copyProposalMembersToProject(@Param("proposalId") Long proposalId, @Param("projectId") Long projectId,
            @Param("username") String username);
    int insertBudget(Map<String, Object> row);
    int copyProposalBudgetToBudget(@Param("proposalId") Long proposalId, @Param("budgetId") Long budgetId);
    List<Map<String, Object>> selectProjectList(@Param("userId") Long userId,
            @Param("canViewAll") boolean canViewAll, @Param("status") String status, @Param("keyword") String keyword);
    Map<String, Object> selectProjectById(Long projectId);
    List<Map<String, Object>> selectProjectMembers(Long projectId);
    List<Map<String, Object>> selectWorkItems(Long projectId);
    List<Map<String, Object>> selectBaselines(Long projectId);
    Map<String, Object> selectCurrentBaseline(Long projectId);
    List<Map<String, Object>> selectProgressReports(Long projectId);
    List<Map<String, Object>> selectProjectRisks(Long projectId);
    List<Map<String, Object>> selectProjectIssues(Long projectId);
    List<Map<String, Object>> selectProjectChanges(Long projectId);
    List<Map<String, Object>> selectChangeItems(Long changeId);
    Map<String, Object> selectCurrentBudget(Long projectId);
    List<Map<String, Object>> selectBudgetLines(Long projectId);
    List<Map<String, Object>> selectExpenses(Long projectId);
    BigDecimal sumExpenses(Long projectId);
    List<Map<String, Object>> selectOutcomes(Long projectId);
    Map<String, Object> selectAcceptance(Long projectId);
    Map<String, Object> selectCloseout(Long projectId);
    List<Map<String, Object>> selectDocuments(@Param("businessType") String businessType,
            @Param("businessId") Long businessId);
    List<Map<String, Object>> selectWorkflowActionsForProject(Long projectId);
    int insertWorkItem(Map<String, Object> row);
    int updateWorkItemAction(@Param("workItemId") Long workItemId, @Param("status") String status,
            @Param("progress") Integer progress, @Param("username") String username);
    int countWorkItems(Long projectId);
    int countMilestones(Long projectId);
    int updateProjectProgress(@Param("projectId") Long projectId, @Param("progress") Integer progress,
            @Param("username") String username);
    Integer calculateProjectProgress(Long projectId);
    int insertBaseline(Map<String, Object> row);
    int supersedeBaselines(Long projectId);
    int activateProject(@Param("projectId") Long projectId, @Param("baselineId") Long baselineId,
            @Param("username") String username);
    int insertProgressReport(Map<String, Object> row);
    int insertRisk(Map<String, Object> row);
    Map<String, Object> selectRiskById(Long riskId);
    int updateRiskStatus(@Param("riskId") Long riskId, @Param("status") String status,
            @Param("username") String username);
    int insertIssue(Map<String, Object> row);
    Map<String, Object> selectIssueById(Long issueId);
    int resolveIssue(@Param("issueId") Long issueId, @Param("resolution") String resolution,
            @Param("username") String username);
    int insertDecision(Map<String, Object> row);
    int insertChangeRequest(Map<String, Object> row);
    Map<String, Object> selectChangeById(Long changeId);
    int insertChangeItem(Map<String, Object> row);
    int updateChangeStatus(@Param("changeId") Long changeId, @Param("status") String status,
            @Param("username") String username);
    int updateProjectCurrentPlan(@Param("projectId") Long projectId,
            @Param("plannedEndDate") String plannedEndDate, @Param("currentBudget") BigDecimal currentBudget,
            @Param("projectName") String projectName, @Param("username") String username);
    int insertExpense(Map<String, Object> row);
    int insertOutcome(Map<String, Object> row);
    int deleteDocuments(@Param("businessType") String businessType, @Param("businessId") Long businessId,
            @Param("category") String category);
    int insertDocument(Map<String, Object> row);
    int insertAcceptance(Map<String, Object> row);
    int updateAcceptanceReview(@Param("acceptanceId") Long acceptanceId, @Param("status") String status,
            @Param("comment") String comment);
    int markProjectClosing(@Param("projectId") Long projectId, @Param("username") String username);
    int insertCloseout(Map<String, Object> row);
    int closeProject(@Param("projectId") Long projectId, @Param("username") String username);
    int updateRecordPhase(@Param("recordId") Long recordId, @Param("phase") String phase,
            @Param("status") String status, @Param("username") String username);
    int insertWorkflow(Map<String, Object> row);
    Map<String, Object> selectActiveWorkflow(@Param("businessType") String businessType,
            @Param("businessId") Long businessId);
    int insertWorkflowAction(Map<String, Object> row);
    int completeWorkflow(@Param("workflowId") Long workflowId, @Param("status") String status);
    List<Map<String, Object>> selectApprovalQueue();
    List<Map<String, Object>> selectRiskRegister(@Param("userId") Long userId, @Param("canViewAll") boolean canViewAll);
    Map<String, Object> selectDashboard(@Param("userId") Long userId, @Param("canViewAll") boolean canViewAll);
    List<Map<String, Object>> selectProjectStatusStats(@Param("userId") Long userId, @Param("canViewAll") boolean canViewAll);
    List<Map<String, Object>> selectProposalStatusStats(@Param("userId") Long userId, @Param("canViewAll") boolean canViewAll);
    Map<String, Object> selectPortfolioFinance(@Param("userId") Long userId, @Param("canViewAll") boolean canViewAll);
}
