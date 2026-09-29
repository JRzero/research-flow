package com.ruoyi.research.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

@Service
public class ResearchFlowServiceImpl implements ResearchFlowService {
    @Autowired private ResearchFlowMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    @Override public List<Map<String,Object>> listAssignableUsers(String keyword){return mapper.selectAssignableUsers(keyword);}

    @Override
    public Map<String,Object> dashboard(Long userId, boolean viewAll) {
        List<Map<String,Object>> projects = listProjects(userId, viewAll, null, null);
        List<Map<String,Object>> proposals = listProposals(userId, viewAll, null, null);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("totalProjects", projects.size());
        out.put("activeProjects", countStatus(projects,"ACTIVE"));
        out.put("planningProjects", countStatus(projects,"PLANNING"));
        out.put("closingProjects", countStatus(projects,"CLOSING"));
        out.put("pendingProposals", proposals.stream().filter(p -> "UNDER_REVIEW".equals(s(p,"status"))).count());
        out.put("draftProposals", proposals.stream().filter(p -> "DRAFT".equals(s(p,"status"))).count());
        List<Map<String,Object>> risks = mapper.selectAllRisks(userId, viewAll);
        List<Map<String,Object>> issues = mapper.selectAllIssues(userId, viewAll);
        out.put("openRisks", risks.stream().filter(r -> !"CLOSED".equals(s(r,"status"))).count());
        out.put("openIssues", issues.stream().filter(i -> !List.of("RESOLVED","CLOSED").contains(s(i,"status"))).count());
        BigDecimal budget = projects.stream().map(p -> money(p.get("currentBudget"))).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal used = projects.stream().map(p -> money(p.get("usedBudget"))).reduce(BigDecimal.ZERO,BigDecimal::add);
        out.put("totalBudget", budget);
        out.put("usedBudget", used);
        out.put("budgetExecutionRate", percent(used,budget));
        out.put("averageProgress", projects.isEmpty()?0:Math.round(projects.stream().mapToInt(p->integer(p.get("progress"),0)).average().orElse(0)));
        out.put("recentProjects", projects.stream().limit(6).toList());
        out.put("priorityRisks", risks.stream().filter(r -> List.of("HIGH","CRITICAL").contains(s(r,"riskLevel")) && !"CLOSED".equals(s(r,"status"))).limit(5).toList());
        return out;
    }

    @Override
    public Map<String,Object> analytics(Long userId, boolean viewAll) {
        List<Map<String,Object>> projects=listProjects(userId,viewAll,null,null);
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("projects",projects);
        out.put("total",projects.size());
        out.put("planning",countStatus(projects,"PLANNING"));
        out.put("active",countStatus(projects,"ACTIVE"));
        out.put("closing",countStatus(projects,"CLOSING"));
        out.put("closed",countStatus(projects,"CLOSED"));
        out.put("risks",mapper.selectAllRisks(userId,viewAll));
        out.put("issues",mapper.selectAllIssues(userId,viewAll));
        return out;
    }

    @Override public List<Map<String,Object>> listProposals(Long userId,boolean viewAll,String status,String keyword){return mapper.selectProposals(userId,viewAll,status,keyword);}

    @Override
    public Map<String,Object> getProposal(Long proposalId,Long userId,boolean viewAll){
        Map<String,Object> p=requiredProposal(proposalId); checkProposalAccess(p,userId,viewAll);
        Map<String,Object> out=new LinkedHashMap<>(p);
        out.put("members",mapper.selectProposalMembers(proposalId));
        out.put("budgetLines",mapper.selectProposalBudget(proposalId));
        out.put("expectedOutputs",mapper.selectExpectedOutputs(proposalId));
        out.put("reviews",mapper.selectReviews(proposalId));
        out.put("documents",mapper.selectDocuments("PROPOSAL",proposalId));
        Map<String,Object> award=mapper.selectAwardByProposal(proposalId); out.put("award",award);
        return out;
    }

    @Override
    @Transactional
    public Map<String,Object> createProposal(Map<String,Object> input,Long userId,Long deptId,String username){
        ResearchFlowRules.require(StringUtils.isNotEmpty(s(input,"title")),"项目名称不能为空");
        ResearchFlowRules.require(StringUtils.isNotEmpty(s(input,"plannedEndDate")),"计划结束日期不能为空");
        Map<String,Object> record=new LinkedHashMap<>();
        record.put("recordNo",no("RR"));
        record.put("title",s(input,"title"));
        record.put("ownerUserId",userId); record.put("deptId",deptId); record.put("username",username);
        mapper.insertRecord(record);
        Map<String,Object> p=new LinkedHashMap<>(input);
        p.put("recordId",record.get("recordId")); p.put("proposalNo",no("PR"));
        p.put("applicantUserId",userId); p.put("applicantDeptId",deptId); p.put("status","DRAFT"); p.put("username",username);
        p.putIfAbsent("requestedBudget",BigDecimal.ZERO);
        mapper.insertProposal(p);
        Map<String,Object> member=new LinkedHashMap<>(); member.put("proposalId",p.get("proposalId")); member.put("userId",userId); member.put("memberRole","PI"); member.put("responsibility","项目总体负责"); member.put("username",username);
        mapper.insertProposalMember(member);
        replaceProposalBudget(longValue(p.get("proposalId")), list(input.get("budgetLines")));
        for(Map<String,Object> o:list(input.get("expectedOutputs"))){o.put("proposalId",p.get("proposalId"));mapper.insertExpectedOutput(o);}
        for(Map<String,Object> d:list(input.get("documents"))){insertDocument(longValue(record.get("recordId")),"PROPOSAL",longValue(p.get("proposalId")),"APPLICATION",d,userId);}
        return getProposal(longValue(p.get("proposalId")),userId,true);
    }

    @Override
    @Transactional
    public void updateProposal(Long proposalId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProposal(proposalId); checkProposalOwner(p,userId,manageAll);
        ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"仅草稿或退回修改的申请可编辑");
        Map<String,Object> data=new LinkedHashMap<>(input); data.put("proposalId",proposalId); data.put("version",integer(p.get("version"),0)); data.put("username",username);
        ResearchFlowRules.require(mapper.updateProposal(data)==1,"申请已被其他用户修改，请刷新后重试");
        if(input.containsKey("budgetLines")) replaceProposalBudget(proposalId,list(input.get("budgetLines")));
        if(input.containsKey("expectedOutputs")) replaceExpectedOutputs(proposalId,list(input.get("expectedOutputs")));
    }

    @Override
    public Map<String,Object> validateProposal(Long proposalId,Long userId,boolean manageAll){
        Map<String,Object> p=requiredProposal(proposalId); checkProposalOwner(p,userId,manageAll);
        List<Map<String,String>> missing=new ArrayList<>(); List<Map<String,String>> warnings=new ArrayList<>();
        miss(missing,StringUtils.isEmpty(s(p,"objectives")),"OBJECTIVES_REQUIRED","缺少研究目标");
        miss(missing,StringUtils.isEmpty(s(p,"researchContent")),"CONTENT_REQUIRED","缺少研究内容");
        miss(missing,p.get("plannedStartDate")==null,"START_DATE_REQUIRED","缺少计划开始日期");
        miss(missing,p.get("plannedEndDate")==null,"END_DATE_REQUIRED","缺少计划结束日期");
        if(p.get("plannedStartDate")!=null&&p.get("plannedEndDate")!=null){
            LocalDate start=LocalDate.parse(String.valueOf(p.get("plannedStartDate")));
            LocalDate end=LocalDate.parse(String.valueOf(p.get("plannedEndDate")));
            miss(missing,end.isBefore(start),"DATE_RANGE_INVALID","计划结束日期不能早于开始日期");
        }
        BigDecimal requestedBudget=money(p.get("requestedBudget"));
        BigDecimal budgetLines=money(mapper.sumProposalBudget(proposalId));
        miss(missing,requestedBudget.compareTo(BigDecimal.ZERO)<=0,"BUDGET_REQUIRED","申请预算必须大于0");
        miss(missing,mapper.selectProposalBudget(proposalId).isEmpty(),"BUDGET_LINES_REQUIRED","至少需要一个预算科目");
        miss(missing,requestedBudget.compareTo(budgetLines)!=0,"BUDGET_TOTAL_MISMATCH","预算明细合计必须等于申请预算");
        miss(missing,mapper.countProposalMembers(proposalId)==0,"MEMBER_REQUIRED","至少需要一名项目成员");
        miss(missing,mapper.selectExpectedOutputs(proposalId).isEmpty(),"OUTPUT_REQUIRED","至少需要一项预期成果");
        if(mapper.selectDocuments("PROPOSAL",proposalId).isEmpty()) warnings.add(msg("NO_DOCUMENT","尚未上传申报附件"));
        Map<String,Object> out=new LinkedHashMap<>();out.put("valid",missing.isEmpty());out.put("missing",missing);out.put("warnings",warnings);return out;
    }

    @Override
    @Transactional
    public void submitProposal(Long proposalId,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProposal(proposalId);checkProposalOwner(p,userId,manageAll);
        ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"当前申请状态不能提交");
        Map<String,Object> validation=validateProposal(proposalId,userId,manageAll);
        ResearchFlowRules.require(Boolean.TRUE.equals(validation.get("valid")),"申报材料不完整，请先完成必填信息");
        mapper.updateProposalStatus(proposalId,"UNDER_REVIEW");
        mapper.updateRecordPhase(longValue(p.get("recordId")),"REVIEW","ACTIVE");
        Map<String,Object> review=new LinkedHashMap<>();review.put("proposalId",proposalId);review.put("reviewType","TECHNICAL");review.put("status","PENDING");review.put("reviewerUserId",null);mapper.insertReview(review);
        Long wf=startWorkflow("PROPOSAL_APPROVAL","PROPOSAL",proposalId,userId,"TECHNICAL");
        workflowAction(wf,"SUBMIT","SUBMIT",userId,"提交项目申报");
    }

    @Override
    @Transactional
    public void approveProposal(Long proposalId,Map<String,Object> approval,Long userId,String username){
        Map<String,Object> p=requiredProposal(proposalId);
        ResearchFlowRules.require("UNDER_REVIEW".equals(s(p,"status")),"仅评审中的申请可以批准");
        ResearchFlowRules.require(mapper.selectAwardByProposal(proposalId)==null,"该申请已生成立项批复");
        String comment=s(approval,"comment");
        mapper.completeReview(proposalId,"PASS",comment,userId);mapper.updateProposalStatus(proposalId,"APPROVED");
        Map<String,Object> award=new LinkedHashMap<>();
        award.put("recordId",p.get("recordId"));award.put("proposalId",proposalId);award.put("awardNo",no("AW"));
        award.put("approvedTitle",value(approval,"approvedTitle",p.get("title")));
        award.put("approvedStartDate",value(approval,"approvedStartDate",p.get("plannedStartDate")));
        award.put("approvedEndDate",value(approval,"approvedEndDate",p.get("plannedEndDate")));
        BigDecimal approvedBudget=approval.get("approvedBudget")==null?money(p.get("requestedBudget")):money(approval.get("approvedBudget"));
        ResearchFlowRules.require(award.get("approvedStartDate")!=null&&award.get("approvedEndDate")!=null,"批复周期不能为空");
        ResearchFlowRules.require(approvedBudget.compareTo(BigDecimal.ZERO)>0,"批复预算必须大于0");
        award.put("approvedBudget",approvedBudget);
        award.put("approvedScope",value(approval,"approvedScope",p.get("projectScope")));
        award.put("approvedObjectives",value(approval,"approvedObjectives",p.get("objectives")));
        award.put("approvedOutputs",s(approval,"approvedOutputs"));award.put("username",username);
        mapper.insertAward(award);
        Map<String,Object> project=new LinkedHashMap<>();
        project.put("recordId",p.get("recordId"));project.put("proposalId",proposalId);project.put("awardId",award.get("awardId"));
        project.put("projectNo",no("RF"));project.put("projectName",award.get("approvedTitle"));project.put("piUserId",p.get("applicantUserId"));project.put("deptId",p.get("applicantDeptId"));
        project.put("plannedStartDate",award.get("approvedStartDate"));project.put("plannedEndDate",award.get("approvedEndDate"));project.put("currentBudget",award.get("approvedBudget"));project.put("username",username);
        mapper.insertProject(project);
        mapper.copyProposalMembersToProject(proposalId,longValue(project.get("projectId")),username);
        Map<String,Object> budget=new LinkedHashMap<>();budget.put("projectId",project.get("projectId"));budget.put("versionNo",1);budget.put("totalAmount",award.get("approvedBudget"));mapper.insertBudget(budget);
        Long budgetId=longValue(budget.get("budgetId"));
        mapper.copyProposalBudgetToProject(proposalId,budgetId);
        BigDecimal budgetDelta=approvedBudget.subtract(money(mapper.sumProposalBudget(proposalId)));
        if(budgetDelta.compareTo(BigDecimal.ZERO)!=0){
            Map<String,Object> adjustment=new LinkedHashMap<>();
            adjustment.put("budgetId",budgetId);adjustment.put("category","批复调整");adjustment.put("plannedAmount",budgetDelta);adjustment.put("description","立项批复预算与申报预算差额");
            mapper.insertBudgetLine(adjustment);
        }
        mapper.updateRecordPhase(longValue(p.get("recordId")),"PLANNING","ACTIVE");
        completeWorkflow("PROPOSAL",proposalId,userId,"MANAGEMENT","APPROVE",comment);
    }

    @Override
    @Transactional
    public void rejectProposal(Long proposalId,String comment,Long userId,String username){
        Map<String,Object> p=requiredProposal(proposalId);ResearchFlowRules.require("UNDER_REVIEW".equals(s(p,"status")),"当前申请不能驳回");
        mapper.completeReview(proposalId,"REJECT",comment,userId);mapper.updateProposalStatus(proposalId,"REJECTED");
        mapper.updateRecordPhase(longValue(p.get("recordId")),"PROPOSAL","ACTIVE");
        completeWorkflow("PROPOSAL",proposalId,userId,"MANAGEMENT","REJECT",comment);
    }

    @Override
    public void addProposalMember(Long proposalId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProposal(proposalId);checkProposalOwner(p,userId,manageAll);ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"当前申请不可修改团队");
        Map<String,Object> d=new LinkedHashMap<>(input);d.put("proposalId",proposalId);d.put("username",username);mapper.insertProposalMember(d);
    }

    @Override public void saveProposalBudget(Long proposalId,List<Map<String,Object>> lines,Long userId,boolean manageAll){Map<String,Object> p=requiredProposal(proposalId);checkProposalOwner(p,userId,manageAll);ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"当前申请不可修改预算");replaceProposalBudget(proposalId,lines);}
    @Override public void addExpectedOutput(Long proposalId,Map<String,Object> input,Long userId,boolean manageAll){Map<String,Object> p=requiredProposal(proposalId);checkProposalOwner(p,userId,manageAll);ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"当前申请不可修改预期成果");input.put("proposalId",proposalId);mapper.insertExpectedOutput(input);}
    @Override public void attachProposalDocument(Long proposalId,Map<String,Object> input,Long userId,boolean manageAll){Map<String,Object> p=requiredProposal(proposalId);checkProposalOwner(p,userId,manageAll);ResearchFlowRules.require(ResearchFlowRules.editableProposal(s(p,"status")),"当前申请不可修改附件");insertDocument(longValue(p.get("recordId")),"PROPOSAL",proposalId,"APPLICATION",input,userId);}

    @Override public List<Map<String,Object>> listProjects(Long userId,boolean viewAll,String status,String keyword){return mapper.selectProjects(userId,viewAll,status,keyword);}

    @Override
    public Map<String,Object> getProject(Long projectId,Long userId,boolean viewAll){
        Map<String,Object> p=requiredProject(projectId);checkProjectAccess(projectId,p,userId,viewAll);
        Map<String,Object> out=new LinkedHashMap<>(p);
        out.put("members",mapper.selectProjectMembers(projectId));out.put("baselines",mapper.selectBaselines(projectId));out.put("workItems",mapper.selectWorkItems(projectId));
        out.put("progressReports",mapper.selectProgressReports(projectId));out.put("risks",mapper.selectProjectRisks(projectId));out.put("issues",mapper.selectProjectIssues(projectId));out.put("decisions",mapper.selectDecisions(projectId));
        out.put("changes",mapper.selectChanges(projectId));Map<String,Object> budget=mapper.selectCurrentBudget(projectId);out.put("budget",budget);
        out.put("budgetLines",budget==null?List.of():mapper.selectBudgetLines(longValue(budget.get("budgetId"))));out.put("expenses",mapper.selectExpenses(projectId));out.put("outcomes",mapper.selectOutcomes(projectId));
        out.put("documents",mapper.selectDocuments("PROJECT",projectId));out.put("workflow",mapper.selectProjectWorkflow(projectId));out.put("acceptance",mapper.selectAcceptance(projectId));out.put("closeout",mapper.selectCloseout(projectId));
        out.put("expectedOutputs",mapper.selectExpectedOutputs(longValue(p.get("proposalId"))));
        return out;
    }

    @Override public void addProjectMember(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object> p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);ResearchFlowRules.require("PLANNING".equals(s(p,"status")),"仅计划阶段可直接调整团队");Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("username",username);mapper.insertProjectMember(d);}

    @Override
    public void addWorkItem(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);ResearchFlowRules.require("PLANNING".equals(s(p,"status")),"项目激活后计划变更必须走变更申请");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("status","NOT_STARTED");d.put("progress",0);d.put("username",username);mapper.insertWorkItem(d);
    }

    @Override
    @Transactional
    public void workItemAction(Long projectId,Long workItemId,String action,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProject(projectId);ResearchFlowRules.require("ACTIVE".equals(s(p,"status")),"仅执行中的项目可更新工作项");
        Map<String,Object>w=mapper.selectWorkItem(workItemId);ResearchFlowRules.require(w!=null&&projectId.equals(longValue(w.get("projectId"))),"工作项不存在");checkWorkItemManage(projectId,p,w,userId,manageAll);
        ResearchFlowRules.require(ResearchFlowRules.canTransitionWorkItem(action,s(w,"status")),"当前工作项状态不允许该操作");
        String status="start".equals(action)?"IN_PROGRESS":"complete".equals(action)?"DONE":"BLOCKED";Integer progress="complete".equals(action)?100:integer(w.get("progress"),0);
        mapper.updateWorkItemAction(workItemId,status,progress,username);mapper.updateProjectProgressFromWorkItems(projectId);
    }

    @Override
    @Transactional
    public void activateProject(Long projectId,Long userId,boolean manageAll,String username){
        Map<String,Object> p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);ResearchFlowRules.require("PLANNING".equals(s(p,"status")),"仅计划阶段项目可以激活");
        ResearchFlowRules.require(mapper.countWorkItems(projectId)>0,"请至少建立一个工作项或里程碑");
        Map<String,Object> budget=mapper.selectCurrentBudget(projectId);ResearchFlowRules.require(budget!=null&&money(budget.get("totalAmount")).compareTo(BigDecimal.ZERO)>0,"项目预算不能为空");
        Long baselineId=createBaseline(p,"AWARD",longValue(p.get("awardId")),userId);
        mapper.updateProjectBaseline(projectId,baselineId,"ACTIVE");mapper.updateRecordPhase(longValue(p.get("recordId")),"EXECUTION","ACTIVE");
    }

    @Override public void addProgressReport(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);ResearchFlowRules.require("ACTIVE".equals(s(p,"status")),"仅执行中项目可提交进展");Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("reportNo",no("RPT"));d.put("preparedBy",userId);d.put("status","SUBMITTED");d.put("username",username);mapper.insertProgressReport(d);}
    @Override
    public void addExpense(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectFinance(projectId,p,userId,manageAll);ResearchFlowRules.require("ACTIVE".equals(s(p,"status")),"仅执行中项目可记录支出");
        BigDecimal amount=money(input.get("amount"));ResearchFlowRules.require(amount.compareTo(BigDecimal.ZERO)>0,"支出金额必须大于0");
        Long budgetLineId=longValue(input.get("budgetLineId"));ResearchFlowRules.require(budgetLineId!=null,"请选择预算科目");
        Map<String,Object>budget=mapper.selectCurrentBudget(projectId);ResearchFlowRules.require(budget!=null,"当前项目没有有效预算");
        Map<String,Object>line=mapper.selectBudgetLine(budgetLineId);ResearchFlowRules.require(line!=null&&longValue(line.get("budgetId")).equals(longValue(budget.get("budgetId"))),"预算科目不属于当前预算版本");
        ResearchFlowRules.require(mapper.sumExpenses(projectId).add(amount).compareTo(money(budget.get("totalAmount")))<=0,"本次支出将超过项目总预算");
        BigDecimal planned=money(line.get("plannedAmount"));ResearchFlowRules.require(planned.compareTo(BigDecimal.ZERO)>0,"该预算科目不可记录支出");
        ResearchFlowRules.require(mapper.sumExpensesByBudgetLine(budgetLineId).add(amount).compareTo(planned)<=0,"本次支出将超过该预算科目额度");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("expenseNo",no("EXP"));d.put("username",username);mapper.insertExpense(d);
    }
    @Override public void addOutcome(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);ResearchFlowRules.require(List.of("ACTIVE","CLOSING").contains(s(p,"status")),"当前项目不可登记成果");Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.putIfAbsent("status","COMPLETED");d.put("username",username);mapper.insertOutcome(d);}
    @Override public void attachProjectDocument(Long projectId,Map<String,Object> input,Long userId,boolean manageAll){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);insertDocument(longValue(p.get("recordId")),"PROJECT",projectId,s(input,"category").isEmpty()?"GENERAL":s(input,"category"),input,userId);}

    @Override
    public void addRisk(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);
        int probability=integer(input.get("probability"),1),impact=integer(input.get("impact"),1);ResearchFlowRules.require(probability>=1&&probability<=5&&impact>=1&&impact<=5,"概率和影响必须在1-5之间");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("riskNo","RISK-"+String.format("%04d",mapper.selectProjectRisks(projectId).size()+1));d.put("score",probability*impact);d.put("riskLevel",ResearchFlowRules.riskLevel(probability,impact));d.put("source",s(input,"source").isEmpty()?"MANUAL":s(input,"source"));d.put("status","OPEN");d.put("username",username);mapper.insertRisk(d);
    }
    @Override public void updateRiskStatus(Long projectId,Long riskId,String status,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);Map<String,Object>r=mapper.selectRisk(riskId);ResearchFlowRules.require(r!=null&&projectId.equals(longValue(r.get("projectId"))),"风险不存在");ResearchFlowRules.require(List.of("OPEN","MONITORING","CLOSED").contains(status),"非法风险状态");mapper.updateRiskStatus(riskId,status,username);}
    @Override @Transactional public Long convertRiskToIssue(Long projectId,Long riskId,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);Map<String,Object>r=mapper.selectRisk(riskId);ResearchFlowRules.require(r!=null&&projectId.equals(longValue(r.get("projectId"))),"风险不存在");ResearchFlowRules.require(!"CLOSED".equals(s(r,"status")),"已关闭风险不能转为问题");mapper.updateRiskStatus(riskId,"OCCURRED",username);Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("sourceRiskId",riskId);d.put("issueNo","ISS-"+String.format("%04d",mapper.selectProjectIssues(projectId).size()+1));d.putIfAbsent("title",r.get("title"));d.putIfAbsent("description",r.get("description"));d.putIfAbsent("severity",r.get("riskLevel"));d.put("status","OPEN");d.put("username",username);mapper.insertIssue(d);return longValue(d.get("issueId"));}
    @Override public void addIssue(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("issueNo","ISS-"+String.format("%04d",mapper.selectProjectIssues(projectId).size()+1));d.put("status","OPEN");d.put("username",username);mapper.insertIssue(d);}
    @Override public void updateIssueStatus(Long projectId,Long issueId,String status,Map<String,Object> input,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);Map<String,Object>i=mapper.selectIssue(issueId);ResearchFlowRules.require(i!=null&&projectId.equals(longValue(i.get("projectId"))),"问题不存在");ResearchFlowRules.require(List.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status),"非法问题状态");mapper.updateIssueStatus(issueId,status,s(input,"resolution"),username);}
    @Override
    public void addDecision(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectContributor(projectId,p,userId,manageAll);
        ResearchFlowRules.require(List.of("ACTIVE","CLOSING").contains(s(p,"status")),"当前项目不可记录决策");
        ResearchFlowRules.require(StringUtils.isNotEmpty(s(input,"title"))&&StringUtils.isNotEmpty(s(input,"decision")),"决策标题和结论不能为空");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("decisionNo",no("DEC"));d.put("decisionMakerUserId",userId);d.put("username",username);mapper.insertDecision(d);
    }

    @Override public Map<String,Object> risksAndIssues(Long userId,boolean viewAll){Map<String,Object>out=new LinkedHashMap<>();out.put("risks",mapper.selectAllRisks(userId,viewAll));out.put("issues",mapper.selectAllIssues(userId,viewAll));return out;}

    @Override
    @Transactional
    public Long createChange(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);ResearchFlowRules.require("ACTIVE".equals(s(p,"status")),"仅执行中项目可发起变更");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("changeNo",no("CR"));d.put("applicantUserId",userId);d.put("status","DRAFT");d.put("username",username);mapper.insertChange(d);
        for(Map<String,Object> item:list(input.get("items"))){item.put("changeId",d.get("changeId"));mapper.insertChangeItem(item);}return longValue(d.get("changeId"));
    }
    @Override @Transactional public void submitChange(Long projectId,Long changeId,Long userId,boolean manageAll,String username){Map<String,Object>p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);Map<String,Object>c=requiredChange(projectId,changeId);ResearchFlowRules.require("DRAFT".equals(s(c,"status")),"仅草稿变更可以提交");ResearchFlowRules.require(!mapper.selectChangeItems(changeId).isEmpty(),"至少需要一个变更项");mapper.updateChangeStatus(changeId,"SUBMITTED",username);Long wf=startWorkflow("CHANGE_APPROVAL","CHANGE_REQUEST",changeId,userId,"MANAGEMENT");workflowAction(wf,"SUBMIT","SUBMIT",userId,"提交变更申请");}
    @Override @Transactional public void approveChange(Long projectId,Long changeId,String comment,Long userId,String username){requiredProject(projectId);Map<String,Object>c=requiredChange(projectId,changeId);ResearchFlowRules.require("SUBMITTED".equals(s(c,"status")),"仅已提交变更可以审批");mapper.updateChangeStatus(changeId,"APPROVED",username);completeWorkflow("CHANGE_REQUEST",changeId,userId,"MANAGEMENT","APPROVE",comment);}
    @Override @Transactional public void rejectChange(Long projectId,Long changeId,String comment,Long userId,String username){requiredProject(projectId);Map<String,Object>c=requiredChange(projectId,changeId);ResearchFlowRules.require("SUBMITTED".equals(s(c,"status")),"仅已提交变更可以审批");mapper.updateChangeStatus(changeId,"REJECTED",username);completeWorkflow("CHANGE_REQUEST",changeId,userId,"MANAGEMENT","REJECT",comment);}
    @Override
    @Transactional
    public void applyChange(Long projectId,Long changeId,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);Map<String,Object>c=requiredChange(projectId,changeId);ResearchFlowRules.require("APPROVED".equals(s(c,"status")),"仅已批准变更可以应用");
        String end=p.get("plannedEndDate")==null?null:p.get("plannedEndDate").toString();
        BigDecimal originalBudget=money(p.get("currentBudget"));
        BigDecimal budget=originalBudget;
        for(Map<String,Object> item:mapper.selectChangeItems(changeId)){
            String field=s(item,"fieldCode");
            if("planned_end_date".equals(field)) end=s(item,"afterValue");
            if("current_budget".equals(field)||"total_budget".equals(field)) budget=money(item.get("afterValue"));
        }
        mapper.updateProjectCurrentPlan(projectId,end,budget);
        Long newBudgetId=null;
        if(budget.compareTo(originalBudget)!=0){
            Map<String,Object> previous=mapper.selectCurrentBudget(projectId);
            int nextVersion=mapper.nextBudgetVersion(projectId)+1;
            mapper.deactivateBudgets(projectId);
            Map<String,Object> versioned=new LinkedHashMap<>();
            versioned.put("projectId",projectId);versioned.put("versionNo",nextVersion);versioned.put("totalAmount",budget);
            mapper.insertBudget(versioned);
            newBudgetId=longValue(versioned.get("budgetId"));
            if(previous!=null){
                mapper.copyBudgetLines(longValue(previous.get("budgetId")),newBudgetId);
                BigDecimal delta=budget.subtract(originalBudget);
                if(delta.compareTo(BigDecimal.ZERO)!=0){
                    Map<String,Object> adjustment=new LinkedHashMap<>();
                    adjustment.put("budgetId",newBudgetId);adjustment.put("category","预算调整");adjustment.put("plannedAmount",delta);adjustment.put("description","由 "+s(c,"changeNo")+" 批准变更产生");
                    mapper.insertBudgetLine(adjustment);
                }
            }
        }
        p=requiredProject(projectId);
        Long baselineId=createBaseline(p,"CHANGE_REQUEST",changeId,userId);
        mapper.updateProjectBaseline(projectId,baselineId,"ACTIVE");
        if(newBudgetId!=null) mapper.updateBudgetBaseline(newBudgetId,baselineId);
        mapper.updateChangeStatus(changeId,"APPLIED",username);
    }

    @Override
    @Transactional
    public void submitAcceptance(Long projectId,Map<String,Object> input,Long userId,boolean manageAll,String username){
        Map<String,Object>p=requiredProject(projectId);checkProjectManage(projectId,p,userId,manageAll);ResearchFlowRules.require("ACTIVE".equals(s(p,"status")),"仅执行中项目可提交验收");
        ResearchFlowRules.require(mapper.countBlockingIssues(projectId)==0,"仍有高严重度未关闭问题，暂不能提交验收");
        ResearchFlowRules.require(mapper.countIncompleteWorkItems(projectId)==0,"仍有未完成任务或里程碑，暂不能提交验收");
        ResearchFlowRules.require(mapper.countOutcomeGaps(projectId)==0,"计划成果尚未全部完成，暂不能提交验收");
        Map<String,Object>existing=mapper.selectAcceptance(projectId);
        ResearchFlowRules.require(existing==null||"RETURNED".equals(s(existing,"status")),"该项目已存在待处理或已完成的验收申请");
        Map<String,Object>d=new LinkedHashMap<>(input);d.put("projectId",projectId);d.put("applicantUserId",userId);d.put("status","SUBMITTED");
        if(existing==null){d.put("acceptanceNo",no("ACC"));mapper.insertAcceptance(d);}
        else{d.put("acceptanceId",existing.get("acceptanceId"));d.put("acceptanceNo",existing.get("acceptanceNo"));ResearchFlowRules.require(mapper.resubmitAcceptance(d)==1,"验收申请状态已变化，请刷新后重试");}
        Long acceptanceId=existing==null?longValue(d.get("acceptanceId")):longValue(existing.get("acceptanceId"));
        Long wf=startWorkflow("ACCEPTANCE","ACCEPTANCE",acceptanceId,userId,"MANAGEMENT");workflowAction(wf,"SUBMIT","SUBMIT",userId,existing==null?"提交项目验收":"重新提交项目验收");
    }
    @Override
    @Transactional
    public void reviewAcceptance(Long projectId,boolean approved,String comment,Long userId,String username){
        Map<String,Object>a=mapper.selectAcceptance(projectId);ResearchFlowRules.require(a!=null&&"SUBMITTED".equals(s(a,"status")),"当前没有待审批验收");
        String status=approved?"APPROVED":"RETURNED";mapper.updateAcceptance(projectId,status,comment);completeWorkflow("ACCEPTANCE",longValue(a.get("acceptanceId")),userId,"MANAGEMENT",approved?"APPROVE":"RETURN",comment);
        if(approved){mapper.updateProjectStatus(projectId,"CLOSING");Map<String,Object>p=requiredProject(projectId);mapper.updateRecordPhase(longValue(p.get("recordId")),"CLOSEOUT","ACTIVE");Map<String,Object>c=new LinkedHashMap<>();c.put("projectId",projectId);c.put("acceptanceId",a.get("acceptanceId"));mapper.insertCloseout(c);}
    }
    @Override
    @Transactional
    public void completeCloseout(Long projectId,Map<String,Object> input,Long userId,String username){
        Map<String,Object>p=requiredProject(projectId);ResearchFlowRules.require("CLOSING".equals(s(p,"status")),"项目尚未进入结项阶段");
        Map<String,Object>a=mapper.selectAcceptance(projectId);ResearchFlowRules.require(a!=null&&"APPROVED".equals(s(a,"status")),"项目验收尚未通过");
        ResearchFlowRules.require(mapper.countBlockingIssues(projectId)==0,"仍有高严重度未关闭问题");
        ResearchFlowRules.require(mapper.countIncompleteWorkItems(projectId)==0,"仍有未完成任务或里程碑");
        ResearchFlowRules.require(mapper.countOutcomeGaps(projectId)==0,"计划成果尚未全部完成");
        ResearchFlowRules.require(mapper.countProjectDocuments(projectId)>0,"至少需要一份项目归档资料");
        ResearchFlowRules.require(mapper.sumExpenses(projectId).compareTo(money(p.get("currentBudget")))<=0,"项目支出超过当前预算，不能结项");
        Map<String,Object>closeout=mapper.selectCloseout(projectId);ResearchFlowRules.require(closeout!=null&&"PENDING".equals(s(closeout,"status")),"结项记录不存在或已完成");
        String conclusion=s(input,"conclusion");ResearchFlowRules.require(StringUtils.isNotEmpty(conclusion),"请填写结项结论");
        mapper.completeCloseout(projectId,conclusion,userId);mapper.updateProjectStatus(projectId,"CLOSED");mapper.updateRecordPhase(longValue(p.get("recordId")),"CLOSED","CLOSED");
    }

    @Override
    public Map<String,Object> approvalCenter(){Map<String,Object>out=new LinkedHashMap<>();out.put("proposals",mapper.selectPendingProposalReviews());out.put("changes",mapper.selectPendingChanges());out.put("acceptances",mapper.selectPendingAcceptances());return out;}

    private Long createBaseline(Map<String,Object> project,String sourceType,Long sourceId,Long userId){
        Long projectId=longValue(project.get("projectId"));Map<String,Object>b=new LinkedHashMap<>();b.put("projectId",projectId);b.put("versionNo",mapper.nextBaselineVersion(projectId)+1);b.put("sourceType",sourceType);b.put("sourceId",sourceId);b.put("plannedStartDate",project.get("plannedStartDate"));b.put("plannedEndDate",project.get("plannedEndDate"));b.put("approvedBudget",project.get("currentBudget"));
        Map<String,Object> award=mapper.selectAward(longValue(project.get("awardId")));b.put("scopeSnapshot",toJson(Map.of("scope",nullToEmpty(award==null?null:award.get("approvedScope")))));b.put("objectiveSnapshot",toJson(Map.of("objectives",nullToEmpty(award==null?null:award.get("approvedObjectives")))));b.put("outputSnapshot",toJson(mapper.selectExpectedOutputs(longValue(project.get("proposalId")))));b.put("workPlanSnapshot",toJson(mapper.selectWorkItems(projectId)));Map<String,Object> budget=mapper.selectCurrentBudget(projectId);b.put("budgetSnapshot",toJson(budget==null?Map.of():Map.of("budget",budget,"lines",mapper.selectBudgetLines(longValue(budget.get("budgetId"))))));b.put("createdByUserId",userId);mapper.insertBaseline(b);return longValue(b.get("baselineId"));
    }
    private void replaceProposalBudget(Long proposalId,List<Map<String,Object>> lines){mapper.deleteProposalBudget(proposalId);for(Map<String,Object>line:lines){line.put("proposalId",proposalId);mapper.insertProposalBudgetLine(line);}}
    private void replaceExpectedOutputs(Long proposalId,List<Map<String,Object>> outputs){mapper.deleteExpectedOutputs(proposalId);for(Map<String,Object>output:outputs){output.put("proposalId",proposalId);mapper.insertExpectedOutput(output);}}
    private void insertDocument(Long recordId,String type,Long businessId,String category,Map<String,Object>d,Long userId){Map<String,Object>x=new LinkedHashMap<>(d);x.put("recordId",recordId);x.put("businessType",type);x.put("businessId",businessId);x.put("category",category);x.put("storageProvider","LOCAL");x.put("uploadedBy",userId);mapper.insertDocument(x);}
    private Long startWorkflow(String wfType,String bizType,Long bizId,Long userId,String step){Map<String,Object>w=new LinkedHashMap<>();w.put("workflowType",wfType);w.put("businessType",bizType);w.put("businessId",bizId);w.put("status","RUNNING");w.put("currentStep",step);w.put("startedBy",userId);mapper.insertWorkflow(w);return longValue(w.get("workflowId"));}
    private void workflowAction(Long wfId,String step,String action,Long userId,String comment){Map<String,Object>a=new LinkedHashMap<>();a.put("workflowId",wfId);a.put("stepCode",step);a.put("action",action);a.put("operatorUserId",userId);a.put("comment",comment);mapper.insertWorkflowAction(a);}
    private void completeWorkflow(String bizType,Long bizId,Long userId,String step,String action,String comment){Long wf=mapper.selectActiveWorkflowId(bizType,bizId);if(wf!=null){workflowAction(wf,step,action,userId,comment);mapper.updateWorkflow(bizType,bizId,"COMPLETED",step);}}
    private Map<String,Object> requiredProposal(Long id){Map<String,Object>p=mapper.selectProposal(id);ResearchFlowRules.require(p!=null,"项目申请不存在");return p;}
    private Map<String,Object> requiredProject(Long id){Map<String,Object>p=mapper.selectProject(id);ResearchFlowRules.require(p!=null,"科研项目不存在");return p;}
    private Map<String,Object> requiredChange(Long projectId,Long id){Map<String,Object>c=mapper.selectChange(id);ResearchFlowRules.require(c!=null&&projectId.equals(longValue(c.get("projectId"))),"变更申请不存在");return c;}
    private void checkProposalAccess(Map<String,Object>p,Long userId,boolean viewAll){ResearchFlowRules.require(viewAll||userId.equals(longValue(p.get("applicantUserId"))),"无权查看该申请");}
    private void checkProposalOwner(Map<String,Object>p,Long userId,boolean manageAll){ResearchFlowRules.require(manageAll||userId.equals(longValue(p.get("applicantUserId"))),"无权修改该申请");}
    private void checkProjectAccess(Long projectId,Map<String,Object>p,Long userId,boolean viewAll){ResearchFlowRules.require(viewAll||userId.equals(longValue(p.get("piUserId")))||mapper.isProjectMember(projectId,userId)>0,"无权查看该项目");}
    private void checkProjectManage(Long projectId,Map<String,Object>p,Long userId,boolean manageAll){String role=mapper.selectProjectMemberRole(projectId,userId);ResearchFlowRules.require(manageAll||userId.equals(longValue(p.get("piUserId")))||List.of("PI","PROJECT_MANAGER").contains(role),"仅PI或项目经理可以执行该操作");}
    private void checkProjectContributor(Long projectId,Map<String,Object>p,Long userId,boolean manageAll){ResearchFlowRules.require(manageAll||userId.equals(longValue(p.get("piUserId")))||mapper.isProjectMember(projectId,userId)>0,"仅项目成员可以执行该操作");}
    private void checkProjectFinance(Long projectId,Map<String,Object>p,Long userId,boolean manageAll){String role=mapper.selectProjectMemberRole(projectId,userId);ResearchFlowRules.require(manageAll||userId.equals(longValue(p.get("piUserId")))||List.of("PI","PROJECT_MANAGER","FINANCE_CONTACT").contains(role),"无权维护项目经费");}
    private void checkWorkItemManage(Long projectId,Map<String,Object>p,Map<String,Object>w,Long userId,boolean manageAll){String role=mapper.selectProjectMemberRole(projectId,userId);ResearchFlowRules.require(manageAll||userId.equals(longValue(p.get("piUserId")))||List.of("PI","PROJECT_MANAGER").contains(role)||userId.equals(longValue(w.get("ownerUserId"))),"仅项目经理或任务负责人可以更新工作项");}
    private long countStatus(List<Map<String,Object>> rows,String status){return rows.stream().filter(x->status.equals(s(x,"status"))).count();}
    private int percent(BigDecimal a,BigDecimal b){return b==null||b.compareTo(BigDecimal.ZERO)==0?0:a.multiply(BigDecimal.valueOf(100)).divide(b,0,java.math.RoundingMode.HALF_UP).intValue();}
    private static String no(String prefix){String time=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));return prefix+"-"+time+"-"+ThreadLocalRandom.current().nextInt(100,1000);}
    private static String s(Map<String,?>m,String k){Object v=m==null?null:m.get(k);return v==null?"":String.valueOf(v);}
    private static Object value(Map<String,Object>m,String k,Object fallback){Object v=m==null?null:m.get(k);return v==null||String.valueOf(v).isBlank()?fallback:v;}
    private static Long longValue(Object v){if(v==null)return null;if(v instanceof Number n)return n.longValue();return Long.valueOf(String.valueOf(v));}
    private static int integer(Object v,int fallback){if(v==null)return fallback;if(v instanceof Number n)return n.intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return fallback;}}
    private static BigDecimal money(Object v){if(v==null||String.valueOf(v).isBlank())return BigDecimal.ZERO;if(v instanceof BigDecimal b)return b;if(v instanceof Number n)return BigDecimal.valueOf(n.doubleValue());return new BigDecimal(String.valueOf(v));}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> list(Object v){if(v instanceof List<?> l)return (List<Map<String,Object>>)(List<?>)l;return new ArrayList<>();}
    private String toJson(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new ServiceException("生成基线快照失败");}}
    private static String nullToEmpty(Object v){return v==null?"":String.valueOf(v);}
    private static void miss(List<Map<String,String>> list,boolean condition,String code,String message){if(condition)list.add(msg(code,message));}
    private static Map<String,String> msg(String code,String message){Map<String,String>m=new LinkedHashMap<>();m.put("code",code);m.put("message",message);return m;}
}
