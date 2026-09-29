-- ResearchFlow V2 科研项目全过程治理模型
-- Demo 数据库：Proposal -> Review -> Award -> Project -> Baseline -> Execution -> Change -> Acceptance -> Closeout
-- 文件存储继续使用本地 uploadPath / Docker Volume，不依赖 MinIO。

SET NAMES utf8mb4;

DROP TABLE IF EXISTS research_workflow_action;
DROP TABLE IF EXISTS research_workflow_instance;
DROP TABLE IF EXISTS research_closeout;
DROP TABLE IF EXISTS research_acceptance;
DROP TABLE IF EXISTS research_document;
DROP TABLE IF EXISTS research_outcome;
DROP TABLE IF EXISTS research_expense;
DROP TABLE IF EXISTS research_budget_line;
DROP TABLE IF EXISTS research_budget;
DROP TABLE IF EXISTS research_change_item;
DROP TABLE IF EXISTS research_change_request;
DROP TABLE IF EXISTS research_decision;
DROP TABLE IF EXISTS research_issue;
DROP TABLE IF EXISTS research_risk;
DROP TABLE IF EXISTS research_progress_report;
DROP TABLE IF EXISTS research_work_item;
DROP TABLE IF EXISTS research_baseline;
DROP TABLE IF EXISTS research_project_member;
DROP TABLE IF EXISTS research_project;
DROP TABLE IF EXISTS research_award;
DROP TABLE IF EXISTS research_review;
DROP TABLE IF EXISTS research_expected_output;
DROP TABLE IF EXISTS research_proposal_budget_line;
DROP TABLE IF EXISTS research_proposal_member;
DROP TABLE IF EXISTS research_proposal;
DROP TABLE IF EXISTS research_call;
DROP TABLE IF EXISTS research_record;

CREATE TABLE research_record (
  record_id bigint NOT NULL AUTO_INCREMENT,
  record_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  record_type varchar(32) NOT NULL DEFAULT 'RESEARCH_PROJECT',
  source_type varchar(32) DEFAULT 'INTERNAL',
  current_phase varchar(32) NOT NULL DEFAULT 'PROPOSAL',
  owner_user_id bigint NOT NULL,
  dept_id bigint DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  closed_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_record_no(record_no),
  KEY idx_record_owner(owner_user_id),
  KEY idx_record_phase(current_phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研事项统一身份';

CREATE TABLE research_call (
  call_id bigint NOT NULL AUTO_INCREMENT,
  call_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  call_type varchar(32) DEFAULT NULL,
  sponsor_org varchar(200) DEFAULT NULL,
  funding_source varchar(200) DEFAULT NULL,
  description text,
  requirements text,
  application_start date DEFAULT NULL,
  application_deadline date DEFAULT NULL,
  max_budget decimal(16,2) DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(call_id),
  UNIQUE KEY uk_call_no(call_no),
  KEY idx_call_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研指南与项目机会';

CREATE TABLE research_proposal (
  proposal_id bigint NOT NULL AUTO_INCREMENT,
  record_id bigint NOT NULL,
  call_id bigint DEFAULT NULL,
  proposal_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  applicant_user_id bigint NOT NULL,
  applicant_dept_id bigint DEFAULT NULL,
  background text,
  objectives text,
  project_scope text,
  out_of_scope text,
  research_content longtext,
  methodology longtext,
  innovation_points text,
  success_criteria text,
  planned_start_date date DEFAULT NULL,
  planned_end_date date DEFAULT NULL,
  requested_budget decimal(16,2) NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  version int NOT NULL DEFAULT 0,
  submitted_at datetime DEFAULT NULL,
  approved_at datetime DEFAULT NULL,
  rejected_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(proposal_id),
  UNIQUE KEY uk_proposal_no(proposal_no),
  KEY idx_proposal_record(record_id),
  KEY idx_proposal_status(status),
  KEY idx_proposal_applicant(applicant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研项目申报';

CREATE TABLE research_proposal_member (
  member_id bigint NOT NULL AUTO_INCREMENT,
  proposal_id bigint NOT NULL,
  user_id bigint NOT NULL,
  member_role varchar(32) NOT NULL DEFAULT 'MEMBER',
  responsibility varchar(1000) DEFAULT NULL,
  planned_allocation decimal(5,2) DEFAULT NULL,
  sort_order int DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(member_id),
  UNIQUE KEY uk_proposal_member(proposal_id,user_id),
  KEY idx_proposal_member_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报团队';

CREATE TABLE research_proposal_budget_line (
  budget_line_id bigint NOT NULL AUTO_INCREMENT,
  proposal_id bigint NOT NULL,
  category varchar(64) NOT NULL,
  amount decimal(16,2) NOT NULL DEFAULT 0,
  description varchar(1000) DEFAULT NULL,
  sort_order int DEFAULT 0,
  PRIMARY KEY(budget_line_id),
  KEY idx_pbl_proposal(proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报预算明细';

CREATE TABLE research_expected_output (
  expected_output_id bigint NOT NULL AUTO_INCREMENT,
  proposal_id bigint NOT NULL,
  output_type varchar(32) NOT NULL,
  name varchar(200) DEFAULT NULL,
  target_quantity int NOT NULL DEFAULT 1,
  target_description varchar(1000) DEFAULT NULL,
  PRIMARY KEY(expected_output_id),
  KEY idx_expected_output_proposal(proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报预期成果';

CREATE TABLE research_review (
  review_id bigint NOT NULL AUTO_INCREMENT,
  proposal_id bigint NOT NULL,
  review_type varchar(32) NOT NULL,
  reviewer_user_id bigint DEFAULT NULL,
  score decimal(5,2) DEFAULT NULL,
  decision varchar(32) DEFAULT NULL,
  comment text,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  started_at datetime DEFAULT NULL,
  completed_at datetime DEFAULT NULL,
  PRIMARY KEY(review_id),
  KEY idx_review_proposal(proposal_id),
  KEY idx_review_reviewer(reviewer_user_id),
  KEY idx_review_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目评审';

CREATE TABLE research_award (
  award_id bigint NOT NULL AUTO_INCREMENT,
  record_id bigint NOT NULL,
  proposal_id bigint NOT NULL,
  award_no varchar(64) NOT NULL,
  approved_title varchar(200) NOT NULL,
  approved_start_date date NOT NULL,
  approved_end_date date NOT NULL,
  approved_budget decimal(16,2) NOT NULL,
  approved_scope text,
  approved_objectives text,
  approved_outputs text,
  status varchar(32) NOT NULL DEFAULT 'ISSUED',
  issued_at datetime NOT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(award_id),
  UNIQUE KEY uk_award_no(award_no),
  UNIQUE KEY uk_award_proposal(proposal_id),
  KEY idx_award_record(record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='立项批复';

CREATE TABLE research_project (
  project_id bigint NOT NULL AUTO_INCREMENT,
  record_id bigint NOT NULL,
  proposal_id bigint NOT NULL,
  award_id bigint NOT NULL,
  project_no varchar(64) NOT NULL,
  project_name varchar(200) NOT NULL,
  pi_user_id bigint NOT NULL,
  dept_id bigint DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_end_date date DEFAULT NULL,
  current_budget decimal(16,2) NOT NULL DEFAULT 0,
  current_baseline_id bigint DEFAULT NULL,
  progress int NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL DEFAULT 'PLANNING',
  version int NOT NULL DEFAULT 0,
  activated_at datetime DEFAULT NULL,
  suspended_at datetime DEFAULT NULL,
  closing_at datetime DEFAULT NULL,
  closed_at datetime DEFAULT NULL,
  terminated_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(project_id),
  UNIQUE KEY uk_project_no(project_no),
  UNIQUE KEY uk_project_record(record_id),
  UNIQUE KEY uk_project_award(award_id),
  KEY idx_project_pi(pi_user_id),
  KEY idx_project_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正式科研项目';

CREATE TABLE research_project_member (
  member_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  user_id bigint NOT NULL,
  member_role varchar(32) NOT NULL DEFAULT 'MEMBER',
  responsibility varchar(1000) DEFAULT NULL,
  allocation_percent decimal(5,2) DEFAULT NULL,
  join_date date DEFAULT NULL,
  leave_date date DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(member_id),
  UNIQUE KEY uk_project_member(project_id,user_id),
  KEY idx_project_member_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正式项目团队';

CREATE TABLE research_baseline (
  baseline_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  version_no int NOT NULL,
  source_type varchar(32) NOT NULL,
  source_id bigint DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_end_date date DEFAULT NULL,
  approved_budget decimal(16,2) DEFAULT NULL,
  scope_snapshot json DEFAULT NULL,
  objective_snapshot json DEFAULT NULL,
  output_snapshot json DEFAULT NULL,
  work_plan_snapshot json DEFAULT NULL,
  budget_snapshot json DEFAULT NULL,
  effective_at datetime NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  created_by_user_id bigint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(baseline_id),
  UNIQUE KEY uk_project_baseline(project_id,version_no),
  KEY idx_baseline_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变项目基线';

CREATE TABLE research_work_item (
  work_item_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  parent_id bigint DEFAULT NULL,
  item_type varchar(32) NOT NULL,
  wbs_code varchar(64) DEFAULT NULL,
  title varchar(200) NOT NULL,
  description text,
  owner_user_id bigint DEFAULT NULL,
  planned_start_date date DEFAULT NULL,
  planned_end_date date DEFAULT NULL,
  actual_start_date date DEFAULT NULL,
  actual_end_date date DEFAULT NULL,
  weight decimal(6,2) DEFAULT NULL,
  progress int NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL DEFAULT 'NOT_STARTED',
  priority varchar(20) NOT NULL DEFAULT 'MEDIUM',
  sort_order int DEFAULT 0,
  version int NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(work_item_id),
  KEY idx_work_project(project_id),
  KEY idx_work_parent(parent_id),
  KEY idx_work_owner(owner_user_id),
  KEY idx_work_status(project_id,status),
  KEY idx_work_type(project_id,item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WBS统一工作项';

CREATE TABLE research_progress_report (
  report_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  report_no varchar(64) DEFAULT NULL,
  report_type varchar(32) NOT NULL DEFAULT 'AD_HOC',
  period_start date DEFAULT NULL,
  period_end date DEFAULT NULL,
  overall_progress int DEFAULT NULL,
  completed_work text,
  key_achievements text,
  problems text,
  risks text,
  next_plan text,
  support_needed text,
  budget_summary text,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  prepared_by bigint NOT NULL,
  submitted_at datetime DEFAULT NULL,
  reviewed_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(report_id),
  KEY idx_progress_report_project(project_id),
  KEY idx_progress_report_type(project_id,report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进展/中期报告';

CREATE TABLE research_risk (
  risk_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  risk_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  description text,
  category varchar(64) DEFAULT NULL,
  probability int NOT NULL DEFAULT 1,
  impact int NOT NULL DEFAULT 1,
  score int NOT NULL DEFAULT 1,
  risk_level varchar(32) NOT NULL DEFAULT 'LOW',
  trigger_condition text,
  response_strategy text,
  owner_user_id bigint DEFAULT NULL,
  source varchar(32) NOT NULL DEFAULT 'MANUAL',
  rule_code varchar(64) DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'OPEN',
  identified_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at datetime DEFAULT NULL,
  version int NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(risk_id),
  UNIQUE KEY uk_project_risk_no(project_id,risk_no),
  KEY idx_risk_project(project_id),
  KEY idx_risk_level(project_id,risk_level),
  KEY idx_risk_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险登记册';

CREATE TABLE research_issue (
  issue_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  source_risk_id bigint DEFAULT NULL,
  issue_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  description text,
  severity varchar(32) NOT NULL DEFAULT 'MEDIUM',
  owner_user_id bigint DEFAULT NULL,
  occurred_at datetime DEFAULT CURRENT_TIMESTAMP,
  due_date date DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'OPEN',
  resolution text,
  resolved_at datetime DEFAULT NULL,
  closed_at datetime DEFAULT NULL,
  version int NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(issue_id),
  UNIQUE KEY uk_project_issue_no(project_id,issue_no),
  KEY idx_issue_project(project_id),
  KEY idx_issue_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目问题日志';

CREATE TABLE research_decision (
  decision_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  decision_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  context text,
  decision text NOT NULL,
  reason text,
  decision_maker_user_id bigint DEFAULT NULL,
  related_risk_id bigint DEFAULT NULL,
  related_issue_id bigint DEFAULT NULL,
  related_change_id bigint DEFAULT NULL,
  decided_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(decision_id),
  UNIQUE KEY uk_project_decision_no(project_id,decision_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目决策日志';

CREATE TABLE research_change_request (
  change_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  change_no varchar(64) NOT NULL,
  title varchar(200) NOT NULL,
  reason text NOT NULL,
  applicant_user_id bigint NOT NULL,
  scope_impact text,
  schedule_impact text,
  cost_impact text,
  output_impact text,
  risk_impact text,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  version int NOT NULL DEFAULT 0,
  submitted_at datetime DEFAULT NULL,
  assessed_at datetime DEFAULT NULL,
  approved_at datetime DEFAULT NULL,
  rejected_at datetime DEFAULT NULL,
  applied_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(change_id),
  UNIQUE KEY uk_change_no(change_no),
  KEY idx_change_project(project_id),
  KEY idx_change_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目变更申请';

CREATE TABLE research_change_item (
  change_item_id bigint NOT NULL AUTO_INCREMENT,
  change_id bigint NOT NULL,
  change_type varchar(32) NOT NULL,
  field_code varchar(64) DEFAULT NULL,
  before_value text,
  after_value text,
  description varchar(1000) DEFAULT NULL,
  PRIMARY KEY(change_item_id),
  KEY idx_change_item_change(change_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='变更项';

CREATE TABLE research_budget (
  budget_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  baseline_id bigint DEFAULT NULL,
  version_no int NOT NULL,
  total_amount decimal(16,2) NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  effective_at datetime DEFAULT CURRENT_TIMESTAMP,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(budget_id),
  UNIQUE KEY uk_project_budget_version(project_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目预算版本';

CREATE TABLE research_budget_line (
  budget_line_id bigint NOT NULL AUTO_INCREMENT,
  budget_id bigint NOT NULL,
  category varchar(64) NOT NULL,
  planned_amount decimal(16,2) NOT NULL DEFAULT 0,
  description varchar(1000) DEFAULT NULL,
  PRIMARY KEY(budget_line_id),
  KEY idx_budget_line_budget(budget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算明细';

CREATE TABLE research_expense (
  expense_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  budget_line_id bigint DEFAULT NULL,
  expense_no varchar(64) DEFAULT NULL,
  expense_date date NOT NULL,
  amount decimal(16,2) NOT NULL,
  description varchar(1000) DEFAULT NULL,
  reference_no varchar(100) DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(expense_id),
  KEY idx_expense_project(project_id),
  KEY idx_expense_budget_line(budget_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目支出';

CREATE TABLE research_outcome (
  outcome_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  expected_output_id bigint DEFAULT NULL,
  outcome_type varchar(32) NOT NULL,
  name varchar(300) NOT NULL,
  description text,
  status varchar(32) DEFAULT 'COMPLETED',
  completed_date date DEFAULT NULL,
  external_reference varchar(500) DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(outcome_id),
  KEY idx_outcome_project(project_id),
  KEY idx_outcome_expected(expected_output_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际科研成果';

CREATE TABLE research_document (
  document_id bigint NOT NULL AUTO_INCREMENT,
  record_id bigint NOT NULL,
  business_type varchar(32) NOT NULL,
  business_id bigint NOT NULL,
  category varchar(32) NOT NULL,
  file_name varchar(255) NOT NULL,
  storage_provider varchar(32) NOT NULL DEFAULT 'LOCAL',
  storage_key varchar(500) NOT NULL,
  mime_type varchar(100) DEFAULT NULL,
  file_size bigint DEFAULT NULL,
  version_no int NOT NULL DEFAULT 1,
  status varchar(32) NOT NULL DEFAULT 'ACTIVE',
  uploaded_by bigint NOT NULL,
  uploaded_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(document_id),
  KEY idx_doc_record(record_id),
  KEY idx_doc_business(business_type,business_id),
  KEY idx_doc_category(business_type,business_id,category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一科研文档元数据';

CREATE TABLE research_acceptance (
  acceptance_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  acceptance_no varchar(64) NOT NULL,
  project_summary longtext,
  completion_statement longtext,
  outstanding_items text,
  applicant_user_id bigint NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  submitted_at datetime DEFAULT NULL,
  reviewed_at datetime DEFAULT NULL,
  review_comment text,
  PRIMARY KEY(acceptance_id),
  UNIQUE KEY uk_acceptance_no(acceptance_no),
  UNIQUE KEY uk_acceptance_project(project_id),
  KEY idx_acceptance_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目验收';

CREATE TABLE research_closeout (
  closeout_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  acceptance_id bigint NOT NULL,
  final_report_complete char(1) NOT NULL DEFAULT '0',
  finance_complete char(1) NOT NULL DEFAULT '0',
  outputs_complete char(1) NOT NULL DEFAULT '0',
  documents_complete char(1) NOT NULL DEFAULT '0',
  issues_complete char(1) NOT NULL DEFAULT '0',
  archive_complete char(1) NOT NULL DEFAULT '0',
  conclusion text,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  completed_by bigint DEFAULT NULL,
  completed_at datetime DEFAULT NULL,
  PRIMARY KEY(closeout_id),
  UNIQUE KEY uk_closeout_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政结项与归档';

CREATE TABLE research_workflow_instance (
  workflow_id bigint NOT NULL AUTO_INCREMENT,
  workflow_type varchar(32) NOT NULL,
  business_type varchar(32) NOT NULL,
  business_id bigint NOT NULL,
  status varchar(32) NOT NULL,
  current_step varchar(64) DEFAULT NULL,
  started_by bigint NOT NULL,
  started_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at datetime DEFAULT NULL,
  PRIMARY KEY(workflow_id),
  KEY idx_workflow_business(business_type,business_id),
  KEY idx_workflow_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务流程实例';

CREATE TABLE research_workflow_action (
  action_id bigint NOT NULL AUTO_INCREMENT,
  workflow_id bigint NOT NULL,
  step_code varchar(64) DEFAULT NULL,
  action varchar(32) NOT NULL,
  operator_user_id bigint NOT NULL,
  comment text,
  acted_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(action_id),
  KEY idx_action_workflow(workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程动作审计';

-- Demo roles / users
INSERT IGNORE INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
(20, '科研管理员', 'research_admin', 20, '1', 1, 1, '0', '0', 'admin', sysdate(), '负责评审、立项、变更和验收'),
(21, '科研用户', 'research_owner', 21, '5', 1, 1, '0', '0', 'admin', sysdate(), '负责申报及项目执行'),
(22, '管理者', 'research_manager', 22, '1', 1, 1, '0', '0', 'admin', sysdate(), '全局只读与组合分析');

INSERT IGNORE INTO sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
VALUES
(20,103,'research_admin','科研管理员','00','research-admin@example.local','13800000020','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',sysdate(),sysdate(),'admin',sysdate(),'',null,'Demo'),
(21,105,'researcher','张伟','00','researcher@example.local','13800000021','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',sysdate(),sysdate(),'admin',sysdate(),'',null,'Demo'),
(22,103,'research_manager','科研管理负责人','00','manager@example.local','13800000022','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',sysdate(),sysdate(),'admin',sysdate(),'',null,'Demo');
INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES (20,20),(21,21),(22,22);

-- Organization wording
UPDATE sys_dept SET dept_name='先进技术研究院', leader='科研负责人' WHERE dept_id=100;
UPDATE sys_dept SET dept_name='智能制造研究中心', leader='中心负责人' WHERE dept_id=101;
UPDATE sys_dept SET dept_name='材料与能源研究中心', leader='中心负责人' WHERE dept_id=102;
UPDATE sys_dept SET dept_name='科研管理处', leader='科研管理员' WHERE dept_id=103;
UPDATE sys_dept SET dept_name='综合管理办公室' WHERE dept_id=104;
UPDATE sys_dept SET dept_name='智能检测实验室', leader='实验室负责人' WHERE dept_id=105;
UPDATE sys_dept SET dept_name='财务管理处' WHERE dept_id=106;

-- Calls
INSERT INTO research_call(call_id,call_no,title,call_type,sponsor_org,funding_source,description,requirements,application_start,application_deadline,max_budget,status)
VALUES
(101,'CALL-2026-01','2026年度智能制造关键技术专项','INTERNAL','先进技术研究院','院级科研专项','聚焦智能制造关键共性技术。','目标可量化，必须包含年度计划和预算明细。','2026-01-01','2026-10-31',1500000,'OPEN');

-- Record + Proposal examples
INSERT INTO research_record(record_id,record_no,title,current_phase,owner_user_id,dept_id,status,create_by) VALUES
(1001,'RR-2026-000001','AI驱动工业质量检测关键技术研究','EXECUTION',21,105,'ACTIVE','researcher'),
(1002,'RR-2026-000002','科研数据智能治理平台预研','REVIEW',21,105,'ACTIVE','researcher'),
(1003,'RR-2025-000003','智能制造知识图谱构建方法','CLOSED',21,105,'CLOSED','researcher'),
(1004,'RR-2026-000004','低碳实验室能耗优化研究','EXECUTION',21,105,'ACTIVE','researcher');

INSERT INTO research_proposal(proposal_id,record_id,call_id,proposal_no,title,applicant_user_id,applicant_dept_id,background,objectives,project_scope,out_of_scope,research_content,methodology,innovation_points,success_criteria,planned_start_date,planned_end_date,requested_budget,status,submitted_at,approved_at,create_by) VALUES
(2001,1001,101,'PR-2026-000001','AI驱动工业质量检测关键技术研究',21,105,'工业外观缺陷依赖人工抽检，效率和稳定性有限。','建立可解释的工业缺陷识别方法并完成现场验证。','数据集建设、算法开发、现场验证。','不建设完整MES和生产级硬件平台。','数据采集、模型训练、工程验证。','多模态视觉模型与小样本学习。','引入可解释缺陷定位和现场闭环验证。','准确率>=95%，完成原型和技术报告。','2026-01-10','2026-12-20',1200000,'APPROVED','2026-01-06 10:00:00','2026-01-08 14:00:00','researcher'),
(2002,1002,101,'PR-2026-000002','科研数据智能治理平台预研',21,105,'科研数据分散在不同项目与文件中。','验证跨项目数据治理与检索方法。','数据标准、治理流程、检索原型。','不建设正式数据湖。','数据标准、元数据治理、原型验证。','结构化元数据优先。','以科研事项为中心组织数据资产。','完成可演示原型及治理规范。','2026-10-15','2027-06-30',500000,'UNDER_REVIEW',sysdate(),NULL,'researcher'),
(2003,1003,NULL,'PR-2025-000003','智能制造知识图谱构建方法',21,105,'制造知识分散。','形成制造知识建模和应用方法。','知识抽取、融合、应用验证。','不做大规模商业知识平台。','知识抽取、融合、应用验证。','知识图谱方法。','领域知识融合。','完成报告和知识图谱。','2025-01-01','2025-12-31',450000,'APPROVED','2024-12-10 10:00:00','2024-12-15 10:00:00','researcher'),
(2004,1004,NULL,'PR-2026-000004','低碳实验室能耗优化研究',21,105,'实验室能耗缺少精细化优化。','形成能耗优化算法及示范。','采集、分析、优化策略。','不改造楼宇控制系统。','能耗采集、基线建模、优化策略。','规则+预测模型。','面向实验室科研场景。','能耗降低8%以上。','2026-03-01','2026-11-30',300000,'APPROVED','2026-02-15 10:00:00','2026-02-18 10:00:00','researcher');

INSERT INTO research_proposal_member(proposal_id,user_id,member_role,responsibility,planned_allocation,create_by) VALUES
(2001,21,'PI','总体研究与交付',60,'researcher'),
(2002,21,'PI','方案设计与原型验证',70,'researcher'),
(2003,21,'PI','知识建模',60,'researcher'),
(2004,21,'PI','算法与示范验证',60,'researcher');

INSERT INTO research_proposal_budget_line(proposal_id,category,amount,description,sort_order) VALUES
(2001,'设备费',300000,'工业相机及边缘设备',1),(2001,'材料费',200000,'实验耗材',2),(2001,'测试费',250000,'测试验证',3),(2001,'人员费',350000,'研究投入',4),(2001,'其他',100000,'其他费用',5),
(2002,'设备费',120000,'开发测试环境',1),(2002,'人员费',300000,'预研投入',2),(2002,'其他',80000,'数据和会议',3);

INSERT INTO research_expected_output(expected_output_id,proposal_id,output_type,name,target_quantity,target_description) VALUES
(3001,2001,'SOFTWARE','工业缺陷检测原型系统',1,'可演示原型'),
(3002,2001,'REPORT','关键技术研究报告',1,'正式研究报告'),
(3003,2001,'PATENT','发明专利',1,'至少申请1项'),
(3004,2003,'REPORT','知识图谱结题报告',1,'正式结题报告');

INSERT INTO research_review(review_id,proposal_id,review_type,reviewer_user_id,score,decision,comment,status,started_at,completed_at) VALUES
(4001,2001,'MANAGEMENT',20,92,'PASS','目标清晰，同意立项。','COMPLETED','2026-01-07 09:00:00','2026-01-08 14:00:00'),
(4002,2002,'TECHNICAL',20,NULL,NULL,'待完成技术评审。','PENDING',sysdate(),NULL),
(4003,2003,'MANAGEMENT',20,90,'PASS','同意立项。','COMPLETED','2024-12-12 09:00:00','2024-12-15 10:00:00');

INSERT INTO research_award(award_id,record_id,proposal_id,award_no,approved_title,approved_start_date,approved_end_date,approved_budget,approved_scope,approved_objectives,approved_outputs,status,issued_at,create_by) VALUES
(5001,1001,2001,'AW-2026-000001','AI驱动工业质量检测关键技术研究','2026-01-10','2026-12-20',1100000,'数据集、算法原型和现场验证。','完成可解释缺陷识别并现场验证。','原型系统、技术报告、专利。','ISSUED','2026-01-08 15:00:00','research_admin'),
(5003,1003,2003,'AW-2025-000003','智能制造知识图谱构建方法','2025-01-01','2025-12-31',420000,'知识抽取、融合与应用验证。','形成制造知识建模方法。','结题报告、知识图谱。','ISSUED','2024-12-15 12:00:00','research_admin'),
(5004,1004,2004,'AW-2026-000004','低碳实验室能耗优化研究','2026-03-01','2026-11-30',280000,'能耗采集、建模和优化策略验证。','形成能耗优化算法及示范。','研究报告、算法模型。','ISSUED','2026-02-18 12:00:00','research_admin');

INSERT INTO research_project(project_id,record_id,proposal_id,award_id,project_no,project_name,pi_user_id,dept_id,planned_start_date,planned_end_date,current_budget,current_baseline_id,progress,status,activated_at,closed_at,create_by) VALUES
(6001,1001,2001,5001,'RF-2026-001','AI驱动工业质量检测关键技术研究',21,105,'2026-01-10','2026-12-20',1100000,7001,64,'ACTIVE','2026-01-10 09:00:00',NULL,'research_admin'),
(6003,1003,2003,5003,'RF-2025-003','智能制造知识图谱构建方法',21,105,'2025-01-01','2025-12-31',420000,7003,100,'CLOSED','2025-01-01 09:00:00','2026-01-08 10:00:00','research_admin'),
(6004,1004,2004,5004,'RF-2026-004','低碳实验室能耗优化研究',21,105,'2026-03-01','2026-11-30',280000,7004,45,'ACTIVE','2026-03-01 09:00:00',NULL,'research_admin');

INSERT INTO research_project_member(project_id,user_id,member_role,responsibility,allocation_percent,join_date,create_by) VALUES
(6001,21,'PI','总体负责',60,'2026-01-10','research_admin'),
(6003,21,'PI','总体负责',60,'2025-01-01','research_admin'),
(6004,21,'PI','总体负责',60,'2026-03-01','research_admin');

INSERT INTO research_baseline(baseline_id,project_id,version_no,source_type,planned_start_date,planned_end_date,approved_budget,scope_snapshot,objective_snapshot,output_snapshot,work_plan_snapshot,budget_snapshot,effective_at,created_by_user_id) VALUES
(7001,6001,1,'AWARD','2026-01-10','2026-12-20',1100000,JSON_OBJECT('scope','数据集、算法原型和现场验证'),JSON_OBJECT('objectives','完成可解释缺陷识别并现场验证'),JSON_ARRAY('原型系统','技术报告','专利'),JSON_ARRAY(),JSON_OBJECT('total',1100000),'2026-01-10 09:00:00',20),
(7003,6003,1,'AWARD','2025-01-01','2025-12-31',420000,JSON_OBJECT('scope','知识抽取、融合与应用验证'),JSON_OBJECT('objectives','形成制造知识建模方法'),JSON_ARRAY('结题报告','知识图谱'),JSON_ARRAY(),JSON_OBJECT('total',420000),'2025-01-01 09:00:00',20),
(7004,6004,1,'AWARD','2026-03-01','2026-11-30',280000,JSON_OBJECT('scope','能耗采集、建模和优化策略验证'),JSON_OBJECT('objectives','形成能耗优化算法及示范'),JSON_ARRAY('研究报告','算法模型'),JSON_ARRAY(),JSON_OBJECT('total',280000),'2026-03-01 09:00:00',20);

INSERT INTO research_work_item(work_item_id,project_id,parent_id,item_type,wbs_code,title,description,owner_user_id,planned_start_date,planned_end_date,actual_start_date,actual_end_date,weight,progress,status,priority,sort_order,create_by) VALUES
(8001,6001,NULL,'PHASE','1','数据与算法阶段','完成数据建设和算法原型。',21,'2026-01-10','2026-06-30','2026-01-10','2026-06-28',50,100,'DONE','HIGH',1,'researcher'),
(8002,6001,8001,'TASK','1.1','完成工业缺陷数据集V1','采集、清洗和标注样本。',21,'2026-01-10','2026-03-31','2026-01-10','2026-03-25',20,100,'DONE','HIGH',1,'researcher'),
(8003,6001,8001,'MILESTONE','M1','算法原型指标达到95%','完成内部技术评审。',21,'2026-06-01','2026-06-30','2026-06-01','2026-06-28',30,100,'DONE','HIGH',2,'researcher'),
(8004,6001,NULL,'PHASE','2','现场验证阶段','完成现场适配和验证。',21,'2026-07-01','2026-11-30','2026-07-01',NULL,50,45,'IN_PROGRESS','HIGH',2,'researcher'),
(8005,6001,8004,'TASK','2.1','完成现场部署适配','完成边缘端适配和环境测试。',21,'2026-07-01','2026-09-30','2026-07-01',NULL,25,70,'IN_PROGRESS','HIGH',1,'researcher'),
(8006,6001,8004,'MILESTONE','M2','完成现场验收测试','目标完成现场测试。',21,'2026-10-01','2026-11-30',NULL,NULL,25,0,'NOT_STARTED','HIGH',2,'researcher'),
(8401,6004,NULL,'TASK','1.1','节能策略验证','完成关键场景实验。',21,'2026-06-01','2026-08-31','2026-06-01',NULL,100,45,'BLOCKED','HIGH',1,'researcher');

INSERT INTO research_progress_report(report_id,project_id,report_no,report_type,period_start,period_end,overall_progress,completed_work,key_achievements,problems,risks,next_plan,support_needed,budget_summary,status,prepared_by,submitted_at,create_by) VALUES
(9001,6001,'RPT-2026-001','QUARTERLY','2026-07-01','2026-09-30',64,'完成现场环境适配和第二轮模型优化。','现场模型稳定性提升。','产线部署窗口有限。','现场测试排期存在延期风险。','完成现场测试并准备验收材料。','协调现场测试窗口。','预算执行总体正常。','SUBMITTED',21,'2026-09-25 18:00:00','researcher');

INSERT INTO research_risk(risk_id,project_id,risk_no,title,description,category,probability,impact,score,risk_level,trigger_condition,response_strategy,owner_user_id,source,rule_code,status,identified_at,create_by) VALUES
(10001,6001,'RISK-0001','现场验证窗口不足','生产现场可用于测试的窗口持续压缩。','SCHEDULE',4,4,16,'HIGH','连续两周无法安排现场测试','提前锁定窗口并准备离线验证方案',21,'MANUAL',NULL,'MONITORING','2026-09-12 10:00:00','researcher'),
(10002,6004,'RISK-0001','关键实验进度滞后','计划时间已消耗较多，但关键任务完成度偏低。','SCHEDULE',4,5,20,'CRITICAL','关键任务延期超过30天','调整实验资源并压缩非关键工作',21,'RULE','SCHEDULE_LAG','OPEN','2026-09-01 09:00:00','system');

INSERT INTO research_issue(issue_id,project_id,source_risk_id,issue_no,title,description,severity,owner_user_id,occurred_at,due_date,status,create_by) VALUES
(11001,6004,10002,'ISS-0001','节能策略实验排期已延期','原计划8月底完成的关键实验尚未完成。','HIGH',21,'2026-09-05 09:00:00','2026-10-15','IN_PROGRESS','researcher');

INSERT INTO research_decision(project_id,decision_no,title,context,decision,reason,decision_maker_user_id,related_risk_id,decided_at,create_by) VALUES
(6001,'DEC-0001','采用离线验证作为现场测试备选','现场验证窗口不足。','并行准备离线回放测试环境。','降低现场排期对整体项目的影响。',21,10001,'2026-09-15 14:00:00','researcher');

INSERT INTO research_change_request(change_id,project_id,change_no,title,reason,applicant_user_id,schedule_impact,cost_impact,risk_impact,status,submitted_at,assessed_at,approved_at,create_by) VALUES
(12001,6001,'CR-2026-0001','现场验证计划调整','现场窗口不足，需要将项目结束时间延后一个月。',21,'结束日期由2026-12-20调整至2027-01-20','预算不变','降低赶工和验证不足风险','APPROVED','2026-09-20 10:00:00','2026-09-21 10:00:00','2026-09-22 10:00:00','researcher');
INSERT INTO research_change_item(change_id,change_type,field_code,before_value,after_value,description) VALUES
(12001,'SCHEDULE','planned_end_date','2026-12-20','2027-01-20','项目计划结束日期调整');

INSERT INTO research_budget(budget_id,project_id,baseline_id,version_no,total_amount,status,effective_at) VALUES
(13001,6001,7001,1,1100000,'ACTIVE','2026-01-10 09:00:00'),
(13003,6003,7003,1,420000,'ACTIVE','2025-01-01 09:00:00'),
(13004,6004,7004,1,280000,'ACTIVE','2026-03-01 09:00:00');
INSERT INTO research_budget_line(budget_line_id,budget_id,category,planned_amount,description) VALUES
(13101,13001,'设备费',280000,'工业相机及边缘设备'),(13102,13001,'材料费',180000,'实验耗材'),(13103,13001,'测试费',240000,'现场及第三方测试'),(13104,13001,'人员费',320000,'研究投入'),(13105,13001,'其他',80000,'其他'),
(13401,13004,'设备费',120000,'采集设备'),(13402,13004,'测试费',100000,'场景实验'),(13403,13004,'其他',60000,'其他');
INSERT INTO research_expense(project_id,budget_line_id,expense_no,expense_date,amount,description,create_by) VALUES
(6001,13101,'EXP-001','2026-02-15',180000,'工业相机及边缘计算设备','researcher'),
(6001,13102,'EXP-002','2026-05-10',95000,'样本及实验耗材','researcher'),
(6001,13103,'EXP-003','2026-08-08',120000,'第三方性能测试','researcher'),
(6004,13401,'EXP-004','2026-04-01',100000,'能耗采集设备','researcher');

INSERT INTO research_outcome(project_id,expected_output_id,outcome_type,name,description,status,completed_date,create_by) VALUES
(6001,3001,'SOFTWARE','工业缺陷检测算法原型 V1','完成第一版可演示算法原型。','COMPLETED','2026-06-28','researcher'),
(6003,3004,'REPORT','智能制造知识图谱结题报告','正式结题报告。','COMPLETED','2025-12-20','researcher');

INSERT INTO research_acceptance(acceptance_id,project_id,acceptance_no,project_summary,completion_statement,outstanding_items,applicant_user_id,status,submitted_at,reviewed_at,review_comment) VALUES
(14003,6003,'ACC-2025-0003','完成知识图谱构建方法研究。','全部批准任务完成。','无重大未完成事项。',21,'APPROVED','2026-01-05 10:00:00','2026-01-08 09:00:00','材料完整，同意验收。');
INSERT INTO research_closeout(closeout_id,project_id,acceptance_id,final_report_complete,finance_complete,outputs_complete,documents_complete,issues_complete,archive_complete,conclusion,status,completed_by,completed_at) VALUES
(15003,6003,14003,'1','1','1','1','1','1','验收完成，资料归档完整。','COMPLETED',20,'2026-01-08 10:00:00');

INSERT INTO research_workflow_instance(workflow_id,workflow_type,business_type,business_id,status,current_step,started_by,started_at,completed_at) VALUES
(16001,'PROPOSAL_APPROVAL','PROPOSAL',2001,'COMPLETED','MANAGEMENT',21,'2026-01-06 10:00:00','2026-01-08 14:00:00'),
(16002,'PROPOSAL_APPROVAL','PROPOSAL',2002,'RUNNING','TECHNICAL',21,sysdate(),NULL),
(16003,'CHANGE_APPROVAL','CHANGE_REQUEST',12001,'COMPLETED','MANAGEMENT',21,'2026-09-20 10:00:00','2026-09-22 10:00:00'),
(16004,'ACCEPTANCE','ACCEPTANCE',14003,'COMPLETED','MANAGEMENT',21,'2026-01-05 10:00:00','2026-01-08 09:00:00');
INSERT INTO research_workflow_action(workflow_id,step_code,action,operator_user_id,comment,acted_at) VALUES
(16001,'SUBMIT','SUBMIT',21,'提交申报','2026-01-06 10:00:00'),
(16001,'MANAGEMENT','APPROVE',20,'同意立项','2026-01-08 14:00:00'),
(16002,'SUBMIT','SUBMIT',21,'提交申报',sysdate()),
(16003,'SUBMIT','SUBMIT',21,'提交变更','2026-09-20 10:00:00'),
(16003,'MANAGEMENT','APPROVE',20,'同意调整计划','2026-09-22 10:00:00'),
(16004,'SUBMIT','SUBMIT',21,'提交验收','2026-01-05 10:00:00'),
(16004,'MANAGEMENT','APPROVE',20,'验收通过','2026-01-08 09:00:00');
