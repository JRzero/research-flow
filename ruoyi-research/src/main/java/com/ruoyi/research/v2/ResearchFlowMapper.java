package com.ruoyi.research.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface ResearchFlowMapper {
    int insertRecord(Map<String,Object> data);
    int updateRecordPhase(@Param("recordId") Long recordId,@Param("phase") String phase,@Param("status") String status);
    Map<String,Object> selectRecord(Long recordId);

    int insertProposal(Map<String,Object> data);
    int updateProposal(Map<String,Object> data);
    int updateProposalStatus(@Param("proposalId") Long proposalId,@Param("status") String status);
    Map<String,Object> selectProposal(Long proposalId);
    List<Map<String,Object>> selectProposals(@Param("userId") Long userId,@Param("viewAll") boolean viewAll,@Param("status") String status,@Param("keyword") String keyword);
    int countProposalMembers(Long proposalId);
    List<Map<String,Object>> selectProposalMembers(Long proposalId);
    int insertProposalMember(Map<String,Object> data);
    int deleteProposalBudget(Long proposalId);
    int insertProposalBudgetLine(Map<String,Object> data);
    List<Map<String,Object>> selectProposalBudget(Long proposalId);
    int insertExpectedOutput(Map<String,Object> data);
    List<Map<String,Object>> selectExpectedOutputs(Long proposalId);

    int insertReview(Map<String,Object> data);
    int completeReview(@Param("proposalId") Long proposalId,@Param("decision") String decision,@Param("comment") String comment,@Param("reviewerId") Long reviewerId);
    List<Map<String,Object>> selectReviews(Long proposalId);

    int insertAward(Map<String,Object> data);
    Map<String,Object> selectAwardByProposal(Long proposalId);
    Map<String,Object> selectAward(Long awardId);

    int insertProject(Map<String,Object> data);
    int updateProjectStatus(@Param("projectId") Long projectId,@Param("status") String status);
    int updateProjectCurrentPlan(@Param("projectId") Long projectId,@Param("plannedEndDate") String plannedEndDate,@Param("currentBudget") BigDecimal currentBudget);
    int updateProjectBaseline(@Param("projectId") Long projectId,@Param("baselineId") Long baselineId,@Param("status") String status);
    int updateProjectProgressFromWorkItems(Long projectId);
    Map<String,Object> selectProject(Long projectId);
    List<Map<String,Object>> selectProjects(@Param("userId") Long userId,@Param("viewAll") boolean viewAll,@Param("status") String status,@Param("keyword") String keyword);
    int insertProjectMember(Map<String,Object> data);
    int copyProposalMembersToProject(@Param("proposalId") Long proposalId,@Param("projectId") Long projectId,@Param("username") String username);
    List<Map<String,Object>> selectProjectMembers(Long projectId);
    int isProjectMember(@Param("projectId") Long projectId,@Param("userId") Long userId);

    int insertWorkItem(Map<String,Object> data);
    Map<String,Object> selectWorkItem(Long workItemId);
    List<Map<String,Object>> selectWorkItems(Long projectId);
    int updateWorkItemAction(@Param("workItemId") Long workItemId,@Param("status") String status,@Param("progress") Integer progress,@Param("username") String username);
    int countWorkItems(Long projectId);

    int insertProgressReport(Map<String,Object> data);
    List<Map<String,Object>> selectProgressReports(Long projectId);

    int insertRisk(Map<String,Object> data);
    Map<String,Object> selectRisk(Long riskId);
    List<Map<String,Object>> selectProjectRisks(Long projectId);
    List<Map<String,Object>> selectAllRisks(@Param("userId") Long userId,@Param("viewAll") boolean viewAll);
    int updateRiskStatus(@Param("riskId") Long riskId,@Param("status") String status,@Param("username") String username);

    int insertIssue(Map<String,Object> data);
    Map<String,Object> selectIssue(Long issueId);
    List<Map<String,Object>> selectProjectIssues(Long projectId);
    List<Map<String,Object>> selectAllIssues(@Param("userId") Long userId,@Param("viewAll") boolean viewAll);
    int updateIssueStatus(@Param("issueId") Long issueId,@Param("status") String status,@Param("resolution") String resolution,@Param("username") String username);
    int countBlockingIssues(Long projectId);

    int insertDecision(Map<String,Object> data);
    List<Map<String,Object>> selectDecisions(Long projectId);

    int insertChange(Map<String,Object> data);
    int insertChangeItem(Map<String,Object> data);
    Map<String,Object> selectChange(Long changeId);
    List<Map<String,Object>> selectChanges(Long projectId);
    List<Map<String,Object>> selectChangeItems(Long changeId);
    int updateChangeStatus(@Param("changeId") Long changeId,@Param("status") String status,@Param("username") String username);

    int insertBudget(Map<String,Object> data);
    int copyProposalBudgetToProject(@Param("proposalId") Long proposalId,@Param("budgetId") Long budgetId);
    Map<String,Object> selectCurrentBudget(Long projectId);
    List<Map<String,Object>> selectBudgetLines(Long budgetId);
    int insertExpense(Map<String,Object> data);
    List<Map<String,Object>> selectExpenses(Long projectId);
    BigDecimal sumExpenses(Long projectId);

    int insertOutcome(Map<String,Object> data);
    List<Map<String,Object>> selectOutcomes(Long projectId);

    int insertDocument(Map<String,Object> data);
    List<Map<String,Object>> selectDocuments(@Param("businessType") String businessType,@Param("businessId") Long businessId);

    int nextBaselineVersion(Long projectId);
    int insertBaseline(Map<String,Object> data);
    List<Map<String,Object>> selectBaselines(Long projectId);

    int insertAcceptance(Map<String,Object> data);
    Map<String,Object> selectAcceptance(Long projectId);
    int updateAcceptance(@Param("projectId") Long projectId,@Param("status") String status,@Param("comment") String comment);

    int insertCloseout(Map<String,Object> data);
    Map<String,Object> selectCloseout(Long projectId);
    int completeCloseout(@Param("projectId") Long projectId,@Param("conclusion") String conclusion,@Param("userId") Long userId);

    int insertWorkflow(Map<String,Object> data);
    int updateWorkflow(@Param("businessType") String businessType,@Param("businessId") Long businessId,@Param("status") String status,@Param("step") String step);
    Long selectActiveWorkflowId(@Param("businessType") String businessType,@Param("businessId") Long businessId);
    int insertWorkflowAction(Map<String,Object> data);
    List<Map<String,Object>> selectProjectWorkflow(Long projectId);

    List<Map<String,Object>> selectPendingProposalReviews();
    List<Map<String,Object>> selectPendingChanges();
    List<Map<String,Object>> selectPendingAcceptances();
}
