package com.ruoyi.research.v2;

import java.util.List;
import java.util.Map;

public interface ResearchFlowService {
    Map<String,Object> dashboard(Long userId, boolean viewAll);
    Map<String,Object> analytics(Long userId, boolean viewAll);

    List<Map<String,Object>> listProposals(Long userId, boolean viewAll, String status, String keyword);
    Map<String,Object> getProposal(Long proposalId, Long userId, boolean viewAll);
    Map<String,Object> createProposal(Map<String,Object> input, Long userId, Long deptId, String username);
    void updateProposal(Long proposalId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    Map<String,Object> validateProposal(Long proposalId, Long userId, boolean manageAll);
    void submitProposal(Long proposalId, Long userId, boolean manageAll, String username);
    void approveProposal(Long proposalId, Map<String,Object> approval, Long userId, String username);
    void rejectProposal(Long proposalId, String comment, Long userId, String username);
    void addProposalMember(Long proposalId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void saveProposalBudget(Long proposalId, List<Map<String,Object>> lines, Long userId, boolean manageAll);
    void addExpectedOutput(Long proposalId, Map<String,Object> input, Long userId, boolean manageAll);
    void attachProposalDocument(Long proposalId, Map<String,Object> input, Long userId, boolean manageAll);

    List<Map<String,Object>> listProjects(Long userId, boolean viewAll, String status, String keyword);
    Map<String,Object> getProject(Long projectId, Long userId, boolean viewAll);
    void addProjectMember(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void addWorkItem(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void workItemAction(Long projectId, Long workItemId, String action, Long userId, boolean manageAll, String username);
    void activateProject(Long projectId, Long userId, boolean manageAll, String username);
    void addProgressReport(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void addExpense(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void addOutcome(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void attachProjectDocument(Long projectId, Map<String,Object> input, Long userId, boolean manageAll);

    void addRisk(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void updateRiskStatus(Long projectId, Long riskId, String status, Long userId, boolean manageAll, String username);
    Long convertRiskToIssue(Long projectId, Long riskId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void addIssue(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void updateIssueStatus(Long projectId, Long issueId, String status, Map<String,Object> input, Long userId, boolean manageAll, String username);
    Map<String,Object> risksAndIssues(Long userId, boolean viewAll);

    Long createChange(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void submitChange(Long projectId, Long changeId, Long userId, boolean manageAll, String username);
    void approveChange(Long projectId, Long changeId, String comment, Long userId, String username);
    void rejectChange(Long projectId, Long changeId, String comment, Long userId, String username);
    void applyChange(Long projectId, Long changeId, Long userId, boolean manageAll, String username);

    void submitAcceptance(Long projectId, Map<String,Object> input, Long userId, boolean manageAll, String username);
    void reviewAcceptance(Long projectId, boolean approved, String comment, Long userId, String username);
    void completeCloseout(Long projectId, Map<String,Object> input, Long userId, String username);

    Map<String,Object> approvalCenter();
}
