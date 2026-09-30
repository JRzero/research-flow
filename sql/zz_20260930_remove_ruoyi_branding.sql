-- ResearchFlow visible-branding cleanup
-- Safe to run repeatedly on an existing MySQL 8 database.

-- Remove legacy framework external links from the management menu.
DELETE FROM sys_menu
WHERE menu_name IN ('若依官网')
   OR path IN ('http://ruoyi.vip','https://ruoyi.vip','http://www.ruoyi.vip','https://www.ruoyi.vip');

-- Neutralize legacy demo identities that can appear in the admin UI.
UPDATE sys_user
SET nick_name='系统管理员',
    email='admin@researchflow.local',
    phonenumber='13800000001'
WHERE user_id=1 AND user_name='admin';

UPDATE sys_user
SET user_name='demo_user',
    nick_name='演示用户',
    email='demo@researchflow.local',
    phonenumber='13800000002'
WHERE user_id=2 AND user_name='ry';

-- Remove framework-branded department contacts if they still exist.
UPDATE sys_dept
SET leader=CASE WHEN leader='若依' THEN '系统管理员' ELSE leader END,
    email=CASE WHEN email IN ('ry@qq.com','ry@163.com') THEN 'admin@researchflow.local' ELSE email END,
    phone=CASE WHEN phone IN ('15888888888','15666666666') THEN '13800000000' ELSE phone END
WHERE leader='若依'
   OR email IN ('ry@qq.com','ry@163.com')
   OR phone IN ('15888888888','15666666666');

-- Replace legacy framework notices that may surface in the notification menu.
DELETE FROM sys_notice
WHERE notice_title LIKE '%若依%'
   OR notice_content LIKE '%RuoYi%'
   OR notice_content LIKE '%ruoyi.vip%'
   OR notice_content LIKE '%若依%';

INSERT INTO sys_notice(
  notice_id, notice_title, notice_type, notice_content, status,
  create_by, create_time, update_by, update_time, remark
)
SELECT 1,'ResearchFlow 使用说明','2','科研项目管理演示环境已就绪。','0','admin',SYSDATE(),'',NULL,'系统管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_notice WHERE notice_id=1);

INSERT INTO sys_notice(
  notice_id, notice_title, notice_type, notice_content, status,
  create_by, create_time, update_by, update_time, remark
)
SELECT 2,'系统维护通知','1','如需维护，请提前通知当前项目成员。','0','admin',SYSDATE(),'',NULL,'系统管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_notice WHERE notice_id=2);
