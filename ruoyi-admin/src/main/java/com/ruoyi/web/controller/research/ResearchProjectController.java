package com.ruoyi.web.controller.research;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.research.ai.ResearchAiService;
import com.ruoyi.research.domain.ResearchAcceptance;
import com.ruoyi.research.domain.ResearchDeliverable;
import com.ruoyi.research.domain.ResearchExpense;
import com.ruoyi.research.domain.ResearchMilestone;
import com.ruoyi.research.domain.ResearchProgress;
import com.ruoyi.research.domain.ResearchProject;
import com.ruoyi.research.service.IResearchProjectService;

@RestController
@RequestMapping("/research")
public class ResearchProjectController extends BaseController {
    @Autowired
    private IResearchProjectService researchProjectService;
    @Autowired
    private ResearchAiService researchAiService;

    @GetMapping("/dashboard")
    public AjaxResult dashboard() {
        return success(researchProjectService.getDashboard(getUserId(), canViewAll()));
    }

    @GetMapping("/projects")
    public AjaxResult projects(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return success(researchProjectService.listProjects(getUserId(), canViewAll(), status, keyword));
    }

    @GetMapping("/projects/{projectId}")
    public AjaxResult detail(@PathVariable Long projectId) {
        return success(researchProjectService.getProjectDetail(projectId, getUserId(), canViewAll()));
    }

    @Log(title = "科研项目", businessType = BusinessType.INSERT)
    @PostMapping("/projects")
    public AjaxResult create(@RequestBody ResearchProject project) {
        return success(researchProjectService.createProject(project, getUserId(), getDeptId(), getUsername()));
    }

    @Log(title = "科研项目", businessType = BusinessType.UPDATE)
    @PutMapping("/projects/{projectId}")
    public AjaxResult update(@PathVariable Long projectId, @RequestBody ResearchProject project) {
        project.setProjectId(projectId);
        researchProjectService.updateDraft(project, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @Log(title = "项目申报", businessType = BusinessType.UPDATE)
    @PostMapping("/projects/{projectId}/submit")
    public AjaxResult submit(@PathVariable Long projectId) {
        researchProjectService.submitProject(projectId, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @Log(title = "项目审批", businessType = BusinessType.UPDATE)
    @PostMapping("/projects/{projectId}/approve")
    public AjaxResult approve(@PathVariable Long projectId, @RequestBody(required = false) Map<String, String> payload) {
        researchProjectService.approveProject(projectId, getUserId(), getUsername(), comment(payload));
        return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @Log(title = "项目审批", businessType = BusinessType.UPDATE)
    @PostMapping("/projects/{projectId}/reject")
    public AjaxResult reject(@PathVariable Long projectId, @RequestBody(required = false) Map<String, String> payload) {
        researchProjectService.rejectProject(projectId, getUserId(), getUsername(), comment(payload));
        return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @Log(title = "项目启动", businessType = BusinessType.UPDATE)
    @PostMapping("/projects/{projectId}/start")
    public AjaxResult start(@PathVariable Long projectId) {
        researchProjectService.startProject(projectId, getUserId(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/milestones")
    public AjaxResult addMilestone(@PathVariable Long projectId, @RequestBody ResearchMilestone milestone) {
        researchProjectService.addMilestone(projectId, milestone, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/milestones/{milestoneId}/complete")
    public AjaxResult completeMilestone(@PathVariable Long projectId, @PathVariable Long milestoneId) {
        researchProjectService.completeMilestone(projectId, milestoneId, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/progress")
    public AjaxResult addProgress(@PathVariable Long projectId, @RequestBody ResearchProgress progress) {
        researchProjectService.addProgress(projectId, progress, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/expenses")
    public AjaxResult addExpense(@PathVariable Long projectId, @RequestBody ResearchExpense expense) {
        researchProjectService.addExpense(projectId, expense, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/deliverables")
    public AjaxResult addDeliverable(@PathVariable Long projectId, @RequestBody ResearchDeliverable deliverable) {
        researchProjectService.addDeliverable(projectId, deliverable, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PostMapping("/projects/{projectId}/acceptance")
    public AjaxResult submitAcceptance(@PathVariable Long projectId, @RequestBody ResearchAcceptance acceptance) {
        researchProjectService.submitAcceptance(projectId, acceptance, getUserId(), canManageAll(), getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/acceptance/approve")
    public AjaxResult approveAcceptance(@PathVariable Long projectId, @RequestBody(required = false) Map<String, String> payload) {
        researchProjectService.reviewAcceptance(projectId, getUserId(), getUsername(), true, comment(payload));
        return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/acceptance/reject")
    public AjaxResult rejectAcceptance(@PathVariable Long projectId, @RequestBody(required = false) Map<String, String> payload) {
        researchProjectService.reviewAcceptance(projectId, getUserId(), getUsername(), false, comment(payload));
        return success();
    }


    @PostMapping("/ai/proposal")
    public AjaxResult aiProposal(@RequestBody Map<String, String> payload) {
        return success(researchAiService.generateProposal(payload == null ? "" : payload.get("description")));
    }

    @GetMapping("/risks")
    public AjaxResult risks() {
        List<ResearchProject> risks = researchProjectService.listRisks(getUserId(), canViewAll());
        return success(risks);
    }

    private boolean canViewAll() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("research_admin") || SecurityUtils.hasRole("research_manager");
    }

    private boolean canManageAll() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("research_admin");
    }

    private String comment(Map<String, String> payload) {
        return payload == null ? "" : payload.getOrDefault("comment", "");
    }
}
