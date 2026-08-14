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
  document_type VARCHAR(20) NOT NULL DEFAULT 'PDF',
  mime_type VARCHAR(100) NULL,
  favorite BOOLEAN NOT NULL DEFAULT FALSE,
  folder_id BIGINT NULL,
  last_opened_at DATETIME NULL,
  content_text LONGTEXT NULL,
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

CREATE TABLE IF NOT EXISTS folder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, parent_id BIGINT NULL, name VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  INDEX idx_folder_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) NOT NULL, color VARCHAR(50) NULL, created_at DATETIME NOT NULL,
  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS document_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_document_tag_document_tag (document_id,tag_id), INDEX idx_document_tag_document(document_id), INDEX idx_document_tag_tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS reading_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, page_number INT NULL, scroll_ratio DOUBLE NULL, zoom DOUBLE NULL, updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_reading_progress_document(document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, page_number INT NULL, source_text LONGTEXT NULL,
  content_markdown LONGTEXT NOT NULL, favorite BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  INDEX idx_note_document_page(document_id,page_number), INDEX idx_note_favorite_updated(favorite,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS note_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, note_id BIGINT NOT NULL, tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_note_tag_note_tag(note_id,tag_id), INDEX idx_note_tag_note(note_id), INDEX idx_note_tag_tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, action VARCHAR(50) NOT NULL, cache_key VARCHAR(255) NOT NULL,
  source_page INT NULL, source_text LONGTEXT NULL, content_markdown LONGTEXT NOT NULL, mode VARCHAR(50) NULL, model VARCHAR(100) NULL, created_at DATETIME NOT NULL,
  INDEX idx_ai_result_document_cache_created(document_id,cache_key,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_provider_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, display_name VARCHAR(80) NOT NULL,
  preset_code VARCHAR(40) NOT NULL, protocol VARCHAR(40) NOT NULL, base_url VARCHAR(500) NOT NULL,
  model VARCHAR(120) NOT NULL, supports_text BOOLEAN NOT NULL DEFAULT TRUE, supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, encrypted_api_key TEXT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_ai_provider_user_name (user_id, display_name), INDEX idx_ai_provider_user_enabled(user_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_routing_config (
  user_id BIGINT PRIMARY KEY, default_text_provider_id BIGINT NULL, default_vision_provider_id BIGINT NULL,
  daily_limit INT NOT NULL DEFAULT 50, max_output_tokens INT NOT NULL DEFAULT 1024, updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_vision_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, document_id BIGINT NOT NULL,
  content_sha256 CHAR(64) NOT NULL, provider_id BIGINT NOT NULL, model VARCHAR(120) NOT NULL,
  prompt_version VARCHAR(40) NOT NULL, observation TEXT NOT NULL, created_at DATETIME NOT NULL, expires_at DATETIME NOT NULL,
  UNIQUE KEY uk_ai_vision_cache(user_id,content_sha256,provider_id,model,prompt_version),
  INDEX idx_ai_vision_cache_owner_expiry(user_id,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
