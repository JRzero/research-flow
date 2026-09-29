package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.ResearchProject;

public interface ResearchProjectMapper {
    List<ResearchProject> selectProjectList(@Param("ownerUserId") Long ownerUserId,
            @Param("status") String status, @Param("keyword") String keyword);
    ResearchProject selectProjectById(Long projectId);
    int insertProject(ResearchProject project);
    int updateProject(ResearchProject project);
    int updateProjectStatus(@Param("projectId") Long projectId, @Param("status") String status,
            @Param("updateBy") String updateBy);
    int updateProjectProgress(@Param("projectId") Long projectId, @Param("progress") Integer progress,
            @Param("updateBy") String updateBy);
}
