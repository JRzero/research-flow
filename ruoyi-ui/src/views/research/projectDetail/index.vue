<template>
  <div v-if="project.projectId" class="workspace" v-loading="loading">
    <div class="project-bar">
      <div class="identity">
        <el-button text @click="router.push('/research/projects')"><el-icon><ArrowLeft /></el-icon></el-button>
        <div class="identity-copy">
          <div class="title-line">
            <strong>{{ project.projectName }}</strong>
            <span :class="['status',project.status?.toLowerCase()]">{{ statusText(project.status) }}</span>
          </div>
          <small>{{ project.projectNo }} · {{ project.piName }} · {{ project.deptName || '-' }} · Baseline V{{ baseline?.versionNo || '-' }}</small>
        </div>
      </div>
      <div class="actions">
        <el-button v-if="canActivate" type="primary" @click="activate">建立基线并启动</el-button>
        <el-button v-if="canOperate && project.status==='ACTIVE'" @click="reportVisible=true">进展报告</el-button>
        <el-button v-if="canOperate && project.status==='ACTIVE'" @click="changeVisible=true">申请变更</el-button>
        <el-button v-if="canOperate && project.status==='ACTIVE'" type="success" plain @click="acceptanceVisible=true">提交验收</el-button>
      </div>
    </div>

    <div class="kpi-strip">
      <div><span>当前进度</span><strong>{{ project.progress || 0 }}%</strong></div>
      <div><span>当前预算</span><strong>¥{{ money(project.currentBudget) }}</strong></div>
      <div><span>预算执行</span><strong>{{ health.budgetExecutionRate || 0 }}%</strong></div>
      <div><span>高风险</span><strong :class="{danger:health.criticalRisks}">{{ health.criticalRisks || 0 }}</strong></div>
      <div><span>开放问题</span><strong :class="{warning:health.openIssues}">{{ health.openIssues || 0 }}</strong></div>
      <div><span>计划结束</span><strong class="date">{{ project.plannedEndDate || '-' }}</strong></div>
    </div>

    <section class="workspace-panel">
      <el-tabs v-model="activeTab" class="compact-tabs">
        <el-tab-pane label="概览" name="overview">
          <div class="overview-grid">
            <section class="block">
              <div class="block-head"><h3>立项信息</h3><span>{{ project.awardNo }}</span></div>
              <dl class="definition">
                <div><dt>批准目标</dt><dd>{{ project.approvedObjectives || '-' }}</dd></div>
                <div><dt>批准范围</dt><dd>{{ project.approvedScope || '-' }}</dd></div>
                <div><dt>批准成果</dt><dd>{{ project.approvedOutputs || '-' }}</dd></div>
                <div><dt>成功标准</dt><dd>{{ project.successCriteria || '-' }}</dd></div>
              </dl>
            </section>
            <section class="block">
              <div class="block-head"><h3>团队</h3><span>{{ members.length }} 人</span></div>
              <div class="compact-list">
                <div v-for="m in members" :key="m.memberId" class="list-row">
                  <span class="avatar">{{ (m.userName||'研').slice(0,1) }}</span>
                  <strong>{{ m.userName }}</strong><span>{{ memberRole(m.memberRole) }}</span><small>{{ m.responsibility || '-' }}</small>
                </div>
              </div>
            </section>
            <section class="block">
              <div class="block-head"><h3>最近进展</h3><span>{{ reports[0]?.submittedAt || '暂无' }}</span></div>
              <div v-if="reports.length" class="report-summary">
                <strong>{{ reportType(reports[0].reportType) }} · {{ reports[0].overallProgress }}%</strong>
                <p>{{ reports[0].completedWork || '暂无完成工作说明' }}</p>
                <small v-if="reports[0].problems">问题：{{ reports[0].problems }}</small>
              </div>
              <el-empty v-else description="暂无进展报告" :image-size="52" />
            </section>
            <section class="block">
              <div class="block-head"><h3>当前基线</h3><span>不可变快照</span></div>
              <dl class="mini-definition">
                <div><dt>版本</dt><dd>V{{ baseline?.versionNo || '-' }}</dd></div>
                <div><dt>来源</dt><dd>{{ baselineSource(baseline?.sourceType) }}</dd></div>
                <div><dt>周期</dt><dd>{{ baseline?.plannedStartDate || '-' }} — {{ baseline?.plannedEndDate || '-' }}</dd></div>
                <div><dt>预算</dt><dd>¥{{ money(baseline?.approvedBudget) }}</dd></div>
              </dl>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="计划" name="plan">
          <div class="tab-toolbar">
            <div><strong>WBS / 工作项</strong><span>PHASE、WORK PACKAGE、TASK、MILESTONE 使用统一模型。</span></div>
            <el-button v-if="canPlan" type="primary" plain @click="workVisible=true">新增工作项</el-button>
          </div>
          <el-table :data="workItems" size="small" stripe row-key="workItemId">
            <el-table-column prop="wbsCode" label="WBS" width="90" />
            <el-table-column label="类型" width="105"><template #default="{row}">{{ itemType(row.itemType) }}</template></el-table-column>
            <el-table-column prop="title" label="工作项" min-width="240" show-overflow-tooltip />
            <el-table-column prop="ownerName" label="负责人" width="90" />
            <el-table-column label="计划周期" width="190"><template #default="{row}">{{ row.plannedStartDate||'-' }} — {{ row.plannedEndDate||'-' }}</template></el-table-column>
            <el-table-column label="进度" width="130"><template #default="{row}"><div class="progress-cell"><el-progress :percentage="row.progress||0" :stroke-width="5" :show-text="false"/><span>{{ row.progress||0 }}%</span></div></template></el-table-column>
            <el-table-column label="状态" width="105"><template #default="{row}"><span class="mini-status">{{ workStatus(row.status) }}</span></template></el-table-column>
            <el-table-column v-if="project.status==='ACTIVE' && canOperate" label="动作" width="165" fixed="right">
              <template #default="{row}">
                <el-button v-if="row.status==='NOT_STARTED'" text type="primary" @click="workAction(row,'start')">开始</el-button>
                <el-button v-if="['NOT_STARTED','IN_PROGRESS'].includes(row.status)" text type="danger" @click="workAction(row,'block')">阻塞</el-button>
                <el-button v-if="['IN_PROGRESS','BLOCKED'].includes(row.status)" text type="success" @click="workAction(row,'complete')">完成</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="baseline-history">
            <span>基线历史</span>
            <button v-for="b in baselines" :key="b.baselineId" type="button" class="baseline-chip">V{{ b.versionNo }} · {{ baselineSource(b.sourceType) }} · {{ b.effectiveAt }}</button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="执行" name="execution">
          <div class="tab-toolbar">
            <div><strong>进展报告</strong><span>月报、季报、年度报告、中期检查统一记录。</span></div>
            <el-button v-if="canOperate && project.status==='ACTIVE'" type="primary" plain @click="reportVisible=true">新增报告</el-button>
          </div>
          <el-table :data="reports" size="small" stripe>
            <el-table-column label="类型" width="100"><template #default="{row}">{{ reportType(row.reportType) }}</template></el-table-column>
            <el-table-column label="报告期" width="190"><template #default="{row}">{{ row.periodStart||'-' }} — {{ row.periodEnd||'-' }}</template></el-table-column>
            <el-table-column prop="overallProgress" label="完成度" width="80" align="center" />
            <el-table-column prop="completedWork" label="完成工作" min-width="260" show-overflow-tooltip />
            <el-table-column prop="problems" label="主要问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="submittedAt" label="提交时间" width="165" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="治理" name="governance">
          <div class="governance-grid">
            <section class="block full">
              <div class="tab-toolbar inner">
                <div><strong>风险登记册</strong><span>风险是可能发生，问题是已经发生。</span></div>
                <el-button v-if="canOperate && project.status==='ACTIVE'" text type="primary" @click="riskVisible=true">+ 风险</el-button>
              </div>
              <el-table :data="risks" size="small">
                <el-table-column label="等级" width="80"><template #default="{row}"><span :class="['risk-level',row.riskLevel?.toLowerCase()]">{{ riskText(row.riskLevel) }}</span></template></el-table-column>
                <el-table-column prop="riskNo" label="编号" width="105" />
                <el-table-column prop="title" label="风险" min-width="220" />
                <el-table-column label="P×I" width="75" align="center"><template #default="{row}">{{ row.probability }}×{{ row.impact }}</template></el-table-column>
                <el-table-column prop="ownerName" label="负责人" width="90" />
                <el-table-column prop="status" label="状态" width="95" />
                <el-table-column v-if="canOperate && project.status==='ACTIVE'" label="" width="100"><template #default="{row}"><el-button v-if="['OPEN','MONITORING'].includes(row.status)" text type="danger" @click="occurRisk(row)">已发生</el-button></template></el-table-column>
              </el-table>
            </section>
            <section class="block full">
              <div class="tab-toolbar inner">
                <div><strong>Issue Log</strong><span>正式跟踪已经发生、需要责任人解决的问题。</span></div>
                <el-button v-if="canOperate && project.status==='ACTIVE'" text type="primary" @click="issueVisible=true">+ 问题</el-button>
              </div>
              <el-table :data="issues" size="small">
                <el-table-column prop="issueNo" label="编号" width="100" />
                <el-table-column prop="title" label="问题" min-width="220" />
                <el-table-column prop="severity" label="严重度" width="90" />
                <el-table-column prop="ownerName" label="负责人" width="90" />
                <el-table-column prop="dueDate" label="解决期限" width="115" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column v-if="canOperate && project.status==='ACTIVE'" label="" width="100"><template #default="{row}"><el-button v-if="['OPEN','IN_PROGRESS'].includes(row.status)" text type="success" @click="resolve(row)">解决</el-button></template></el-table-column>
              </el-table>
            </section>
            <section class="block full">
              <div class="tab-toolbar inner">
                <div><strong>变更控制</strong><span>批准和应用分离，应用变更会生成新的项目基线。</span></div>
                <el-button v-if="canOperate && project.status==='ACTIVE'" text type="primary" @click="changeVisible=true">+ 变更</el-button>
              </div>
              <el-table :data="changes" size="small">
                <el-table-column prop="changeNo" label="变更编号" width="150" />
                <el-table-column prop="title" label="变更" min-width="220" />
                <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
                <el-table-column label="变更项" min-width="220"><template #default="{row}"><span class="change-items">{{ changeItems(row) }}</span></template></el-table-column>
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column v-if="canOperate || isAdmin" label="操作" width="195">
                  <template #default="{row}">
                    <el-button v-if="row.status==='DRAFT'&&canOperate" text type="primary" @click="submitChange(row)">提交</el-button>
                    <el-button v-if="row.status==='SUBMITTED'&&isAdmin" text type="success" @click="reviewChangeRow(row,true)">批准</el-button>
                    <el-button v-if="row.status==='SUBMITTED'&&isAdmin" text type="danger" @click="reviewChangeRow(row,false)">拒绝</el-button>
                    <el-button v-if="row.status==='APPROVED'&&canOperate" text type="primary" @click="applyChange(row)">应用</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="经费" name="finance">
          <div class="finance-summary">
            <div><span>当前预算</span><strong>¥{{ money(project.currentBudget) }}</strong></div>
            <div><span>累计支出</span><strong>¥{{ money(health.usedBudget) }}</strong></div>
            <div><span>执行率</span><strong>{{ health.budgetExecutionRate||0 }}%</strong></div>
            <el-button v-if="canOperate && project.status==='ACTIVE'" type="primary" plain @click="expenseVisible=true">记录支出</el-button>
          </div>
          <div class="finance-grid">
            <section class="block"><div class="block-head"><h3>预算科目</h3></div><el-table :data="budgetLines" size="small"><el-table-column prop="category" label="科目"/><el-table-column label="预算" align="right"><template #default="{row}">¥{{ money(row.plannedAmount) }}</template></el-table-column><el-table-column label="已使用" align="right"><template #default="{row}">¥{{ money(row.usedAmount) }}</template></el-table-column></el-table></section>
            <section class="block"><div class="block-head"><h3>支出记录</h3></div><el-table :data="expenses" size="small"><el-table-column prop="expenseDate" label="日期" width="110"/><el-table-column prop="category" label="科目" width="90"/><el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip/><el-table-column label="金额" width="110" align="right"><template #default="{row}">¥{{ money(row.amount) }}</template></el-table-column></el-table></section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="成果" name="outcomes">
          <div class="tab-toolbar">
            <div><strong>计划成果 vs 实际成果</strong><span>计划成果来自申报 / 批复，实际成果独立登记。</span></div>
            <el-button v-if="canOperate && ['ACTIVE','CLOSING'].includes(project.status)" type="primary" plain @click="outcomeVisible=true">登记成果</el-button>
          </div>
          <div class="outcome-grid">
            <section class="block"><div class="block-head"><h3>计划成果</h3><span>{{ expectedOutputs.length }}</span></div><div class="compact-list"><div v-for="o in expectedOutputs" :key="o.expectedOutputId" class="output-row"><span>{{ outputType(o.outputType) }}</span><strong>{{ o.name }}</strong><small>目标 {{ o.targetQuantity }}</small></div></div></section>
            <section class="block"><div class="block-head"><h3>实际成果</h3><span>{{ outcomes.length }}</span></div><div class="compact-list"><div v-for="o in outcomes" :key="o.outcomeId" class="output-row"><span>{{ outputType(o.outcomeType) }}</span><strong>{{ o.name }}</strong><small>{{ o.completedDate || '-' }}</small></div></div></section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="结项" name="closeout">
          <div class="closeout-grid">
            <section class="block">
              <div class="block-head"><h3>验收</h3><span>{{ acceptance ? acceptanceStatus(acceptance.status) : '未提交' }}</span></div>
              <template v-if="acceptance">
                <p class="copy"><strong>项目总结：</strong>{{ acceptance.projectSummary }}</p>
                <p class="copy"><strong>完成情况：</strong>{{ acceptance.completionStatement || '-' }}</p>
                <p v-if="acceptance.reviewComment" class="copy"><strong>验收意见：</strong>{{ acceptance.reviewComment }}</p>
                <div v-if="isAdmin&&acceptance.status==='SUBMITTED'" class="inline-actions"><el-button @click="reviewAcceptance(false)">退回</el-button><el-button type="success" @click="reviewAcceptance(true)">验收通过</el-button></div>
              </template>
              <el-empty v-else description="尚未提交验收" :image-size="55" />
            </section>
            <section class="block">
              <div class="block-head"><h3>Closeout</h3><span>{{ closeout?.status || '未开始' }}</span></div>
              <template v-if="closeout"><p class="copy">{{ closeout.conclusion || '项目已完成结项归档。' }}</p><p class="copy">完成时间：{{ closeout.completedAt }}</p></template>
              <template v-else>
                <p class="copy muted">验收通过后，科研管理员完成最终报告、经费、成果、文档和归档检查，项目才进入 CLOSED。</p>
                <el-button v-if="isAdmin&&acceptance?.status==='APPROVED'&&project.status==='CLOSING'" type="primary" @click="closeoutVisible=true">完成结项</el-button>
              </template>
            </section>
            <section class="block full">
              <div class="block-head"><h3>流程审计</h3><span>{{ workflow.length }} 条</span></div>
              <div class="audit-list"><div v-for="a in workflow" :key="a.actionId" class="audit-row"><span>{{ a.actedAt }}</span><strong>{{ a.action }}</strong><span>{{ a.operatorName }}</span><small>{{ a.comment || '-' }}</small></div></div>
            </section>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="workVisible" title="新增工作项" width="560px"><el-form :model="workForm" label-position="top" class="compact-form"><div class="form-grid"><el-form-item label="类型"><el-select v-model="workForm.itemType"><el-option v-for="t in workItemTypes" :key="t.value" :label="t.label" :value="t.value"/></el-select></el-form-item><el-form-item label="WBS"><el-input v-model="workForm.wbsCode"/></el-form-item></div><el-form-item label="工作项"><el-input v-model="workForm.title"/></el-form-item><el-form-item label="说明"><el-input v-model="workForm.description" type="textarea" :rows="2"/></el-form-item><div class="form-grid"><el-form-item label="开始"><el-date-picker v-model="workForm.plannedStartDate" type="date" value-format="YYYY-MM-DD"/></el-form-item><el-form-item label="结束"><el-date-picker v-model="workForm.plannedEndDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></div><div class="form-grid"><el-form-item label="权重"><el-input-number v-model="workForm.weight" :min="0" :max="100"/></el-form-item><el-form-item label="优先级"><el-select v-model="workForm.priority"><el-option label="高" value="HIGH"/><el-option label="中" value="MEDIUM"/><el-option label="低" value="LOW"/></el-select></el-form-item></div></el-form><template #footer><el-button @click="workVisible=false">取消</el-button><el-button type="primary" @click="saveWork">保存</el-button></template></el-dialog>

    <el-dialog v-model="reportVisible" title="新增进展报告" width="650px"><el-form :model="reportForm" label-position="top" class="compact-form"><div class="form-grid"><el-form-item label="类型"><el-select v-model="reportForm.reportType"><el-option label="月报" value="MONTHLY"/><el-option label="季报" value="QUARTERLY"/><el-option label="年度报告" value="ANNUAL"/><el-option label="中期检查" value="MIDTERM"/><el-option label="专项" value="AD_HOC"/></el-select></el-form-item><el-form-item label="总体进度"><el-input-number v-model="reportForm.overallProgress" :min="0" :max="100"/></el-form-item></div><el-form-item label="完成工作"><el-input v-model="reportForm.completedWork" type="textarea" :rows="2"/></el-form-item><el-form-item label="关键成果"><el-input v-model="reportForm.keyAchievements" type="textarea" :rows="2"/></el-form-item><div class="form-grid"><el-form-item label="问题"><el-input v-model="reportForm.problems" type="textarea" :rows="2"/></el-form-item><el-form-item label="风险"><el-input v-model="reportForm.risks" type="textarea" :rows="2"/></el-form-item></div><el-form-item label="下一阶段计划"><el-input v-model="reportForm.nextPlan" type="textarea" :rows="2"/></el-form-item></el-form><template #footer><el-button @click="reportVisible=false">取消</el-button><el-button type="primary" @click="saveReport">提交报告</el-button></template></el-dialog>

    <el-dialog v-model="riskVisible" title="登记风险" width="560px"><el-form :model="riskForm" label-position="top" class="compact-form"><el-form-item label="风险"><el-input v-model="riskForm.title"/></el-form-item><el-form-item label="描述"><el-input v-model="riskForm.description" type="textarea" :rows="2"/></el-form-item><div class="form-grid"><el-form-item label="发生概率 1-5"><el-input-number v-model="riskForm.probability" :min="1" :max="5"/></el-form-item><el-form-item label="影响 1-5"><el-input-number v-model="riskForm.impact" :min="1" :max="5"/></el-form-item></div><el-form-item label="应对策略"><el-input v-model="riskForm.responseStrategy" type="textarea" :rows="2"/></el-form-item></el-form><template #footer><el-button @click="riskVisible=false">取消</el-button><el-button type="primary" @click="saveRisk">保存</el-button></template></el-dialog>

    <el-dialog v-model="issueVisible" title="登记问题" width="540px"><el-form :model="issueForm" label-position="top" class="compact-form"><el-form-item label="问题"><el-input v-model="issueForm.title"/></el-form-item><el-form-item label="描述"><el-input v-model="issueForm.description" type="textarea" :rows="2"/></el-form-item><div class="form-grid"><el-form-item label="严重度"><el-select v-model="issueForm.severity"><el-option label="严重" value="CRITICAL"/><el-option label="高" value="HIGH"/><el-option label="中" value="MEDIUM"/><el-option label="低" value="LOW"/></el-select></el-form-item><el-form-item label="解决期限"><el-date-picker v-model="issueForm.dueDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></div></el-form><template #footer><el-button @click="issueVisible=false">取消</el-button><el-button type="primary" @click="saveIssue">保存</el-button></template></el-dialog>

    <el-dialog v-model="changeVisible" title="项目变更申请" width="650px"><el-form :model="changeForm" label-position="top" class="compact-form"><el-form-item label="变更标题"><el-input v-model="changeForm.title"/></el-form-item><el-form-item label="变更原因"><el-input v-model="changeForm.reason" type="textarea" :rows="2"/></el-form-item><div class="form-grid"><el-form-item label="变更类型"><el-select v-model="changeItem.changeType"><el-option label="进度" value="SCHEDULE"/><el-option label="预算" value="BUDGET"/><el-option label="项目名称" value="SCOPE"/></el-select></el-form-item><el-form-item label="字段"><el-select v-model="changeItem.fieldCode"><el-option label="计划结束日期" value="planned_end_date"/><el-option label="当前预算" value="current_budget"/><el-option label="项目名称" value="project_name"/></el-select></el-form-item></div><div class="form-grid"><el-form-item label="变更前"><el-input v-model="changeItem.beforeValue"/></el-form-item><el-form-item label="变更后"><el-input v-model="changeItem.afterValue"/></el-form-item></div><el-form-item label="影响分析"><el-input v-model="changeForm.scheduleImpact" type="textarea" :rows="2" placeholder="说明对范围、进度、成本或成果的影响"/></el-form-item></el-form><template #footer><el-button @click="changeVisible=false">取消</el-button><el-button type="primary" @click="saveChange">保存草稿</el-button></template></el-dialog>

    <el-dialog v-model="expenseVisible" title="记录项目支出" width="520px"><el-form :model="expenseForm" label-position="top" class="compact-form"><el-form-item label="预算科目"><el-select v-model="expenseForm.budgetLineId"><el-option v-for="b in budgetLines" :key="b.budgetLineId" :label="b.category" :value="b.budgetLineId"/></el-select></el-form-item><div class="form-grid"><el-form-item label="金额"><el-input-number v-model="expenseForm.amount" :min="0" :step="1000"/></el-form-item><el-form-item label="日期"><el-date-picker v-model="expenseForm.expenseDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></div><el-form-item label="说明"><el-input v-model="expenseForm.description"/></el-form-item></el-form><template #footer><el-button @click="expenseVisible=false">取消</el-button><el-button type="primary" @click="saveExpense">保存</el-button></template></el-dialog>

    <el-dialog v-model="outcomeVisible" title="登记科研成果" width="540px"><el-form :model="outcomeForm" label-position="top" class="compact-form"><div class="form-grid"><el-form-item label="成果类型"><el-select v-model="outcomeForm.outcomeType"><el-option v-for="t in outcomeTypes" :key="t.value" :label="t.label" :value="t.value"/></el-select></el-form-item><el-form-item label="对应计划成果"><el-select v-model="outcomeForm.expectedOutputId" clearable><el-option v-for="o in expectedOutputs" :key="o.expectedOutputId" :label="o.name" :value="o.expectedOutputId"/></el-select></el-form-item></div><el-form-item label="成果名称"><el-input v-model="outcomeForm.name"/></el-form-item><el-form-item label="说明"><el-input v-model="outcomeForm.description" type="textarea" :rows="2"/></el-form-item><el-form-item label="完成日期"><el-date-picker v-model="outcomeForm.completedDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></el-form><template #footer><el-button @click="outcomeVisible=false">取消</el-button><el-button type="primary" @click="saveOutcome">保存</el-button></template></el-dialog>

    <el-dialog v-model="acceptanceVisible" title="提交项目验收" width="620px"><el-form :model="acceptanceForm" label-position="top" class="compact-form"><el-form-item label="项目总结"><el-input v-model="acceptanceForm.projectSummary" type="textarea" :rows="4"/></el-form-item><el-form-item label="任务完成情况"><el-input v-model="acceptanceForm.completionStatement" type="textarea" :rows="3"/></el-form-item><el-form-item label="未完成事项"><el-input v-model="acceptanceForm.outstandingItems" type="textarea" :rows="2"/></el-form-item></el-form><template #footer><el-button @click="acceptanceVisible=false">取消</el-button><el-button type="success" @click="saveAcceptance">提交验收</el-button></template></el-dialog>

    <el-dialog v-model="closeoutVisible" title="项目结项检查" width="560px"><div class="checklist"><el-checkbox v-model="closeoutForm.finalReportComplete">最终研究报告完整</el-checkbox><el-checkbox v-model="closeoutForm.financeComplete">经费信息完整</el-checkbox><el-checkbox v-model="closeoutForm.outputsComplete">成果登记完整</el-checkbox><el-checkbox v-model="closeoutForm.documentsComplete">项目资料完整</el-checkbox><el-checkbox v-model="closeoutForm.archiveComplete">项目归档完成</el-checkbox></div><el-input v-model="closeoutForm.conclusion" type="textarea" :rows="3" placeholder="结项说明"/></el-dialog>
  </div>
</template>

<script setup>
import useUserStore from '@/store/modules/user'
import {
  getResearchProject,activateResearchProject,addResearchWorkItem,researchWorkItemAction,addResearchProgressReport,
  addResearchRisk,occurResearchRisk,addResearchIssue,resolveResearchIssue,createResearchChange,submitResearchChange,
  approveResearchChange,rejectResearchChange,applyResearchChange,addResearchExpense,addResearchOutcome,
  submitResearchAcceptance,approveResearchAcceptance,rejectResearchAcceptance,completeResearchCloseout
} from '@/api/research'

const route=useRoute(),router=useRouter(),userStore=useUserStore(),{proxy}=getCurrentInstance()
const loading=ref(false),activeTab=ref('overview')
const detail=reactive({})
const project=computed(()=>detail.project||{}),members=computed(()=>detail.members||[]),workItems=computed(()=>detail.workItems||[])
const baseline=computed(()=>detail.baseline||null),baselines=computed(()=>detail.baselines||[]),reports=computed(()=>detail.progressReports||[])
const risks=computed(()=>detail.risks||[]),issues=computed(()=>detail.issues||[]),changes=computed(()=>detail.changes||[])
const budgetLines=computed(()=>detail.budgetLines||[]),expenses=computed(()=>detail.expenses||[]),outcomes=computed(()=>detail.outcomes||[])
const expectedOutputs=computed(()=>detail.expectedOutputs||[]),acceptance=computed(()=>detail.acceptance||null),closeout=computed(()=>detail.closeout||null)
const workflow=computed(()=>detail.workflow||[]),health=computed(()=>detail.health||{})
const isAdmin=computed(()=>userStore.roles.includes('admin')||userStore.roles.includes('research_admin'))
const isOwner=computed(()=>Number(userStore.id)===Number(project.value.piUserId))
const canOperate=computed(()=>isAdmin.value||isOwner.value||members.value.some(m=>Number(m.userId)===Number(userStore.id)&&['PI','PROJECT_MANAGER'].includes(m.memberRole)))
const canPlan=computed(()=>canOperate.value&&project.value.status==='PLANNING')
const canActivate=canPlan

const workVisible=ref(false),reportVisible=ref(false),riskVisible=ref(false),issueVisible=ref(false),changeVisible=ref(false),expenseVisible=ref(false),outcomeVisible=ref(false),acceptanceVisible=ref(false),closeoutVisible=ref(false)
const workForm=reactive({itemType:'TASK',wbsCode:'',title:'',description:'',plannedStartDate:'',plannedEndDate:'',weight:10,priority:'MEDIUM',sortOrder:0})
const reportForm=reactive({reportType:'QUARTERLY',overallProgress:0,completedWork:'',keyAchievements:'',problems:'',risks:'',nextPlan:''})
const riskForm=reactive({title:'',description:'',category:'SCHEDULE',probability:3,impact:3,responseStrategy:''})
const issueForm=reactive({title:'',description:'',severity:'MEDIUM',dueDate:''})
const changeForm=reactive({title:'',reason:'',scheduleImpact:'',costImpact:'',scopeImpact:'',outputImpact:'',riskImpact:''})
const changeItem=reactive({changeType:'SCHEDULE',fieldCode:'planned_end_date',beforeValue:'',afterValue:'',description:''})
const expenseForm=reactive({budgetLineId:null,amount:0,expenseDate:'',description:''})
const outcomeForm=reactive({outcomeType:'REPORT',expectedOutputId:null,name:'',description:'',completedDate:''})
const acceptanceForm=reactive({projectSummary:'',completionStatement:'',outstandingItems:''})
const closeoutForm=reactive({finalReportComplete:false,financeComplete:false,outputsComplete:false,documentsComplete:false,archiveComplete:false,conclusion:''})
const workItemTypes=[{value:'PHASE',label:'阶段'},{value:'WORK_PACKAGE',label:'工作包'},{value:'TASK',label:'任务'},{value:'MILESTONE',label:'里程碑'}]
const outcomeTypes=[{value:'PAPER',label:'论文'},{value:'PATENT',label:'专利'},{value:'SOFTWARE',label:'软件'},{value:'DATASET',label:'数据集'},{value:'STANDARD',label:'标准'},{value:'REPORT',label:'报告'},{value:'PROTOTYPE',label:'原型'},{value:'OTHER',label:'其他'}]

async function load(){loading.value=true;try{const r=await getResearchProject(route.params.projectId);Object.keys(detail).forEach(k=>delete detail[k]);Object.assign(detail,r.data||{});reportForm.overallProgress=project.value.progress||0;changeItem.beforeValue=project.value.plannedEndDate||''}finally{loading.value=false}}
async function activate(){await proxy.$modal.confirm('将当前团队、WBS、预算和成果计划固化为 Baseline V1 并启动项目，确认继续？');await activateResearchProject(project.value.projectId);proxy.$modal.msgSuccess('项目已启动');load()}
async function saveWork(){if(!workForm.title)return proxy.$modal.msgWarning('请输入工作项名称');await addResearchWorkItem(project.value.projectId,workForm);workVisible.value=false;proxy.$modal.msgSuccess('已添加');load()}
async function workAction(row,action){await researchWorkItemAction(project.value.projectId,row.workItemId,action);load()}
async function saveReport(){await addResearchProgressReport(project.value.projectId,reportForm);reportVisible.value=false;proxy.$modal.msgSuccess('进展报告已提交');load()}
async function saveRisk(){await addResearchRisk(project.value.projectId,riskForm);riskVisible.value=false;proxy.$modal.msgSuccess('风险已登记');load()}
async function occurRisk(row){await proxy.$modal.confirm('确认该风险已经发生并转为正式 Issue？');await occurResearchRisk(project.value.projectId,row.riskId,{});proxy.$modal.msgSuccess('已转为问题');load()}
async function saveIssue(){await addResearchIssue(project.value.projectId,issueForm);issueVisible.value=false;proxy.$modal.msgSuccess('问题已登记');load()}
async function resolve(row){try{const {value}=await ElMessageBox.prompt('填写解决方案','解决问题',{inputType:'textarea',confirmButtonText:'确认',cancelButtonText:'取消'});await resolveResearchIssue(project.value.projectId,row.issueId,value||'');proxy.$modal.msgSuccess('问题已解决');load()}catch{}}
async function saveChange(){if(!changeForm.title||!changeForm.reason)return proxy.$modal.msgWarning('请填写变更标题和原因');await createResearchChange(project.value.projectId,{...changeForm,items:[{...changeItem}]});changeVisible.value=false;proxy.$modal.msgSuccess('变更草稿已创建');load()}
async function submitChange(row){await submitResearchChange(project.value.projectId,row.changeId);proxy.$modal.msgSuccess('已提交审批');load()}
async function reviewChangeRow(row,ok){ok?await approveResearchChange(project.value.projectId,row.changeId,'同意变更'):await rejectResearchChange(project.value.projectId,row.changeId,'不同意变更');proxy.$modal.msgSuccess('已处理');load()}
async function applyChange(row){await proxy.$modal.confirm('应用变更将更新当前计划并生成新的不可变 Baseline，确认继续？');await applyResearchChange(project.value.projectId,row.changeId);proxy.$modal.msgSuccess('变更已应用');load()}
async function saveExpense(){await addResearchExpense(project.value.projectId,expenseForm);expenseVisible.value=false;proxy.$modal.msgSuccess('支出已记录');load()}
async function saveOutcome(){if(!outcomeForm.name)return proxy.$modal.msgWarning('请输入成果名称');await addResearchOutcome(project.value.projectId,outcomeForm);outcomeVisible.value=false;proxy.$modal.msgSuccess('成果已登记');load()}
async function saveAcceptance(){await submitResearchAcceptance(project.value.projectId,acceptanceForm);acceptanceVisible.value=false;proxy.$modal.msgSuccess('已提交验收');activeTab.value='closeout';load()}
async function reviewAcceptance(ok){ok?await approveResearchAcceptance(project.value.projectId,'验收通过'):await rejectResearchAcceptance(project.value.projectId,'退回完善');proxy.$modal.msgSuccess('验收已处理');load()}
async function saveCloseout(){await completeResearchCloseout(project.value.projectId,closeoutForm);closeoutVisible.value=false;proxy.$modal.msgSuccess('项目已结项');load()}
function statusText(s){return ({PLANNING:'规划中',ACTIVE:'执行中',SUSPENDED:'暂停',CLOSING:'结项中',CLOSED:'已结项',TERMINATED:'已终止'})[s]||s}
function memberRole(r){return ({PI:'PI',PROJECT_MANAGER:'项目经理',RESEARCHER:'研究人员',TECHNICAL_LEAD:'技术负责人',FINANCE_CONTACT:'经费联系人',SPONSOR:'Sponsor',MEMBER:'成员'})[r]||r}
function itemType(t){return ({PHASE:'阶段',WORK_PACKAGE:'工作包',TASK:'任务',MILESTONE:'里程碑'})[t]||t}
function workStatus(s){return ({NOT_STARTED:'未开始',IN_PROGRESS:'进行中',BLOCKED:'阻塞',DONE:'完成',CANCELLED:'取消'})[s]||s}
function reportType(t){return ({MONTHLY:'月报',QUARTERLY:'季报',ANNUAL:'年度报告',MIDTERM:'中期检查',AD_HOC:'专项报告'})[t]||t}
function riskText(t){return ({LOW:'低',MEDIUM:'中',HIGH:'高',CRITICAL:'严重'})[t]||t}
function baselineSource(s){return ({AWARD:'立项批复',CHANGE_REQUEST:'批准变更'})[s]||s||'-'}
function outputType(t){return ({PAPER:'论文',PATENT:'专利',SOFTWARE:'软件',DATASET:'数据集',STANDARD:'标准',REPORT:'报告',PROTOTYPE:'原型',OTHER:'其他'})[t]||t}
function acceptanceStatus(s){return ({DRAFT:'草稿',SUBMITTED:'待验收',UNDER_REVIEW:'验收中',RETURNED:'已退回',APPROVED:'已通过'})[s]||s}
function changeItems(row){return (row.items||[]).map(x=>\`\${x.fieldCode}: \${x.beforeValue||'-'} → \${x.afterValue||'-'}\`).join('；')||'-'}
function money(v){return Number(v||0).toLocaleString('zh-CN',{maximumFractionDigits:0})}
onMounted(load)
</script>

<style scoped lang="scss">
.workspace{display:flex;flex-direction:column;gap:8px;min-width:0}.project-bar{min-height:58px;padding:7px 10px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface);display:flex;align-items:center;justify-content:space-between;gap:12px}.identity{min-width:0;display:flex;align-items:center;gap:4px}.identity-copy{min-width:0}.title-line{display:flex;align-items:center;gap:8px}.title-line strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:15px}.identity-copy small{display:block;margin-top:3px;color:var(--rf-text-muted);font-size:11px}.actions{display:flex;gap:6px;flex-wrap:wrap}.status{display:inline-flex;min-height:22px;padding:0 7px;border-radius:999px;align-items:center;background:var(--rf-surface-subtle);font-size:10px;font-weight:700;color:var(--rf-text-secondary)}.status.active{background:var(--rf-primary-soft);color:var(--rf-primary)}.status.planning,.status.closing{background:var(--rf-warning-soft);color:var(--rf-warning)}.status.closed{background:var(--rf-success-soft);color:var(--rf-success)}.kpi-strip{display:grid;grid-template-columns:repeat(6,1fr);border:1px solid var(--rf-border);border-radius:10px;overflow:hidden;background:var(--rf-surface)}.kpi-strip>div{padding:8px 10px;border-right:1px solid var(--rf-border);display:flex;flex-direction:column;gap:3px}.kpi-strip>div:last-child{border-right:0}.kpi-strip span{font-size:10px;color:var(--rf-text-muted)}.kpi-strip strong{font-size:16px;font-variant-numeric:tabular-nums}.kpi-strip .date{font-size:12px}.danger{color:var(--rf-danger)!important}.warning{color:var(--rf-warning)!important}.workspace-panel{min-width:0;padding:0 12px 12px;border:1px solid var(--rf-border);border-radius:10px;background:var(--rf-surface)}.compact-tabs :deep(.el-tabs__header){margin-bottom:10px}.compact-tabs :deep(.el-tabs__item){height:42px;padding:0 12px;font-size:12px}.overview-grid,.outcome-grid,.closeout-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px}.block{min-width:0;padding:10px;border:1px solid var(--rf-border);border-radius:8px;background:var(--rf-surface)}.block.full{grid-column:1/-1}.block-head{min-height:28px;display:flex;align-items:center;justify-content:space-between;gap:8px}.block-head h3{margin:0;font-size:12px}.block-head>span{font-size:10px;color:var(--rf-text-muted)}.definition{margin:5px 0 0}.definition>div{padding:6px 0;border-top:1px solid var(--rf-border);display:grid;grid-template-columns:75px 1fr;gap:8px}.definition dt{font-size:10px;color:var(--rf-text-muted)}.definition dd{margin:0;font-size:11px;line-height:1.55;color:var(--rf-text-secondary)}.mini-definition{margin:5px 0 0;display:grid;grid-template-columns:1fr 1fr}.mini-definition>div{padding:7px 0;border-top:1px solid var(--rf-border)}.mini-definition dt{font-size:10px;color:var(--rf-text-muted)}.mini-definition dd{margin:3px 0 0;font-size:11px}.compact-list{display:flex;flex-direction:column}.list-row,.output-row{min-height:36px;border-top:1px solid var(--rf-border);display:grid;align-items:center;gap:7px;font-size:11px}.list-row{grid-template-columns:26px 75px 75px 1fr}.avatar{width:24px;height:24px;border-radius:50%;display:grid;place-items:center;background:var(--rf-primary-soft);color:var(--rf-primary);font-size:10px}.list-row span,.list-row small,.output-row small{color:var(--rf-text-muted)}.report-summary{padding-top:6px;border-top:1px solid var(--rf-border);font-size:11px}.report-summary p{margin:5px 0;color:var(--rf-text-secondary);line-height:1.55}.report-summary small{color:var(--rf-warning)}.tab-toolbar{min-height:42px;margin-bottom:6px;display:flex;align-items:center;justify-content:space-between;gap:10px}.tab-toolbar.inner{margin:0}.tab-toolbar>div{display:flex;align-items:baseline;gap:8px}.tab-toolbar strong{font-size:12px}.tab-toolbar span{font-size:10px;color:var(--rf-text-muted)}.progress-cell{display:grid;grid-template-columns:1fr 34px;align-items:center;gap:5px;font-size:10px}.mini-status{font-size:10px;color:var(--rf-text-secondary)}.baseline-history{min-height:40px;margin-top:8px;padding-top:8px;border-top:1px solid var(--rf-border);display:flex;align-items:center;gap:6px;overflow:auto}.baseline-history>span{font-size:10px;color:var(--rf-text-muted);white-space:nowrap}.baseline-chip{min-height:25px;padding:0 8px;border:1px solid var(--rf-border);border-radius:999px;background:var(--rf-surface-subtle);font-size:10px;color:var(--rf-text-secondary);white-space:nowrap}.governance-grid{display:flex;flex-direction:column;gap:8px}.risk-level{display:inline-flex;min-height:21px;padding:0 6px;border-radius:999px;align-items:center;font-size:10px;font-weight:700}.risk-level.low{background:var(--rf-success-soft);color:var(--rf-success)}.risk-level.medium{background:var(--rf-warning-soft);color:var(--rf-warning)}.risk-level.high,.risk-level.critical{background:var(--rf-danger-soft);color:var(--rf-danger)}.change-items{font-size:10px;color:var(--rf-text-secondary)}.finance-summary{min-height:48px;margin-bottom:8px;padding:7px 10px;border:1px solid var(--rf-border);border-radius:8px;display:flex;align-items:center;gap:24px}.finance-summary>div{display:flex;flex-direction:column;gap:2px}.finance-summary span{font-size:10px;color:var(--rf-text-muted)}.finance-summary strong{font-size:14px}.finance-summary .el-button{margin-left:auto}.finance-grid{display:grid;grid-template-columns:1fr 1.3fr;gap:8px}.output-row{grid-template-columns:70px 1fr 80px}.output-row>span{font-size:10px;color:var(--rf-primary)}.copy{margin:6px 0;font-size:11px;line-height:1.55;color:var(--rf-text-secondary)}.copy.muted{color:var(--rf-text-muted)}.inline-actions{margin-top:8px;display:flex;justify-content:flex-end;gap:6px}.audit-list{display:flex;flex-direction:column}.audit-row{min-height:34px;border-top:1px solid var(--rf-border);display:grid;grid-template-columns:150px 90px 90px 1fr;gap:8px;align-items:center;font-size:10px}.audit-row span,.audit-row small{color:var(--rf-text-muted)}.compact-form :deep(.el-form-item){margin-bottom:10px}.compact-form :deep(.el-form-item__label){padding-bottom:4px;font-size:11px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.checklist{margin-bottom:12px;display:grid;grid-template-columns:1fr 1fr;gap:8px}@media(max-width:1000px){.kpi-strip{grid-template-columns:repeat(3,1fr)}.overview-grid,.finance-grid,.outcome-grid,.closeout-grid{grid-template-columns:1fr}.block.full{grid-column:auto}}@media(max-width:700px){.project-bar{align-items:flex-start;flex-direction:column}.actions{width:100%}.kpi-strip{grid-template-columns:repeat(2,1fr)}.form-grid{grid-template-columns:1fr}.audit-row{grid-template-columns:1fr 1fr}}
</style>
