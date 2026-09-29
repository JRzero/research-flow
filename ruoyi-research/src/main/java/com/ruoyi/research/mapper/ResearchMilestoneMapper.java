package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.ResearchMilestone;

public interface ResearchMilestoneMapper {
    List<ResearchMilestone> selectByProjectId(Long projectId);
    ResearchMilestone selectById(Long milestoneId);
    int insertMilestone(ResearchMilestone milestone);
    int completeMilestone(@Param("milestoneId") Long milestoneId, @Param("updateBy") String updateBy);
}
