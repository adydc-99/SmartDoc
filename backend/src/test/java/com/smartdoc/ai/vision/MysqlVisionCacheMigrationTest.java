package com.smartdoc.ai.vision;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MysqlVisionCacheMigrationTest {
    @Test void freshAndExistingMysqlSchemasUseMediumTextForVisionObservation() throws Exception {
        String init=Files.readString(script("init_mysql.sql"));
        String providerMigration=Files.readString(script("alter_ai_provider_config.sql"));
        String cacheMigration=Files.readString(script("alter_ai_vision_cache_observation.sql"));

        assertTrue(init.contains("observation MEDIUMTEXT NOT NULL"));
        assertTrue(providerMigration.contains("observation MEDIUMTEXT NOT NULL"));
        assertTrue(cacheMigration.contains("ALTER TABLE ai_vision_cache"));
        assertTrue(cacheMigration.contains("MODIFY COLUMN observation MEDIUMTEXT NOT NULL"));
    }

    private static Path script(String name) {
        Path fromRoot=Path.of("scripts",name);
        return Files.exists(fromRoot)?fromRoot:Path.of("..","scripts",name);
    }
}
