package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.ResearchProgress;

public interface ResearchProgressMapper {
    List<ResearchProgress> selectByProjectId(Long projectId);
    int insertProgress(ResearchProgress progress);
}
