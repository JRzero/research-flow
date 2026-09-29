-- ResearchFlow 科研项目全生命周期 MVP
-- 本文件会在 MySQL 首次启动时由 docker-entrypoint-initdb.d 自动执行。

DROP TABLE IF EXISTS research_acceptance;
DROP TABLE IF EXISTS research_approval;
DROP TABLE IF EXISTS research_deliverable;
DROP TABLE IF EXISTS research_expense;
DROP TABLE IF EXISTS research_progress;
DROP TABLE IF EXISTS research_milestone;
DROP TABLE IF EXISTS research_project;

CREATE TABLE research_project (
  project_id bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  project_no varchar(64) NOT NULL COMMENT '项目编号',
  project_name varchar(200) NOT NULL COMMENT '项目名称',
  owner_user_id bigint NOT NULL COMMENT '项目负责人用户ID',
  dept_id bigint DEFAULT NULL COMMENT '所属部门ID',
  summary text COMMENT '项目简介',
  research_objectives text COMMENT '研究目标',
  research_content text COMMENT '研究内容',
  start_date date DEFAULT NULL COMMENT '开始日期',
  planned_end_date date NOT NULL COMMENT '计划结束日期',
  total_budget decimal(16,2) NOT NULL DEFAULT 0 COMMENT '项目总预算',
  progress int NOT NULL DEFAULT 0 COMMENT '项目进度0-100',
  status varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '项目状态',
  expected_deliverables text COMMENT '预期成果',
  application_attachments longtext COMMENT '申报附件JSON',
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  PRIMARY KEY (project_id),
  UNIQUE KEY uk_research_project_no (project_no),
  KEY idx_research_project_owner (owner_user_id),
  KEY idx_research_project_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研项目';

CREATE TABLE research_milestone (
  milestone_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  title varchar(200) NOT NULL,
  due_date date DEFAULT NULL,
  completed_at datetime DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  description varchar(1000) DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL,
  PRIMARY KEY (milestone_id), KEY idx_milestone_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目里程碑';

CREATE TABLE research_progress (
  progress_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  record_date date NOT NULL,
  progress_percent int NOT NULL,
  completed_work text,
  issues text,
  next_plan text,
  recorder_user_id bigint NOT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  PRIMARY KEY (progress_id), KEY idx_progress_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目进展';

CREATE TABLE research_expense (
  expense_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  expense_date date NOT NULL,
  expense_type varchar(64) NOT NULL,
  amount decimal(16,2) NOT NULL,
  description varchar(1000) DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  PRIMARY KEY (expense_id), KEY idx_expense_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目支出';

CREATE TABLE research_deliverable (
  deliverable_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  name varchar(200) NOT NULL,
  type varchar(64) NOT NULL,
  completed_date date DEFAULT NULL,
  description varchar(1000) DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  PRIMARY KEY (deliverable_id), KEY idx_deliverable_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成果';

CREATE TABLE research_approval (
  approval_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  business_type varchar(64) NOT NULL,
  action varchar(32) NOT NULL,
  operator_id bigint NOT NULL,
  comment varchar(1000) DEFAULT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY (approval_id), KEY idx_approval_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目流程审计记录';

CREATE TABLE research_acceptance (
  acceptance_id bigint NOT NULL AUTO_INCREMENT,
  project_id bigint NOT NULL,
  project_summary text NOT NULL,
  completion_statement text,
  unfinished_items text,
  acceptance_note text,
  reviewer_id bigint DEFAULT NULL,
  review_comment varchar(1000) DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  submitted_at datetime DEFAULT NULL,
  reviewed_at datetime DEFAULT NULL,
  create_by varchar(64) DEFAULT '', create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '', update_time datetime DEFAULT NULL,
  PRIMARY KEY (acceptance_id), UNIQUE KEY uk_acceptance_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目验收';

-- MVP 演示角色。管理员 admin 仍拥有全部权限。
INSERT IGNORE INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
(20, '科研管理员', 'research_admin', 20, '1', 1, 1, '0', '0', 'admin', sysdate(), '负责审批、验收及全局科研项目管理'),
(21, '项目负责人', 'research_owner', 21, '5', 1, 1, '0', '0', 'admin', sysdate(), '负责项目申报和执行'),
(22, '管理者', 'research_manager', 22, '1', 1, 1, '0', '0', 'admin', sysdate(), '查看科研项目全局数据');

-- 默认密码与若依初始化账号保持一致，便于面试演示；正式环境必须修改。
INSERT IGNORE INTO sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
VALUES
(20, 103, 'research_admin', '科研管理员', '00', 'research-admin@example.local', '13800000020', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'MVP演示账号'),
(21, 105, 'researcher', '张伟', '00', 'researcher@example.local', '13800000021', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'MVP演示账号'),
(22, 103, 'research_manager', '科研管理负责人', '00', 'manager@example.local', '13800000022', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, 'MVP演示账号');

INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES (20,20),(21,21),(22,22);

-- 演示项目：覆盖待审批、执行中、风险、待验收、已结项等状态。
INSERT INTO research_project(project_id,project_no,project_name,owner_user_id,dept_id,summary,research_objectives,research_content,start_date,planned_end_date,total_budget,progress,status,expected_deliverables,create_by,create_time,update_by,update_time,remark) VALUES
(1001,'RF-2026-001','AI驱动工业质量检测关键技术研究',21,105,'研究多模态模型在工业外观质量检测中的应用。','建立可解释的工业缺陷识别方法并完成工程验证。','数据采集、模型训练、产线验证。','2026-01-10','2026-12-20',1200000,58,'IN_PROGRESS','算法原型、技术报告、软件系统','researcher','2026-01-05 09:00:00','researcher',sysdate(),'重点演示项目'),
(1002,'RF-2026-002','高可靠机器人控制方法研究',21,105,'面向复杂工况研究高可靠控制算法。','完成控制算法和实验平台验证。','控制模型、仿真、实验。','2026-05-01','2027-04-30',800000,25,'IN_PROGRESS','论文、技术报告','researcher','2026-04-15 09:00:00','researcher',sysdate(),NULL),
(1003,'RF-2026-003','科研数据智能治理平台预研',21,105,'建设科研数据治理与检索原型。','验证跨项目数据治理方法。','数据标准、治理流程、原型开发。','2026-10-01','2027-06-30',500000,0,'PENDING_APPROVAL','原型系统','researcher',sysdate(),'researcher',sysdate(),NULL),
(1004,'RF-2026-004','先进材料性能预测模型',21,105,'应用机器学习预测材料关键性能。','构建材料性能预测模型。','特征工程、模型训练、对比实验。','2025-11-01','2026-10-15',600000,88,'PENDING_ACCEPTANCE','论文、模型、实验报告','researcher','2025-10-10 09:00:00','researcher',sysdate(),NULL),
(1005,'RF-2025-005','智能制造知识图谱构建方法',21,105,'构建制造领域知识图谱方法体系。','形成知识建模和应用方法。','知识抽取、融合、应用验证。','2025-01-01','2025-12-31',450000,100,'COMPLETED','论文、知识图谱、结题报告','researcher','2024-12-15 09:00:00','research_admin','2026-01-08 10:00:00',NULL),
(1006,'RF-2026-006','低碳实验室能耗优化研究',21,105,'研究实验室能耗监测及优化策略。','形成能耗优化算法及示范。','采集、分析、策略优化。','2026-03-01','2026-09-15',300000,42,'IN_PROGRESS','研究报告、算法模型','researcher','2026-02-18 09:00:00','researcher',sysdate(),'用于展示延期风险');

INSERT INTO research_milestone(project_id,title,due_date,completed_at,status,description,create_by,create_time,update_time) VALUES
(1001,'完成数据集建设','2026-03-31','2026-03-25 10:00:00','COMPLETED','形成第一版工业缺陷数据集','researcher','2026-01-10 10:00:00',sysdate()),
(1001,'完成算法原型','2026-06-30','2026-06-28 10:00:00','COMPLETED','完成算法原型并内部评审','researcher','2026-01-10 10:00:00',sysdate()),
(1001,'完成现场验证','2026-10-31',NULL,'PENDING','完成至少一条产线现场验证','researcher','2026-01-10 10:00:00',sysdate()),
(1006,'完成节能策略验证','2026-07-31',NULL,'PENDING','验证核心节能策略','researcher','2026-03-01 10:00:00',sysdate());

INSERT INTO research_progress(project_id,record_date,progress_percent,completed_work,issues,next_plan,recorder_user_id,create_by,create_time) VALUES
(1001,'2026-03-25',25,'完成数据集一期建设。','部分现场样本质量不稳定。','补充异常工况样本并启动算法训练。',21,'researcher','2026-03-25 18:00:00'),
(1001,'2026-06-28',48,'完成算法原型和第一轮指标验证。','小样本缺陷召回率仍需提升。','优化数据增强并开展现场适配。',21,'researcher','2026-06-28 18:00:00'),
(1001,'2026-09-20',58,'完成现场环境适配和第二轮模型优化。','产线部署窗口有限。','协调现场验证并完成结题材料初稿。',21,'researcher','2026-09-20 18:00:00'),
(1006,'2026-08-20',42,'完成能耗数据采集和基线模型。','策略验证进度落后。','优先完成关键场景实验。',21,'researcher','2026-08-20 18:00:00');

INSERT INTO research_expense(project_id,expense_date,expense_type,amount,description,create_by,create_time) VALUES
(1001,'2026-02-15','设备费',180000,'工业相机及边缘计算设备','researcher','2026-02-15 10:00:00'),
(1001,'2026-05-10','材料费',95000,'样本及实验耗材','researcher','2026-05-10 10:00:00'),
(1001,'2026-08-08','测试费',120000,'第三方性能测试','researcher','2026-08-08 10:00:00'),
(1006,'2026-04-01','设备费',130000,'能耗采集设备','researcher','2026-04-01 10:00:00'),
(1006,'2026-06-20','测试费',90000,'实验场景测试','researcher','2026-06-20 10:00:00');

INSERT INTO research_deliverable(project_id,name,type,completed_date,description,create_by,create_time) VALUES
(1001,'工业缺陷检测算法原型 V1','软件','2026-06-28','完成第一版算法原型。','researcher','2026-06-28 18:10:00'),
(1005,'智能制造知识图谱结题报告','技术报告','2025-12-20','正式结题报告。','researcher','2025-12-20 10:00:00');

INSERT INTO research_approval(project_id,business_type,action,operator_id,comment,created_at) VALUES
(1001,'PROJECT_APPLICATION','SUBMIT',21,'提交项目申报','2026-01-06 10:00:00'),
(1001,'PROJECT_APPLICATION','APPROVE',20,'同意立项','2026-01-08 14:00:00'),
(1001,'PROJECT_EXECUTION','START',20,'项目正式启动','2026-01-10 09:00:00'),
(1003,'PROJECT_APPLICATION','SUBMIT',21,'提交项目申报',sysdate()),
(1004,'PROJECT_ACCEPTANCE','SUBMIT',21,'提交项目验收','2026-09-26 10:00:00'),
(1005,'PROJECT_ACCEPTANCE','APPROVE',20,'验收通过','2026-01-08 10:00:00');

INSERT INTO research_acceptance(project_id,project_summary,completion_statement,unfinished_items,acceptance_note,reviewer_id,review_comment,status,submitted_at,reviewed_at,create_by,create_time,update_by,update_time) VALUES
(1004,'项目已完成核心算法研究与实验验证。','计划任务基本完成，主要指标达到预期。','无重大未完成事项。','申请组织项目验收。',NULL,NULL,'PENDING','2026-09-26 10:00:00',NULL,'researcher','2026-09-26 10:00:00','',sysdate()),
(1005,'完成知识图谱构建方法研究。','全部计划任务完成。','','提交结题。',20,'材料完整，同意通过。','APPROVED','2026-01-05 10:00:00','2026-01-08 10:00:00','researcher','2026-01-05 10:00:00','research_admin','2026-01-08 10:00:00');

-- 将默认组织演示数据调整为科研院所语境，避免产品界面暴露框架示例名称。
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
