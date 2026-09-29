-- ResearchFlow V2
-- 科研事项统一身份 + Proposal / Award / Project 分离 + Baseline / Execution / Governance 分层
-- Demo 数据库可直接重建，不兼容 V1 业务表。

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

-- V1 tables that are replaced by V2 models.
DROP TABLE IF EXISTS research_approval;
DROP TABLE IF EXISTS research_deliverable;
DROP TABLE IF EXISTS research_expense_v1;
DROP TABLE IF EXISTS research_progress;
DROP TABLE IF EXISTS research_milestone;

CREATE TABLE research_record (
  record_id BIGINT NOT NULL AUTO_INCREMENT,
  record_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  record_type VARCHAR(32) NOT NULL DEFAULT 'RESEARCH_PROJECT',
  source_type VARCHAR(32) DEFAULT 'INTERNAL',
  current_phase VARCHAR(32) NOT NULL DEFAULT 'PROPOSAL',
  owner_user_id BIGINT NOT NULL,
  dept_id BIGINT DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  closed_at DATETIME DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (record_id),
  UNIQUE KEY uk_record_no(record_no),
  KEY idx_record_owner(owner_user_id),
  KEY idx_record_dept(dept_id),
  KEY idx_record_phase(current_phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研事项统一记录';

CREATE TABLE research_call (
  call_id BIGINT NOT NULL AUTO_INCREMENT,
  call_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  call_type VARCHAR(32) DEFAULT 'INTERNAL',
  sponsor_org VARCHAR(200),
  funding_source VARCHAR(200),
  description TEXT,
  requirements TEXT,
  application_start DATE,
  application_deadline DATE,
  max_budget DECIMAL(16,2),
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(call_id),
  UNIQUE KEY uk_call_no(call_no),
  KEY idx_call_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研指南/项目机会';

CREATE TABLE research_proposal (
  proposal_id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  call_id BIGINT DEFAULT NULL,
  proposal_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  applicant_dept_id BIGINT DEFAULT NULL,
  background TEXT,
  objectives TEXT,
  project_scope TEXT,
  out_of_scope TEXT,
  research_content LONGTEXT,
  methodology LONGTEXT,
  innovation_points TEXT,
  success_criteria TEXT,
  planned_start_date DATE,
  planned_end_date DATE,
  requested_budget DECIMAL(16,2) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 0,
  submitted_at DATETIME,
  approved_at DATETIME,
  rejected_at DATETIME,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(proposal_id),
  UNIQUE KEY uk_proposal_no(proposal_no),
  KEY idx_proposal_record(record_id),
  KEY idx_proposal_call(call_id),
  KEY idx_proposal_status(status),
  KEY idx_proposal_applicant(applicant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研项目申报';

CREATE TABLE research_proposal_member (
  member_id BIGINT NOT NULL AUTO_INCREMENT,
  proposal_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  member_role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
  responsibility VARCHAR(1000),
  planned_allocation DECIMAL(5,2),
  sort_order INT DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(member_id),
  UNIQUE KEY uk_proposal_member(proposal_id,user_id),
  KEY idx_pm_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报团队';

CREATE TABLE research_proposal_budget_line (
  budget_line_id BIGINT NOT NULL AUTO_INCREMENT,
  proposal_id BIGINT NOT NULL,
  category VARCHAR(64) NOT NULL,
  amount DECIMAL(16,2) NOT NULL DEFAULT 0,
  description VARCHAR(1000),
  sort_order INT DEFAULT 0,
  PRIMARY KEY(budget_line_id),
  KEY idx_pbl_proposal(proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报预算明细';

CREATE TABLE research_expected_output (
  expected_output_id BIGINT NOT NULL AUTO_INCREMENT,
  proposal_id BIGINT NOT NULL,
  output_type VARCHAR(32) NOT NULL,
  name VARCHAR(200),
  target_quantity INT DEFAULT 1,
  target_description VARCHAR(1000),
  PRIMARY KEY(expected_output_id),
  KEY idx_expected_output_proposal(proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划科研成果';

CREATE TABLE research_review (
  review_id BIGINT NOT NULL AUTO_INCREMENT,
  proposal_id BIGINT NOT NULL,
  review_type VARCHAR(32) NOT NULL DEFAULT 'MANAGEMENT',
  reviewer_user_id BIGINT,
  score DECIMAL(5,2),
  decision VARCHAR(32),
  comment TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  started_at DATETIME,
  completed_at DATETIME,
  PRIMARY KEY(review_id),
  KEY idx_review_proposal(proposal_id),
  KEY idx_review_reviewer(reviewer_user_id),
  KEY idx_review_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研申报评审';

CREATE TABLE research_award (
  award_id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  proposal_id BIGINT NOT NULL,
  award_no VARCHAR(64) NOT NULL,
  approved_title VARCHAR(200),
  approved_start_date DATE NOT NULL,
  approved_end_date DATE NOT NULL,
  approved_budget DECIMAL(16,2) NOT NULL,
  approved_scope TEXT,
  approved_objectives TEXT,
  approved_outputs TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
  issued_at DATETIME NOT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(award_id),
  UNIQUE KEY uk_award_no(award_no),
  UNIQUE KEY uk_award_proposal(proposal_id),
  KEY idx_award_record(record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目立项批复';

CREATE TABLE research_project (
  project_id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  proposal_id BIGINT NOT NULL,
  award_id BIGINT NOT NULL,
  project_no VARCHAR(64) NOT NULL,
  project_name VARCHAR(200) NOT NULL,
  pi_user_id BIGINT NOT NULL,
  dept_id BIGINT DEFAULT NULL,
  current_baseline_id BIGINT DEFAULT NULL,
  current_planned_start_date DATE,
  current_planned_end_date DATE,
  current_budget DECIMAL(16,2) NOT NULL DEFAULT 0,
  progress INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PLANNING',
  version INT NOT NULL DEFAULT 0,
  activated_at DATETIME,
  suspended_at DATETIME,
  closing_at DATETIME,
  closed_at DATETIME,
  terminated_at DATETIME,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(project_id),
  UNIQUE KEY uk_project_no(project_no),
  UNIQUE KEY uk_project_record(record_id),
  UNIQUE KEY uk_project_award(award_id),
  KEY idx_project_pi(pi_user_id),
  KEY idx_project_dept(dept_id),
  KEY idx_project_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正式科研项目';

CREATE TABLE research_project_member (
  member_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  member_role VARCHAR(32) NOT NULL,
  responsibility VARCHAR(1000),
  allocation_percent DECIMAL(5,2),
  join_date DATE,
  leave_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(member_id),
  UNIQUE KEY uk_project_member(project_id,user_id),
  KEY idx_project_member_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正式项目团队';

CREATE TABLE research_baseline (
  baseline_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT,
  planned_start_date DATE,
  planned_end_date DATE,
  approved_budget DECIMAL(16,2),
  scope_snapshot JSON,
  objective_snapshot JSON,
  output_snapshot JSON,
  work_plan_snapshot JSON,
  budget_snapshot JSON,
  effective_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by_user_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(baseline_id),
  UNIQUE KEY uk_project_baseline(project_id,version_no),
  KEY idx_baseline_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变项目基线';

CREATE TABLE research_work_item (
  work_item_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  item_type VARCHAR(32) NOT NULL,
  wbs_code VARCHAR(64),
  title VARCHAR(200) NOT NULL,
  description TEXT,
  owner_user_id BIGINT,
  planned_start_date DATE,
  planned_end_date DATE,
  actual_start_date DATE,
  actual_end_date DATE,
  weight DECIMAL(6,2),
  progress INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  sort_order INT DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(work_item_id),
  KEY idx_work_project(project_id),
  KEY idx_work_parent(parent_id),
  KEY idx_work_owner(owner_user_id),
  KEY idx_work_status(project_id,status),
  KEY idx_work_type(project_id,item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一WBS工作项';

CREATE TABLE research_progress_report (
  report_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  report_no VARCHAR(64),
  report_type VARCHAR(32) NOT NULL,
  period_start DATE,
  period_end DATE,
  overall_progress INT,
  completed_work TEXT,
  key_achievements TEXT,
  problems TEXT,
  risks TEXT,
  next_plan TEXT,
  support_needed TEXT,
  budget_summary TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  prepared_by BIGINT NOT NULL,
  submitted_at DATETIME,
  reviewed_at DATETIME,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(report_id),
  KEY idx_progress_project(project_id),
  KEY idx_progress_type(project_id,report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目进展/中期报告';

CREATE TABLE research_risk (
  risk_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  risk_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  category VARCHAR(64),
  probability INT NOT NULL,
  impact INT NOT NULL,
  score INT NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  trigger_condition TEXT,
  response_strategy TEXT,
  owner_user_id BIGINT,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  rule_code VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  identified_at DATETIME NOT NULL,
  closed_at DATETIME,
  version INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(risk_id),
  UNIQUE KEY uk_project_risk_no(project_id,risk_no),
  KEY idx_risk_project(project_id),
  KEY idx_risk_level(project_id,risk_level),
  KEY idx_risk_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险登记册';

CREATE TABLE research_issue (
  issue_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  source_risk_id BIGINT,
  issue_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  severity VARCHAR(32) NOT NULL,
  owner_user_id BIGINT,
  occurred_at DATETIME,
  due_date DATE,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  resolution TEXT,
  resolved_at DATETIME,
  closed_at DATETIME,
  version INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(issue_id),
  UNIQUE KEY uk_project_issue_no(project_id,issue_no),
  KEY idx_issue_project(project_id),
  KEY idx_issue_owner(owner_user_id),
  KEY idx_issue_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目问题日志';

CREATE TABLE research_decision (
  decision_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  decision_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  context TEXT,
  decision TEXT NOT NULL,
  reason TEXT,
  decision_maker_user_id BIGINT,
  related_risk_id BIGINT,
  related_issue_id BIGINT,
  related_change_id BIGINT,
  decided_at DATETIME NOT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(decision_id),
  UNIQUE KEY uk_project_decision_no(project_id,decision_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目决策日志';

CREATE TABLE research_change_request (
  change_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  change_no VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  reason TEXT NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  scope_impact TEXT,
  schedule_impact TEXT,
  cost_impact TEXT,
  output_impact TEXT,
  risk_impact TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  version INT NOT NULL DEFAULT 0,
  submitted_at DATETIME,
  assessed_at DATETIME,
  approved_at DATETIME,
  rejected_at DATETIME,
  applied_at DATETIME,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(change_id),
  UNIQUE KEY uk_change_no(change_no),
  KEY idx_change_project(project_id),
  KEY idx_change_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目变更申请';

CREATE TABLE research_change_item (
  change_item_id BIGINT NOT NULL AUTO_INCREMENT,
  change_id BIGINT NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  field_code VARCHAR(64),
  before_value TEXT,
  after_value TEXT,
  description VARCHAR(1000),
  PRIMARY KEY(change_item_id),
  KEY idx_change_item_change(change_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目变更项';

CREATE TABLE research_budget (
  budget_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  baseline_id BIGINT,
  version_no INT NOT NULL,
  total_amount DECIMAL(16,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  effective_at DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(budget_id),
  UNIQUE KEY uk_project_budget_version(project_id,version_no),
  KEY idx_budget_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目预算版本';

CREATE TABLE research_budget_line (
  budget_line_id BIGINT NOT NULL AUTO_INCREMENT,
  budget_id BIGINT NOT NULL,
  category VARCHAR(64) NOT NULL,
  planned_amount DECIMAL(16,2) NOT NULL DEFAULT 0,
  description VARCHAR(1000),
  PRIMARY KEY(budget_line_id),
  KEY idx_budget_line_budget(budget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目预算科目';

CREATE TABLE research_expense (
  expense_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  budget_line_id BIGINT,
  expense_no VARCHAR(64),
  expense_date DATE NOT NULL,
  amount DECIMAL(16,2) NOT NULL,
  description VARCHAR(1000),
  reference_no VARCHAR(100),
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(expense_id),
  KEY idx_expense_project(project_id),
  KEY idx_expense_budget_line(budget_line_id),
  KEY idx_expense_date(project_id,expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目预算执行记录';

CREATE TABLE research_outcome (
  outcome_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  expected_output_id BIGINT,
  outcome_type VARCHAR(32) NOT NULL,
  name VARCHAR(300) NOT NULL,
  description TEXT,
  status VARCHAR(32),
  completed_date DATE,
  external_reference VARCHAR(500),
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(outcome_id),
  KEY idx_outcome_project(project_id),
  KEY idx_outcome_expected(expected_output_id),
  KEY idx_outcome_type(project_id,outcome_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际科研成果';

CREATE TABLE research_document (
  document_id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  business_type VARCHAR(32) NOT NULL,
  business_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(100),
  file_size BIGINT,
  version_no INT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  uploaded_by BIGINT NOT NULL,
  uploaded_at DATETIME NOT NULL,
  PRIMARY KEY(document_id),
  KEY idx_doc_record(record_id),
  KEY idx_doc_business(business_type,business_id),
  KEY idx_doc_category(business_type,business_id,category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研业务文档元数据';

CREATE TABLE research_acceptance (
  acceptance_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  acceptance_no VARCHAR(64) NOT NULL,
  project_summary LONGTEXT,
  completion_statement LONGTEXT,
  outstanding_items TEXT,
  applicant_user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  submitted_at DATETIME,
  reviewed_at DATETIME,
  review_comment TEXT,
  PRIMARY KEY(acceptance_id),
  UNIQUE KEY uk_acceptance_no(acceptance_no),
  UNIQUE KEY uk_acceptance_project(project_id),
  KEY idx_acceptance_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目验收';

CREATE TABLE research_closeout (
  closeout_id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  acceptance_id BIGINT NOT NULL,
  final_report_complete CHAR(1) NOT NULL DEFAULT '0',
  finance_complete CHAR(1) NOT NULL DEFAULT '0',
  outputs_complete CHAR(1) NOT NULL DEFAULT '0',
  documents_complete CHAR(1) NOT NULL DEFAULT '0',
  issues_complete CHAR(1) NOT NULL DEFAULT '0',
  archive_complete CHAR(1) NOT NULL DEFAULT '0',
  conclusion TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  completed_by BIGINT,
  completed_at DATETIME,
  PRIMARY KEY(closeout_id),
  UNIQUE KEY uk_closeout_project(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目结项归档';

CREATE TABLE research_workflow_instance (
  workflow_id BIGINT NOT NULL AUTO_INCREMENT,
  workflow_type VARCHAR(32) NOT NULL,
  business_type VARCHAR(32) NOT NULL,
  business_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  current_step VARCHAR(64),
  started_by BIGINT NOT NULL,
  started_at DATETIME NOT NULL,
  completed_at DATETIME,
  PRIMARY KEY(workflow_id),
  KEY idx_workflow_business(business_type,business_id),
  KEY idx_workflow_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轻量业务流程实例';

CREATE TABLE research_workflow_action (
  action_id BIGINT NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT NOT NULL,
  step_code VARCHAR(64),
  action VARCHAR(32) NOT NULL,
  operator_user_id BIGINT NOT NULL,
  comment TEXT,
  acted_at DATETIME NOT NULL,
  PRIMARY KEY(action_id),
  KEY idx_action_workflow(workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程审计动作';

-- Demo roles/users. Password: admin123
INSERT IGNORE INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
(20, '科研管理员', 'research_admin', 20, '1', 1, 1, '0', '0', 'admin', sysdate(), '申报评审、立项、变更、验收和全局科研管理'),
(21, '科研人员', 'research_owner', 21, '5', 1, 1, '0', '0', 'admin', sysdate(), '科研申报与项目执行'),
(22, '管理者', 'research_manager', 22, '1', 1, 1, '0', '0', 'admin', sysdate(), '科研组合视角只读管理');

INSERT IGNORE INTO sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
VALUES
(20, 103, 'research_admin', '科研管理员', '00', 'research-admin@example.local', '13800000020', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'V2演示账号'),
(21, 105, 'researcher', '张伟', '00', 'researcher@example.local', '13800000021', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'V2演示账号'),
(22, 103, 'research_manager', '科研管理负责人', '00', 'manager@example.local', '13800000022', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'V2演示账号');

INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES (20,20),(21,21),(22,22);

-- Demo call.
INSERT INTO research_call(call_id,call_no,title,call_type,sponsor_org,funding_source,description,requirements,application_start,application_deadline,max_budget,status,create_by)
VALUES (1001,'CALL-2026-01','2026年度智能制造关键技术专项','INTERNAL','先进技术研究院','院级科研专项','面向智能制造关键技术形成可工程验证的科研成果。','需明确技术路线、团队、预算、计划成果与风险。','2026-01-01','2026-10-31',1500000,'OPEN','research_admin');

-- Proposal 1 approved and converted to active project.
INSERT INTO research_record(record_id,record_no,title,current_phase,owner_user_id,dept_id,create_by)
VALUES (2001,'RR-2026-000001','AI驱动工业质量检测关键技术研究','EXECUTION',21,105,'researcher'),
       (2002,'RR-2026-000002','科研数据智能治理平台预研','REVIEW',21,105,'researcher'),
       (2003,'RR-2026-000003','低碳实验室能耗优化研究','EXECUTION',21,109,'researcher');

INSERT INTO research_proposal(proposal_id,record_id,call_id,proposal_no,title,applicant_user_id,applicant_dept_id,background,objectives,project_scope,out_of_scope,research_content,methodology,innovation_points,success_criteria,planned_start_date,planned_end_date,requested_budget,status,submitted_at,approved_at,create_by)
VALUES
(2101,2001,1001,'PR-2026-000001','AI驱动工业质量检测关键技术研究',21,105,'现有人工质检效率低且一致性不足。','形成工业缺陷识别算法与可验证原型。','缺陷数据集、算法模型、现场验证。','不建设生产MES集成。','数据建设、模型研发、现场验证。','多模态模型+工业现场验证。','面向小样本缺陷的可解释识别。','准确率>=95%，完成原型和技术报告。','2026-01-10','2026-12-20',1200000,'APPROVED','2026-01-05 10:00:00','2026-01-08 14:00:00','researcher'),
(2102,2002,1001,'PR-2026-000002','科研数据智能治理平台预研',21,105,'科研数据分散，缺少统一治理方式。','验证跨项目科研数据治理方法。','数据标准、元数据治理和检索原型。','不建设正式数据中台。','数据标准、治理流程、原型开发。','元数据模型+轻量知识检索。','以科研项目记录为统一上下文。','完成可演示原型与研究报告。','2026-11-01','2027-06-30',500000,'UNDER_REVIEW',sysdate(),NULL,'researcher'),
(2103,2003,NULL,'PR-2026-000003','低碳实验室能耗优化研究',21,109,'实验室设备能耗缺少精细化优化。','形成能耗监测和优化策略。','数据采集、基线建模、策略验证。','不改造实验室供配电系统。','采集、分析、策略优化。','规则+预测模型结合。','核心场景节能率达到10%。','2026-03-01','2026-09-15',300000,'APPROVED','2026-02-18 09:00:00','2026-02-25 10:00:00','researcher');

INSERT INTO research_proposal_member(proposal_id,user_id,member_role,responsibility,planned_allocation,sort_order,create_by)
VALUES (2101,21,'PI','总体技术路线与项目交付',60,1,'researcher'),
       (2102,21,'PI','总体方案与原型实现',70,1,'researcher'),
       (2103,21,'PI','算法研究与项目管理',60,1,'researcher');

INSERT INTO research_proposal_budget_line(proposal_id,category,amount,description,sort_order)
VALUES (2101,'设备费',300000,'工业相机及边缘计算设备',1),
       (2101,'材料费',200000,'样本及实验耗材',2),
       (2101,'测试费',200000,'第三方及现场测试',3),
       (2101,'人员费',350000,'项目研究投入',4),
       (2101,'其他',150000,'差旅、会议及其他',5),
       (2102,'设备费',100000,'开发测试设备',1),
       (2102,'人员费',300000,'研发投入',2),
       (2102,'其他',100000,'资料、会议及其他',3),
       (2103,'设备费',130000,'能耗采集设备',1),
       (2103,'测试费',90000,'实验场景测试',2),
       (2103,'人员费',80000,'研究投入',3);

INSERT INTO research_expected_output(proposal_id,output_type,name,target_quantity,target_description)
VALUES (2101,'SOFTWARE','工业缺陷检测原型系统',1,'完成可演示原型'),
       (2101,'REPORT','项目技术报告',1,'形成完整技术报告'),
       (2101,'PATENT','相关发明专利',1,'至少申请1项'),
       (2102,'SOFTWARE','科研数据治理原型',1,'可演示'),
       (2102,'REPORT','预研报告',1,'形成可评审报告'),
       (2103,'REPORT','能耗优化研究报告',1,'完成研究报告');

INSERT INTO research_review(proposal_id,review_type,reviewer_user_id,score,decision,comment,status,started_at,completed_at)
VALUES (2101,'MANAGEMENT',20,91,'PASS','技术路线清晰，同意立项。','COMPLETED','2026-01-07 09:00:00','2026-01-08 14:00:00'),
       (2102,'TECHNICAL',20,NULL,NULL,'进入技术评审。','PENDING',sysdate(),NULL),
       (2103,'MANAGEMENT',20,88,'PASS','同意立项。','COMPLETED','2026-02-24 09:00:00','2026-02-25 10:00:00');

INSERT INTO research_award(award_id,record_id,proposal_id,award_no,approved_title,approved_start_date,approved_end_date,approved_budget,approved_scope,approved_objectives,approved_outputs,status,issued_at,create_by)
VALUES
(2201,2001,2101,'AW-2026-000001','AI驱动工业质量检测关键技术研究','2026-01-10','2026-12-20',1000000,'缺陷数据集、算法模型、现场验证。','形成可解释工业缺陷识别能力。','原型系统、技术报告、专利。','ISSUED','2026-01-09 09:00:00','research_admin'),
(2203,2003,2103,'AW-2026-000003','低碳实验室能耗优化研究','2026-03-01','2026-09-15',300000,'数据采集、基线建模、策略验证。','形成能耗优化算法及示范。','研究报告、算法模型。','ISSUED','2026-02-26 09:00:00','research_admin');

INSERT INTO research_project(project_id,record_id,proposal_id,award_id,project_no,project_name,pi_user_id,dept_id,current_planned_start_date,current_planned_end_date,current_budget,progress,status,activated_at,create_by)
VALUES
(3001,2001,2101,2201,'RF-2026-001','AI驱动工业质量检测关键技术研究',21,105,'2026-01-10','2026-12-20',1000000,58,'ACTIVE','2026-01-10 09:00:00','research_admin'),
(3003,2003,2103,2203,'RF-2026-003','低碳实验室能耗优化研究',21,109,'2026-03-01','2026-09-15',300000,42,'ACTIVE','2026-03-01 09:00:00','research_admin');

INSERT INTO research_project_member(project_id,user_id,member_role,responsibility,allocation_percent,join_date,status,create_by)
VALUES (3001,21,'PI','项目总体负责',60,'2026-01-10','ACTIVE','research_admin'),
       (3003,21,'PI','项目总体负责',60,'2026-03-01','ACTIVE','research_admin');

INSERT INTO research_work_item(work_item_id,project_id,parent_id,item_type,wbs_code,title,description,owner_user_id,planned_start_date,planned_end_date,actual_start_date,actual_end_date,weight,progress,status,priority,sort_order,create_by)
VALUES
(4001,3001,NULL,'PHASE','1','数据与算法研发','完成数据建设和算法研发',21,'2026-01-10','2026-06-30','2026-01-10',NULL,50,100,'DONE','HIGH',1,'researcher'),
(4002,3001,4001,'TASK','1.1','完成工业缺陷数据集V1','完成第一版数据集建设',21,'2026-01-10','2026-03-31','2026-01-10','2026-03-25',20,100,'DONE','HIGH',1,'researcher'),
(4003,3001,4001,'MILESTONE','M1','算法原型达到内部验证标准','完成算法原型及第一轮性能验证',21,'2026-04-01','2026-06-30','2026-04-01','2026-06-28',30,100,'DONE','HIGH',2,'researcher'),
(4004,3001,NULL,'PHASE','2','现场验证与成果形成','现场适配、验证和成果整理',21,'2026-07-01','2026-12-20','2026-07-01',NULL,50,35,'IN_PROGRESS','HIGH',2,'researcher'),
(4005,3001,4004,'TASK','2.1','完成现场适配','完成现场环境适配和模型优化',21,'2026-07-01','2026-09-30','2026-07-01',NULL,20,70,'IN_PROGRESS','HIGH',1,'researcher'),
(4006,3001,4004,'MILESTONE','M2','完成现场验证','至少完成一条产线现场验证',21,'2026-10-01','2026-10-31',NULL,NULL,20,0,'NOT_STARTED','HIGH',2,'researcher'),
(4007,3001,4004,'TASK','2.3','完成结题材料','整理技术报告、成果和验收资料',21,'2026-11-01','2026-12-20',NULL,NULL,10,0,'NOT_STARTED','MEDIUM',3,'researcher'),
(4010,3003,NULL,'TASK','1.1','完成节能策略验证','完成核心场景验证',21,'2026-04-01','2026-07-31','2026-04-01',NULL,100,42,'BLOCKED','HIGH',1,'researcher');

INSERT INTO research_budget(budget_id,project_id,baseline_id,version_no,total_amount,status,effective_at)
VALUES (5001,3001,NULL,1,1000000,'ACTIVE','2026-01-10 09:00:00'),
       (5003,3003,NULL,1,300000,'ACTIVE','2026-03-01 09:00:00');

INSERT INTO research_budget_line(budget_line_id,budget_id,category,planned_amount,description)
VALUES (5101,5001,'设备费',250000,'设备购置'),
       (5102,5001,'材料费',180000,'实验材料'),
       (5103,5001,'测试费',180000,'现场与第三方测试'),
       (5104,5001,'人员费',300000,'科研投入'),
       (5105,5001,'其他',90000,'其他项目支出'),
       (5131,5003,'设备费',130000,'采集设备'),
       (5132,5003,'测试费',90000,'场景测试'),
       (5133,5003,'人员费',80000,'科研投入');

INSERT INTO research_baseline(baseline_id,project_id,version_no,source_type,source_id,planned_start_date,planned_end_date,approved_budget,scope_snapshot,objective_snapshot,output_snapshot,work_plan_snapshot,budget_snapshot,effective_at,status,created_by_user_id)
VALUES
(6001,3001,1,'AWARD',2201,'2026-01-10','2026-12-20',1000000,JSON_OBJECT('scope','缺陷数据集、算法模型、现场验证'),JSON_OBJECT('objective','形成可解释工业缺陷识别能力'),JSON_OBJECT('outputs','原型系统、技术报告、专利'),JSON_ARRAY(JSON_OBJECT('wbs','1','name','数据与算法研发'),JSON_OBJECT('wbs','2','name','现场验证与成果形成')),JSON_OBJECT('total',1000000),'2026-01-10 09:00:00','ACTIVE',20),
(6003,3003,1,'AWARD',2203,'2026-03-01','2026-09-15',300000,JSON_OBJECT('scope','数据采集、基线建模、策略验证'),JSON_OBJECT('objective','形成能耗优化算法及示范'),JSON_OBJECT('outputs','研究报告、算法模型'),JSON_ARRAY(JSON_OBJECT('wbs','1.1','name','完成节能策略验证')),JSON_OBJECT('total',300000),'2026-03-01 09:00:00','ACTIVE',20);

UPDATE research_project SET current_baseline_id=6001 WHERE project_id=3001;
UPDATE research_project SET current_baseline_id=6003 WHERE project_id=3003;

INSERT INTO research_progress_report(project_id,report_no,report_type,period_start,period_end,overall_progress,completed_work,key_achievements,problems,risks,next_plan,support_needed,budget_summary,status,prepared_by,submitted_at,create_by)
VALUES
(3001,'RPT-2026-0001','QUARTERLY','2026-07-01','2026-09-30',58,'完成现场环境适配和第二轮模型优化。','算法在主要缺陷类型上达到目标。','产线部署窗口有限。','现场验证进度存在滞后风险。','协调现场验证并启动结题材料。','需要协调生产窗口。','预算执行处于可控范围。','SUBMITTED',21,'2026-09-20 18:00:00','researcher'),
(3003,'RPT-2026-0003','QUARTERLY','2026-06-01','2026-08-31',42,'完成能耗数据采集和基线模型。','形成基线能耗模型。','策略验证进度落后。','计划结束时间临近。','优先完成关键场景实验。','','设备及测试费用执行较快。','SUBMITTED',21,'2026-08-20 18:00:00','researcher');

INSERT INTO research_risk(risk_id,project_id,risk_no,title,description,category,probability,impact,score,risk_level,trigger_condition,response_strategy,owner_user_id,source,rule_code,status,identified_at,create_by)
VALUES
(7001,3001,'RISK-0001','现场验证窗口不足','生产线可用于测试的窗口有限，可能影响最终验证。','SCHEDULE',4,4,16,'HIGH','10月中旬前仍未获得连续测试窗口','提前锁定窗口并准备替代验证环境',21,'RULE','SCHEDULE_LAG','MONITORING','2026-09-20 18:10:00','system'),
(7003,3003,'RISK-0001','项目计划延期风险','计划结束时间临近但整体完成度偏低。','SCHEDULE',5,4,20,'CRITICAL','计划结束日前仍未完成策略验证','聚焦关键场景并申请必要的计划变更',21,'RULE','OVERDUE_PROGRESS','OPEN','2026-08-20 18:10:00','system');

INSERT INTO research_issue(issue_id,project_id,source_risk_id,issue_no,title,description,severity,owner_user_id,occurred_at,due_date,status,create_by)
VALUES (8001,3001,NULL,'ISS-0001','现场样本质量波动','部分现场样本光照和角度变化较大。','MEDIUM',21,'2026-09-12 09:00:00','2026-10-10','IN_PROGRESS','researcher');

INSERT INTO research_change_request(change_id,project_id,change_no,title,reason,applicant_user_id,schedule_impact,cost_impact,status,submitted_at,assessed_at,approved_at,create_by)
VALUES (9001,3001,'CR-2026-0001','现场验证周期调整','生产验证窗口变化，需要延长现场验证阶段。',21,'建议项目计划结束时间延长至2027-01-31','无新增预算','APPROVED','2026-09-22 10:00:00','2026-09-23 10:00:00','2026-09-24 10:00:00','researcher');
INSERT INTO research_change_item(change_id,change_type,field_code,before_value,after_value,description)
VALUES (9001,'SCHEDULE','planned_end_date','2026-12-20','2027-01-31','延长现场验证与结题准备周期');

INSERT INTO research_expense(project_id,budget_line_id,expense_no,expense_date,amount,description,create_by)
VALUES (3001,5101,'EXP-001','2026-02-15',180000,'工业相机及边缘计算设备','researcher'),
       (3001,5102,'EXP-002','2026-05-10',95000,'样本及实验耗材','researcher'),
       (3001,5103,'EXP-003','2026-08-08',120000,'第三方性能测试','researcher'),
       (3003,5131,'EXP-031','2026-04-01',130000,'能耗采集设备','researcher'),
       (3003,5132,'EXP-032','2026-06-20',90000,'实验场景测试','researcher');

INSERT INTO research_outcome(project_id,expected_output_id,outcome_type,name,description,status,completed_date,create_by)
VALUES (3001,1,'SOFTWARE','工业缺陷检测算法原型 V1','完成第一版算法原型。','COMPLETED','2026-06-28','researcher');

INSERT INTO research_workflow_instance(workflow_id,workflow_type,business_type,business_id,status,current_step,started_by,started_at,completed_at)
VALUES (10001,'PROPOSAL_APPROVAL','PROPOSAL',2101,'COMPLETED','DONE',21,'2026-01-05 10:00:00','2026-01-08 14:00:00'),
       (10002,'PROPOSAL_APPROVAL','PROPOSAL',2102,'RUNNING','TECHNICAL_REVIEW',21,sysdate(),NULL),
       (10003,'CHANGE_APPROVAL','CHANGE_REQUEST',9001,'COMPLETED','DONE',21,'2026-09-22 10:00:00','2026-09-24 10:00:00');

INSERT INTO research_workflow_action(workflow_id,step_code,action,operator_user_id,comment,acted_at)
VALUES (10001,'SUBMIT','SUBMIT',21,'提交申报','2026-01-05 10:00:00'),
       (10001,'MANAGEMENT_REVIEW','APPROVE',20,'同意立项','2026-01-08 14:00:00'),
       (10002,'SUBMIT','SUBMIT',21,'提交申报',sysdate()),
       (10003,'SUBMIT','SUBMIT',21,'提交变更申请','2026-09-22 10:00:00'),
       (10003,'CHANGE_REVIEW','APPROVE',20,'同意调整周期','2026-09-24 10:00:00');

UPDATE sys_dept SET dept_name='先进技术研究院', leader='科研负责人' WHERE dept_id=100;
UPDATE sys_dept SET dept_name='智能制造研究中心', leader='中心负责人' WHERE dept_id=101;
UPDATE sys_dept SET dept_name='材料与能源研究中心', leader='中心负责人' WHERE dept_id=102;
UPDATE sys_dept SET dept_name='科研管理处', leader='科研管理员' WHERE dept_id=103;
UPDATE sys_dept SET dept_name='综合管理办公室' WHERE dept_id=104;
UPDATE sys_dept SET dept_name='智能检测实验室', leader='实验室负责人' WHERE dept_id=105;
UPDATE sys_dept SET dept_name='财务管理处' WHERE dept_id=106;
UPDATE sys_dept SET dept_name='科研信息中心' WHERE dept_id=107;
UPDATE sys_dept SET dept_name='先进材料实验室' WHERE dept_id=108;
UPDATE sys_dept SET dept_name='低碳技术实验室' WHERE dept_id=109;
