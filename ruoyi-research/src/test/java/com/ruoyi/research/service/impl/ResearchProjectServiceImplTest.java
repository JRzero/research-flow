package com.ruoyi.research.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.research.domain.ResearchAcceptance;
import com.ruoyi.research.domain.ResearchProject;
import com.ruoyi.research.domain.ResearchProjectStatus;
import com.ruoyi.research.mapper.ResearchAcceptanceMapper;
import com.ruoyi.research.mapper.ResearchApprovalMapper;
import com.ruoyi.research.mapper.ResearchDeliverableMapper;
import com.ruoyi.research.mapper.ResearchExpenseMapper;
import com.ruoyi.research.mapper.ResearchMilestoneMapper;
import com.ruoyi.research.mapper.ResearchProgressMapper;
import com.ruoyi.research.mapper.ResearchProjectMapper;
import com.ruoyi.research.workflow.WorkflowService;

@ExtendWith(MockitoExtension.class)
class ResearchProjectServiceImplTest {
    @Mock private ResearchProjectMapper projectMapper;
    @Mock private ResearchMilestoneMapper milestoneMapper;
    @Mock private ResearchProgressMapper progressMapper;
    @Mock private ResearchExpenseMapper expenseMapper;
    @Mock private ResearchDeliverableMapper deliverableMapper;
    @Mock private ResearchApprovalMapper approvalMapper;
    @Mock private ResearchAcceptanceMapper acceptanceMapper;
    @Mock private WorkflowService workflowService;
    @InjectMocks private ResearchProjectServiceImpl service;

    @Test
    void rejectedAcceptanceCanBeResubmitted() {
        ResearchProject project = new ResearchProject();
        project.setProjectId(1001L);
        project.setOwnerUserId(21L);
        project.setStatus(ResearchProjectStatus.IN_PROGRESS);
        when(projectMapper.selectProjectById(1001L)).thenReturn(project);

        ResearchAcceptance rejected = new ResearchAcceptance();
        rejected.setProjectId(1001L);
        rejected.setStatus("REJECTED");
        when(acceptanceMapper.selectByProjectId(1001L)).thenReturn(rejected);

        ResearchAcceptance resubmission = new ResearchAcceptance();
        resubmission.setProjectSummary("已根据验收意见完成整改并补充验证材料");

        service.submitAcceptance(1001L, resubmission, 21L, false, "researcher");

        verify(acceptanceMapper).resubmitAcceptance(resubmission);
        verify(acceptanceMapper, never()).insertAcceptance(resubmission);
        verify(workflowService).submitAcceptance(1001L, 21L, "researcher");
    }
}
