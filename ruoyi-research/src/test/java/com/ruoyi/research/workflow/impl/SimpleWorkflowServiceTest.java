package com.ruoyi.research.workflow.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.research.domain.ResearchApproval;
import com.ruoyi.research.domain.ResearchProject;
import com.ruoyi.research.domain.ResearchProjectStatus;
import com.ruoyi.research.mapper.ResearchApprovalMapper;
import com.ruoyi.research.mapper.ResearchProjectMapper;

@ExtendWith(MockitoExtension.class)
class SimpleWorkflowServiceTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ResearchApprovalMapper approvalMapper;
    @InjectMocks private SimpleWorkflowService workflowService;

    @Test
    void approveMovesPendingProjectToApprovedAndWritesAuditRecord() {
        ResearchProject project = project(ResearchProjectStatus.PENDING_APPROVAL);
        when(projectMapper.selectProjectById(1001L)).thenReturn(project);

        workflowService.approveProject(1001L, 20L, "research_admin", "同意立项");

        verify(projectMapper).updateProjectStatus(1001L, ResearchProjectStatus.APPROVED, "research_admin");
        ArgumentCaptor<ResearchApproval> captor = ArgumentCaptor.forClass(ResearchApproval.class);
        verify(approvalMapper).insertApproval(captor.capture());
        ResearchApproval approval = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("PROJECT_APPLICATION", approval.getBusinessType());
        org.junit.jupiter.api.Assertions.assertEquals("APPROVE", approval.getAction());
        org.junit.jupiter.api.Assertions.assertEquals("同意立项", approval.getComment());
    }

    @Test
    void cannotApproveProjectOutsidePendingApproval() {
        when(projectMapper.selectProjectById(1001L)).thenReturn(project(ResearchProjectStatus.DRAFT));
        assertThrows(ServiceException.class,
                () -> workflowService.approveProject(1001L, 20L, "research_admin", "同意"));
    }

    @Test
    void acceptanceRejectionReturnsProjectToExecution() {
        when(projectMapper.selectProjectById(1001L)).thenReturn(project(ResearchProjectStatus.PENDING_ACCEPTANCE));

        workflowService.reviewAcceptance(1001L, 20L, "research_admin", false, "补充验证材料");

        verify(projectMapper).updateProjectStatus(1001L, ResearchProjectStatus.IN_PROGRESS, "research_admin");
        ArgumentCaptor<ResearchApproval> captor = ArgumentCaptor.forClass(ResearchApproval.class);
        verify(approvalMapper).insertApproval(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("REJECT", captor.getValue().getAction());
    }

    private ResearchProject project(String status) {
        ResearchProject project = new ResearchProject();
        project.setProjectId(1001L);
        project.setStatus(status);
        return project;
    }
}
