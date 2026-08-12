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
