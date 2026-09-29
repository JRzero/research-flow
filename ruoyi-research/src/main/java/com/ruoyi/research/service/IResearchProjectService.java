package com.ruoyi.research.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.research.domain.ResearchAcceptance;
import com.ruoyi.research.domain.ResearchDeliverable;
import com.ruoyi.research.domain.ResearchExpense;
import com.ruoyi.research.domain.ResearchMilestone;
import com.ruoyi.research.domain.ResearchProgress;
import com.ruoyi.research.domain.ResearchProject;

public interface IResearchProjectService {
    List<ResearchProject> listProjects(Long userId, boolean canViewAll, String status, String keyword);
    Map<String, Object> getDashboard(Long userId, boolean canViewAll);
    Map<String, Object> getProjectDetail(Long projectId, Long userId, boolean canViewAll);
    ResearchProject createProject(ResearchProject project, Long userId, Long deptId, String username);
    void updateDraft(ResearchProject project, Long userId, boolean canManageAll, String username);
    void submitProject(Long projectId, Long userId, boolean canManageAll, String username);
    void approveProject(Long projectId, Long userId, String username, String comment);
    void rejectProject(Long projectId, Long userId, String username, String comment);
    void startProject(Long projectId, Long userId, String username);
    void addMilestone(Long projectId, ResearchMilestone milestone, Long userId, boolean canManageAll, String username);
    void completeMilestone(Long projectId, Long milestoneId, Long userId, boolean canManageAll, String username);
    void addProgress(Long projectId, ResearchProgress progress, Long userId, boolean canManageAll, String username);
    void addExpense(Long projectId, ResearchExpense expense, Long userId, boolean canManageAll, String username);
    void addDeliverable(Long projectId, ResearchDeliverable deliverable, Long userId, boolean canManageAll, String username);
    void submitAcceptance(Long projectId, ResearchAcceptance acceptance, Long userId, boolean canManageAll, String username);
    void reviewAcceptance(Long projectId, Long userId, String username, boolean approved, String comment);
    List<ResearchProject> listRisks(Long userId, boolean canViewAll);
}
