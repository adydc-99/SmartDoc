-- SmartDoc Task 3 retry-attempt migration. Do not auto-execute.
-- Pre-verification (run manually):
-- SHOW CREATE TABLE document_record;

ALTER TABLE document_record
  ADD COLUMN IF NOT EXISTS processing_version BIGINT NOT NULL DEFAULT 0;

-- Post-verification (run manually):
-- SHOW CREATE TABLE document_record;
