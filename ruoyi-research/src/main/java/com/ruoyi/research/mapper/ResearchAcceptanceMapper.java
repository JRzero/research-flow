package com.ruoyi.research.mapper;

import com.ruoyi.research.domain.ResearchAcceptance;

public interface ResearchAcceptanceMapper {
    ResearchAcceptance selectByProjectId(Long projectId);
    int insertAcceptance(ResearchAcceptance acceptance);
    int resubmitAcceptance(ResearchAcceptance acceptance);
    int updateAcceptanceReview(ResearchAcceptance acceptance);
}
