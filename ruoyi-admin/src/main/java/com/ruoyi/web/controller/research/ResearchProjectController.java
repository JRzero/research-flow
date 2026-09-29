package com.ruoyi.web.controller.research;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.research.ai.ResearchAiService;
import com.ruoyi.research.v2.ResearchFlowService;

@RestController
@RequestMapping("/research")
public class ResearchProjectController extends BaseController {
    @Autowired private ResearchFlowService service;
    @Autowired private ResearchAiService researchAiService;

    @GetMapping("/users") public AjaxResult users(@RequestParam(required=false)String keyword){return success(service.listAssignableUsers(keyword));}
    @GetMapping("/dashboard") public AjaxResult dashboard(){return success(service.dashboard(getUserId(),canViewAll()));}
    @GetMapping("/analytics") public AjaxResult analytics(){return success(service.analytics(getUserId(),canViewAll()));}

    @GetMapping("/proposals") public AjaxResult proposals(@RequestParam(required=false)String status,@RequestParam(required=false)String keyword){return success(service.listProposals(getUserId(),canViewAll(),status,keyword));}
    @GetMapping("/proposals/{id}") public AjaxResult proposal(@PathVariable Long id){return success(service.getProposal(id,getUserId(),canViewAll()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/proposals") @Log(title="项目申请",businessType=BusinessType.INSERT)
    public AjaxResult createProposal(@RequestBody Map<String,Object> body){return success(service.createProposal(body,getUserId(),getDeptId(),getUsername()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PutMapping("/proposals/{id}")
    public AjaxResult updateProposal(@PathVariable Long id,@RequestBody Map<String,Object> body){service.updateProposal(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @GetMapping("/proposals/{id}/validation") public AjaxResult validateProposal(@PathVariable Long id){return success(service.validateProposal(id,getUserId(),canManageAll()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/proposals/{id}/submit")
    public AjaxResult submitProposal(@PathVariable Long id){service.submitProposal(id,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/proposals/{id}/approve")
    public AjaxResult approveProposal(@PathVariable Long id,@RequestBody(required=false)Map<String,Object> body){service.approveProposal(id,body==null?Map.of():body,getUserId(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/proposals/{id}/reject")
    public AjaxResult rejectProposal(@PathVariable Long id,@RequestBody(required=false)Map<String,Object> body){service.rejectProposal(id,comment(body),getUserId(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/proposals/{id}/members")
    public AjaxResult addProposalMember(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addProposalMember(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PutMapping("/proposals/{id}/budget")
    public AjaxResult saveProposalBudget(@PathVariable Long id,@RequestBody List<Map<String,Object>> body){service.saveProposalBudget(id,body,getUserId(),canManageAll());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/proposals/{id}/outputs")
    public AjaxResult addExpectedOutput(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addExpectedOutput(id,body,getUserId(),canManageAll());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/proposals/{id}/documents")
    public AjaxResult proposalDocument(@PathVariable Long id,@RequestBody Map<String,Object> body){service.attachProposalDocument(id,body,getUserId(),canManageAll());return success();}

    @GetMapping("/projects") public AjaxResult projects(@RequestParam(required=false)String status,@RequestParam(required=false)String keyword){return success(service.listProjects(getUserId(),canViewAll(),status,keyword));}
    @GetMapping("/projects/{id}") public AjaxResult project(@PathVariable Long id){return success(service.getProject(id,getUserId(),canViewAll()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/members")
    public AjaxResult addProjectMember(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addProjectMember(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/work-items")
    public AjaxResult addWorkItem(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addWorkItem(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/work-items/{workItemId}/{action}")
    public AjaxResult workItemAction(@PathVariable Long id,@PathVariable Long workItemId,@PathVariable String action){service.workItemAction(id,workItemId,action,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/activate")
    public AjaxResult activate(@PathVariable Long id){service.activateProject(id,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/progress-reports")
    public AjaxResult progress(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addProgressReport(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/expenses")
    public AjaxResult expense(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addExpense(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/outcomes")
    public AjaxResult outcome(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addOutcome(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/documents")
    public AjaxResult projectDocument(@PathVariable Long id,@RequestBody Map<String,Object> body){service.attachProjectDocument(id,body,getUserId(),canManageAll());return success();}

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/risks")
    public AjaxResult risk(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addRisk(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/risks/{riskId}/status/{status}")
    public AjaxResult riskStatus(@PathVariable Long id,@PathVariable Long riskId,@PathVariable String status){service.updateRiskStatus(id,riskId,status,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/risks/{riskId}/occur")
    public AjaxResult riskOccur(@PathVariable Long id,@PathVariable Long riskId,@RequestBody(required=false)Map<String,Object> body){return success(service.convertRiskToIssue(id,riskId,body==null?Map.of():body,getUserId(),canManageAll(),getUsername()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/issues")
    public AjaxResult issue(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addIssue(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/decisions")
    public AjaxResult decision(@PathVariable Long id,@RequestBody Map<String,Object> body){service.addDecision(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/issues/{issueId}/status/{status}")
    public AjaxResult issueStatus(@PathVariable Long id,@PathVariable Long issueId,@PathVariable String status,@RequestBody(required=false)Map<String,Object> body){service.updateIssueStatus(id,issueId,status,body==null?Map.of():body,getUserId(),canManageAll(),getUsername());return success();}
    @GetMapping("/governance") public AjaxResult governance(){return success(service.risksAndIssues(getUserId(),canViewAll()));}

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/changes")
    public AjaxResult createChange(@PathVariable Long id,@RequestBody Map<String,Object> body){return success(service.createChange(id,body,getUserId(),canManageAll(),getUsername()));}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/changes/{changeId}/submit")
    public AjaxResult submitChange(@PathVariable Long id,@PathVariable Long changeId){service.submitChange(id,changeId,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/projects/{id}/changes/{changeId}/approve")
    public AjaxResult approveChange(@PathVariable Long id,@PathVariable Long changeId,@RequestBody(required=false)Map<String,Object> body){service.approveChange(id,changeId,comment(body),getUserId(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/projects/{id}/changes/{changeId}/reject")
    public AjaxResult rejectChange(@PathVariable Long id,@PathVariable Long changeId,@RequestBody(required=false)Map<String,Object> body){service.rejectChange(id,changeId,comment(body),getUserId(),getUsername());return success();}
    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/changes/{changeId}/apply")
    public AjaxResult applyChange(@PathVariable Long id,@PathVariable Long changeId){service.applyChange(id,changeId,getUserId(),canManageAll(),getUsername());return success();}

    @PreAuthorize("@ss.hasAnyRoles('research_owner,research_admin')") @PostMapping("/projects/{id}/acceptance")
    public AjaxResult acceptance(@PathVariable Long id,@RequestBody Map<String,Object> body){service.submitAcceptance(id,body,getUserId(),canManageAll(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/projects/{id}/acceptance/review/{decision}")
    public AjaxResult acceptanceReview(@PathVariable Long id,@PathVariable String decision,@RequestBody(required=false)Map<String,Object> body){service.reviewAcceptance(id,"approve".equalsIgnoreCase(decision),comment(body),getUserId(),getUsername());return success();}
    @PreAuthorize("@ss.hasRole('research_admin')") @PostMapping("/projects/{id}/closeout")
    public AjaxResult closeout(@PathVariable Long id,@RequestBody(required=false)Map<String,Object> body){service.completeCloseout(id,body==null?Map.of():body,getUserId(),getUsername());return success();}

    @PreAuthorize("@ss.hasRole('research_admin')") @GetMapping("/approvals") public AjaxResult approvals(){return success(service.approvalCenter());}
    @PostMapping("/ai/proposal") public AjaxResult aiProposal(@RequestBody Map<String,String> body){return success(researchAiService.generateProposal(body==null?"":body.get("description")));}

    private boolean canViewAll(){return SecurityUtils.isAdmin()||SecurityUtils.hasRole("research_admin")||SecurityUtils.hasRole("research_manager");}
    private boolean canManageAll(){return SecurityUtils.isAdmin()||SecurityUtils.hasRole("research_admin");}
    private String comment(Map<String,?>body){Object v=body==null?null:body.get("comment");return v==null?"":String.valueOf(v);}
}
