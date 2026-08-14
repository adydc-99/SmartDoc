package com.smartdoc.ai.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:provider-owner;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class AiProviderOwnershipIntegrationTest {
    @Autowired AiProviderMapper providers;

    @Test void providerLookupAlwaysRequiresItsOwner() {
        AiProviderConfig row = AiProviderConfig.textProvider(41L, "DeepSeek", "CUSTOM", "https://api.deepseek.com/v1", "deepseek-chat");
        assertEquals(1, providers.insert(row));
        assertNotNull(providers.selectOwned(row.getId(), 41L));
        assertNull(providers.selectOwned(row.getId(), 42L));
        assertTrue(providers.listOwned(42L).isEmpty());
    }
}
