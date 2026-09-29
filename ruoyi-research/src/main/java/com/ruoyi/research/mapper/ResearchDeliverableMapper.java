package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.ResearchDeliverable;

public interface ResearchDeliverableMapper {
    List<ResearchDeliverable> selectByProjectId(Long projectId);
    int insertDeliverable(ResearchDeliverable deliverable);
}
