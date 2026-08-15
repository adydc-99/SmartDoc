-- One-time manual migration for existing SmartDoc MySQL databases.
-- Do not run this script automatically; back up the database and execute it manually.
ALTER TABLE ai_vision_cache
  MODIFY COLUMN observation MEDIUMTEXT NOT NULL;
