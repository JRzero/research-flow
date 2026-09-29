package com.ruoyi.research.workflow;

public interface WorkflowService {
    void submitProject(Long projectId, Long operatorId, String operatorName);
    void approveProject(Long projectId, Long operatorId, String operatorName, String comment);
    void rejectProject(Long projectId, Long operatorId, String operatorName, String comment);
    void startProject(Long projectId, Long operatorId, String operatorName);
    void submitAcceptance(Long projectId, Long operatorId, String operatorName);
    void reviewAcceptance(Long projectId, Long operatorId, String operatorName, boolean approved, String comment);
}
