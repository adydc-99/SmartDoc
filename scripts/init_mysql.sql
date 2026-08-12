CREATE DATABASE IF NOT EXISTS smartdoc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartdoc;

CREATE TABLE IF NOT EXISTS document_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  size_bytes BIGINT NOT NULL,
  page_count INT NULL,
  storage_key VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  summary TEXT NULL,
  keywords VARCHAR(1000) NULL,
  error_message VARCHAR(500) NULL,
  processing_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_document_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  page_number INT NOT NULL,
  content TEXT NOT NULL,
  INDEX idx_chunk_document (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS question_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  question VARCHAR(500) NOT NULL,
  answer TEXT NOT NULL,
  references_json TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_question_document_user (document_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
