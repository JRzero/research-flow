-- ResearchFlow V2 replaces the V1 application_attachments column with research_document.
-- Kept as an empty compatibility marker so existing references to this migration do not fail.
SELECT 'ResearchFlow V2 document model active' AS researchflow_v2;
