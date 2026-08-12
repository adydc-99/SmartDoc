-- SmartDoc study workspace one-time MySQL migration. Do not auto-execute.
-- Pre-verification (run manually):
-- SHOW CREATE TABLE document_record;
-- SHOW TABLES LIKE 'folder';

ALTER TABLE document_record
  ADD COLUMN document_type VARCHAR(20) NOT NULL DEFAULT 'PDF',
  ADD COLUMN mime_type VARCHAR(100) NULL,
  ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN folder_id BIGINT NULL,
  ADD COLUMN last_opened_at DATETIME NULL,
  ADD COLUMN content_text LONGTEXT NULL;

CREATE TABLE folder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_folder_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  color VARCHAR(50) NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_document_tag_document_tag (document_id, tag_id),
  INDEX idx_document_tag_document (document_id),
  INDEX idx_document_tag_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reading_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  page_number INT NULL,
  scroll_ratio DOUBLE NULL,
  zoom DOUBLE NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_reading_progress_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  page_number INT NULL,
  source_text LONGTEXT NULL,
  content_markdown LONGTEXT NOT NULL,
  favorite BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_note_document_page (document_id, page_number),
  INDEX idx_note_favorite_updated (favorite, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE note_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  UNIQUE KEY uk_note_tag_note_tag (note_id, tag_id),
  INDEX idx_note_tag_note (note_id),
  INDEX idx_note_tag_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  cache_key VARCHAR(255) NOT NULL,
  source_page INT NULL,
  source_text LONGTEXT NULL,
  content_markdown LONGTEXT NOT NULL,
  mode VARCHAR(50) NULL,
  model VARCHAR(100) NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_ai_result_document_cache_created (document_id, cache_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Post-verification (run manually):
-- SHOW CREATE TABLE document_record;
-- SHOW TABLES LIKE 'folder';
-- SHOW INDEX FROM document_tag;
-- SHOW INDEX FROM note_tag;
