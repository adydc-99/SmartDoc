-- One-time manual migration: execute once against an existing SmartDoc MySQL database.
CREATE TABLE ai_provider_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, display_name VARCHAR(80) NOT NULL,
  preset_code VARCHAR(40) NOT NULL, protocol VARCHAR(40) NOT NULL, base_url VARCHAR(500) NOT NULL,
  model VARCHAR(120) NOT NULL, supports_text BOOLEAN NOT NULL DEFAULT TRUE, supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, encrypted_api_key TEXT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_ai_provider_user_name (user_id, display_name), INDEX idx_ai_provider_user_enabled(user_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE ai_routing_config (
  user_id BIGINT PRIMARY KEY, default_text_provider_id BIGINT NULL, default_vision_provider_id BIGINT NULL,
  daily_limit INT NOT NULL DEFAULT 50, max_output_tokens INT NOT NULL DEFAULT 1024, updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_vision_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, document_id BIGINT NOT NULL,
  content_sha256 CHAR(64) NOT NULL, provider_id BIGINT NOT NULL, model VARCHAR(120) NOT NULL,
  prompt_version VARCHAR(40) NOT NULL, observation MEDIUMTEXT NOT NULL, created_at DATETIME NOT NULL, expires_at DATETIME NOT NULL,
  UNIQUE KEY uk_ai_vision_cache(user_id,content_sha256,provider_id,model,prompt_version),
  INDEX idx_ai_vision_cache_owner_expiry(user_id,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
