package com.ruoyi.web.controller.research;

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
import com.ruoyi.research.v2.application.ResearchFlowV2Service;

@RestController
@RequestMapping("/research")
public class ResearchProjectController extends BaseController {
    @Autowired private ResearchFlowV2Service service;
    @Autowired private ResearchAiService researchAiService;

    @GetMapping("/dashboard")
    public AjaxResult dashboard() { return success(service.dashboard(getUserId(), canViewAll())); }

    @GetMapping("/analytics")
    public AjaxResult analytics() { return success(service.analytics(getUserId(), canViewAll())); }

    @GetMapping("/proposals")
    public AjaxResult proposals(@RequestParam(required=false) String status, @RequestParam(required=false) String keyword) {
        return success(service.proposals(getUserId(), canViewAll(), status, keyword));
    }

    @GetMapping("/proposals/{proposalId}")
    public AjaxResult proposal(@PathVariable Long proposalId) {
        return success(service.proposal(proposalId, getUserId(), canViewAll()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @Log(title="科研申报",businessType=BusinessType.INSERT)
    @PostMapping("/proposals")
    public AjaxResult createProposal(@RequestBody Map<String,Object> input) {
        return success(service.createProposal(input,getUserId(),getDeptId(),getUsername()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @Log(title="科研申报",businessType=BusinessType.UPDATE)
    @PutMapping("/proposals/{proposalId}")
    public AjaxResult updateProposal(@PathVariable Long proposalId,@RequestBody Map<String,Object> input) {
        service.updateProposal(proposalId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @GetMapping("/proposals/{proposalId}/validation")
    public AjaxResult validateProposal(@PathVariable Long proposalId) {
        return success(service.validateProposal(proposalId,getUserId(),canViewAll()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/proposals/{proposalId}/submit")
    public AjaxResult submitProposal(@PathVariable Long proposalId) {
        return success(service.submitProposal(proposalId,getUserId(),canManageAll(),getUsername()));
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/proposals/{proposalId}/approve")
    public AjaxResult approveProposal(@PathVariable Long proposalId,@RequestBody(required=false) Map<String,Object> input) {
        service.approveProposal(proposalId,getUserId(),getUsername(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/proposals/{proposalId}/reject")
    public AjaxResult rejectProposal(@PathVariable Long proposalId,@RequestBody(required=false) Map<String,Object> input) {
        service.rejectProposal(proposalId,getUserId(),getUsername(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/proposals/{proposalId}/award")
    public AjaxResult issueAward(@PathVariable Long proposalId,@RequestBody(required=false) Map<String,Object> input) {
        return success(service.issueAward(proposalId,input==null?Map.of():input,getUserId(),getUsername()));
    }

    @GetMapping("/projects")
    public AjaxResult projects(@RequestParam(required=false) String status,@RequestParam(required=false) String keyword) {
        return success(service.projects(getUserId(),canViewAll(),status,keyword));
    }

    @GetMapping("/projects/{projectId}")
    public AjaxResult project(@PathVariable Long projectId) {
        return success(service.project(projectId,getUserId(),canViewAll()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/work-items")
    public AjaxResult addWorkItem(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.addWorkItem(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/activate")
    public AjaxResult activateProject(@PathVariable Long projectId) {
        service.activateProject(projectId,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/work-items/{workItemId}/{action}")
    public AjaxResult workItemAction(@PathVariable Long projectId,@PathVariable Long workItemId,@PathVariable String action) {
        service.workItemAction(projectId,workItemId,action,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/progress-reports")
    public AjaxResult addProgress(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.addProgressReport(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/risks")
    public AjaxResult addRisk(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        return success(service.addRisk(projectId,input,getUserId(),canManageAll(),getUsername()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/risks/{riskId}/occur")
    public AjaxResult riskOccurred(@PathVariable Long projectId,@PathVariable Long riskId,@RequestBody(required=false) Map<String,Object> input) {
        return success(service.riskOccurred(projectId,riskId,input==null?Map.of():input,getUserId(),canManageAll(),getUsername()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/issues")
    public AjaxResult addIssue(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.addIssue(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/issues/{issueId}/resolve")
    public AjaxResult resolveIssue(@PathVariable Long projectId,@PathVariable Long issueId,@RequestBody Map<String,Object> input) {
        service.resolveIssue(projectId,issueId,String.valueOf(input.getOrDefault("resolution","")),getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/changes")
    public AjaxResult createChange(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        return success(service.createChange(projectId,input,getUserId(),canManageAll(),getUsername()));
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/changes/{changeId}/submit")
    public AjaxResult submitChange(@PathVariable Long projectId,@PathVariable Long changeId) {
        service.submitChange(projectId,changeId,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/changes/{changeId}/approve")
    public AjaxResult approveChange(@PathVariable Long projectId,@PathVariable Long changeId,@RequestBody(required=false) Map<String,Object> input) {
        service.reviewChange(projectId,changeId,true,getUserId(),getUsername(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/changes/{changeId}/reject")
    public AjaxResult rejectChange(@PathVariable Long projectId,@PathVariable Long changeId,@RequestBody(required=false) Map<String,Object> input) {
        service.reviewChange(projectId,changeId,false,getUserId(),getUsername(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/changes/{changeId}/apply")
    public AjaxResult applyChange(@PathVariable Long projectId,@PathVariable Long changeId) {
        service.applyChange(projectId,changeId,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/expenses")
    public AjaxResult addExpense(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.addExpense(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/outcomes")
    public AjaxResult addOutcome(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.addOutcome(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')")
    @PostMapping("/projects/{projectId}/acceptance")
    public AjaxResult submitAcceptance(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.submitAcceptance(projectId,input,getUserId(),canManageAll(),getUsername()); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/acceptance/approve")
    public AjaxResult approveAcceptance(@PathVariable Long projectId,@RequestBody(required=false) Map<String,Object> input) {
        service.reviewAcceptance(projectId,true,getUserId(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/acceptance/reject")
    public AjaxResult rejectAcceptance(@PathVariable Long projectId,@RequestBody(required=false) Map<String,Object> input) {
        service.reviewAcceptance(projectId,false,getUserId(),comment(input)); return success();
    }

    @PreAuthorize("@ss.hasRole('research_admin')")
    @PostMapping("/projects/{projectId}/closeout")
    public AjaxResult closeout(@PathVariable Long projectId,@RequestBody Map<String,Object> input) {
        service.completeCloseout(projectId,input,getUserId(),getUsername()); return success();
    }

    @GetMapping("/approvals")
    public AjaxResult approvals() { return success(service.approvalQueue()); }

    @GetMapping("/risks")
    public AjaxResult risks() { return success(service.riskRegister(getUserId(),canViewAll())); }

    @PostMapping("/ai/proposal")
    public AjaxResult aiProposal(@RequestBody Map<String,String> input) {
        return success(researchAiService.generateProposal(input==null?"":input.getOrDefault("description","")));
    }

    private boolean canViewAll() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("research_admin") || SecurityUtils.hasRole("research_manager");
    }
    private boolean canManageAll() {
        return SecurityUtils.isAdmin() || SecurityUtils.hasRole("research_admin");
    }
    private String comment(Map<String,?> input) {
        if (input == null || input.get("comment") == null) return "";
        return String.valueOf(input.get("comment"));
    }
}
