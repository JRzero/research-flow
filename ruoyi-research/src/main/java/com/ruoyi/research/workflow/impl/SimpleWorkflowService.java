package com.ruoyi.research.workflow.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.research.domain.ResearchApproval;
import com.ruoyi.research.domain.ResearchProject;
import com.ruoyi.research.domain.ResearchProjectStatus;
import com.ruoyi.research.mapper.ResearchApprovalMapper;
import com.ruoyi.research.mapper.ResearchProjectMapper;
import com.ruoyi.research.workflow.WorkflowService;

@Service
public class SimpleWorkflowService implements WorkflowService {
    @Autowired
    private ResearchProjectMapper projectMapper;
    @Autowired
    private ResearchApprovalMapper approvalMapper;

    @Override
    @Transactional
    public void submitProject(Long projectId, Long operatorId, String operatorName) {
        ResearchProject project = required(projectId);
        if (!ResearchProjectStatus.DRAFT.equals(project.getStatus()) && !ResearchProjectStatus.REJECTED.equals(project.getStatus())) {
            throw new ServiceException("仅草稿或已驳回项目可以提交审批");
        }
        move(projectId, ResearchProjectStatus.PENDING_APPROVAL, operatorName);
        record(projectId, "PROJECT_APPLICATION", "SUBMIT", operatorId, "提交项目申报");
    }

    @Override
    @Transactional
    public void approveProject(Long projectId, Long operatorId, String operatorName, String comment) {
        ensure(projectId, ResearchProjectStatus.PENDING_APPROVAL, "当前项目不在待审批状态");
        move(projectId, ResearchProjectStatus.APPROVED, operatorName);
        record(projectId, "PROJECT_APPLICATION", "APPROVE", operatorId, comment);
    }

    @Override
    @Transactional
    public void rejectProject(Long projectId, Long operatorId, String operatorName, String comment) {
        ensure(projectId, ResearchProjectStatus.PENDING_APPROVAL, "当前项目不在待审批状态");
        move(projectId, ResearchProjectStatus.REJECTED, operatorName);
        record(projectId, "PROJECT_APPLICATION", "REJECT", operatorId, comment);
    }

    @Override
    @Transactional
    public void startProject(Long projectId, Long operatorId, String operatorName) {
        ensure(projectId, ResearchProjectStatus.APPROVED, "仅审批通过的项目可以启动");
        move(projectId, ResearchProjectStatus.IN_PROGRESS, operatorName);
        record(projectId, "PROJECT_EXECUTION", "START", operatorId, "项目正式启动");
    }

    @Override
    @Transactional
    public void submitAcceptance(Long projectId, Long operatorId, String operatorName) {
        ensure(projectId, ResearchProjectStatus.IN_PROGRESS, "仅执行中的项目可以提交验收");
        move(projectId, ResearchProjectStatus.PENDING_ACCEPTANCE, operatorName);
        record(projectId, "PROJECT_ACCEPTANCE", "SUBMIT", operatorId, "提交项目验收");
    }

    @Override
    @Transactional
    public void reviewAcceptance(Long projectId, Long operatorId, String operatorName, boolean approved, String comment) {
        ensure(projectId, ResearchProjectStatus.PENDING_ACCEPTANCE, "当前项目不在待验收状态");
        move(projectId, approved ? ResearchProjectStatus.COMPLETED : ResearchProjectStatus.IN_PROGRESS, operatorName);
        record(projectId, "PROJECT_ACCEPTANCE", approved ? "APPROVE" : "REJECT", operatorId, comment);
    }

    private ResearchProject required(Long projectId) {
        ResearchProject project = projectMapper.selectProjectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private void ensure(Long projectId, String expectedStatus, String message) {
        if (!expectedStatus.equals(required(projectId).getStatus())) {
            throw new ServiceException(message);
        }
    }

    private void move(Long projectId, String status, String operatorName) {
        projectMapper.updateProjectStatus(projectId, status, operatorName);
    }

    private void record(Long projectId, String businessType, String action, Long operatorId, String comment) {
        ResearchApproval approval = new ResearchApproval();
        approval.setProjectId(projectId);
        approval.setBusinessType(businessType);
        approval.setAction(action);
        approval.setOperatorId(operatorId);
        approval.setComment(comment == null ? "" : comment);
        approvalMapper.insertApproval(approval);
    }
}
