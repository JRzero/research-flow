package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.ResearchApproval;

public interface ResearchApprovalMapper {
    List<ResearchApproval> selectByProjectId(Long projectId);
    int insertApproval(ResearchApproval approval);
}
