---
name: sql-table-generator
description: Generate normalized MySQL 8.0 CREATE TABLE SQL from Chinese table and field descriptions, following project naming, comments, charset, and audit-field conventions.
---

# SQL表创建语句生成器

根据输入的表名和字段信息，生成符合规范的MySQL 8.0建表SQL语句。

## 输入格式

请按以下格式输入：
- 表名：中文名称
- 字段：字段1中文名, 字段2中文名, ...

示例：
- 表名：产品信息
- 字段：产品编码、产品名称, 产品类型, 碳排放量

## 生成规则

1. 表名以 `t_` 开头，使用英文小写加下划线命名
2. 列名使用英文小写加下划线命名
3. 将中文表名和列名翻译成英文
4. id 为 bigint 类型，自增主键
5. 包含完整的 COMMENT 信息
6. 表编码：utf8mb4，排序：utf8mb4_0900_ai_ci
7. 默认字段：
   - 开头：`id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID'
   - 结尾：remark, status, del_flag, create_by, create_time, update_by, update_time

## 输出模板

```sql
DROP TABLE IF EXISTS `t_table_name`;
CREATE TABLE `t_table_name` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `column_name` data_type NULL/NOT NULL COMMENT '列注释',
  ...
  `remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
  `status` char(1) NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标识(0：正常 ;1：删除)',
  `create_by` varchar(50) NOT NULL DEFAULT 'admin' COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) NULL DEFAULT 'admin' COMMENT '最后修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表注释';
```

## 字段类型推断规则

根据字段名称推断合适的数据类型：
- 编码、代码 → varchar(50)
- 名称、标题 → varchar(100)
- 描述、内容、地址 → varchar(500)
- 数量、次数 → int
- 金额、价格 → decimal(18,2)
- 比例、百分比 → decimal(5,2)
- 重量、排放量 → decimal(18,4)
- 日期 → date
- 时间 → datetime
- 状态、类型 → char(1) 或 varchar(10)
- 是否 → tinyint(1)
- 备注 → varchar(500)

