CREATE TABLE IF NOT EXISTS document_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  size_bytes BIGINT NOT NULL,
  page_count INT,
  storage_key VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  summary CLOB,
  keywords VARCHAR(1000),
  error_message VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_document_user_created ON document_record(user_id, created_at);

ALTER TABLE document_record ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'PDF';
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS favorite BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS folder_id BIGINT;
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS last_opened_at TIMESTAMP;
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS content_text CLOB;
ALTER TABLE document_record ADD COLUMN IF NOT EXISTS processing_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS folder (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT,
  name VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_folder_parent ON folder(parent_id);

CREATE TABLE IF NOT EXISTS tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  color VARCHAR(50),
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_tag_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS document_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  CONSTRAINT uk_document_tag_document_tag UNIQUE (document_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_document_tag_document ON document_tag(document_id);
CREATE INDEX IF NOT EXISTS idx_document_tag_tag ON document_tag(tag_id);

CREATE TABLE IF NOT EXISTS reading_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  page_number INT,
  scroll_ratio DOUBLE,
  zoom DOUBLE,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_reading_progress_document UNIQUE (document_id)
);

CREATE TABLE IF NOT EXISTS note (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  page_number INT,
  source_text CLOB,
  content_markdown CLOB NOT NULL,
  favorite BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_note_document_page ON note(document_id, page_number);
CREATE INDEX IF NOT EXISTS idx_note_favorite_updated ON note(favorite, updated_at);

CREATE TABLE IF NOT EXISTS note_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  CONSTRAINT uk_note_tag_note_tag UNIQUE (note_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_note_tag_note ON note_tag(note_id);
CREATE INDEX IF NOT EXISTS idx_note_tag_tag ON note_tag(tag_id);

CREATE TABLE IF NOT EXISTS ai_result (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  cache_key VARCHAR(255) NOT NULL,
  source_page INT,
  source_text CLOB,
  content_markdown CLOB NOT NULL,
  mode VARCHAR(50),
  model VARCHAR(100),
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ai_result_document_cache_created ON ai_result(document_id, cache_key, created_at);

CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  page_number INT NOT NULL,
  content CLOB NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_chunk_document ON document_chunk(document_id, chunk_index);

CREATE TABLE IF NOT EXISTS question_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  question VARCHAR(500) NOT NULL,
  answer CLOB NOT NULL,
  references_json CLOB NOT NULL,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_question_document_user ON question_history(document_id, user_id, created_at);
