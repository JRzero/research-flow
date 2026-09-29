-- Add project application attachments for existing ResearchFlow databases.
-- Safe to run repeatedly on MySQL 8.

SET @rf_attachment_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'research_project'
    AND COLUMN_NAME = 'application_attachments'
);

SET @rf_attachment_sql = IF(
  @rf_attachment_column_exists = 0,
  'ALTER TABLE research_project ADD COLUMN application_attachments LONGTEXT NULL COMMENT ''申报附件JSON'' AFTER expected_deliverables',
  'SELECT 1'
);

PREPARE rf_attachment_stmt FROM @rf_attachment_sql;
EXECUTE rf_attachment_stmt;
DEALLOCATE PREPARE rf_attachment_stmt;
