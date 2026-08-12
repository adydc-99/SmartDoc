package com.smartdoc.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiSettingsServiceTest {
    @Test
    void preservesAnExistingKeyWhenUpdateOmitsOrEmptiesItAndClearRemovesIt() {
        RecordingSecretStore store = new RecordingSecretStore(true);
        AiSettingsService service = service(store, "sk-test-sentinel-9999");

        service.update(new AiSettingsUpdate(AiMode.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat", null, false, 512, 25));
        assertEquals("sk-test-sentinel-9999", service.currentKey().orElseThrow());
        service.update(new AiSettingsUpdate(AiMode.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat", "", false, 512, 25));
        assertEquals("sk-****9999", service.view().getMaskedKey());

        service.clearKey();
        assertFalse(service.currentKey().isPresent());
        assertFalse(service.view().isKeyConfigured());
        assertTrue(store.cleared);
    }

    @Test
    void validatesAllUserControlledSettingsAndPersistenceCapability() {
        AiSettingsService service = service(new RecordingSecretStore(false), "");
        assertThrows(IllegalArgumentException.class, () -> update(service, "http://example.com", "model", 512, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://user@example.com", "model", 512, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com/#fragment", "model", 512, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "ftp://example.com", "model", 512, 25, false));
        assertDoesNotThrow(() -> update(service, "http://localhost:8081/v1", "model", 128, 1, false));
        assertDoesNotThrow(() -> update(service, "http://127.0.0.1:8081/v1", "model", 4096, 500, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com", " ", 512, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com", "m".repeat(101), 512, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com", "model", 127, 25, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com", "model", 512, 501, false));
        assertThrows(IllegalArgumentException.class, () -> update(service, "https://example.com", "model", 512, 25, true));
    }

    private static void update(AiSettingsService service, String url, String model, int tokens, int limit, boolean persist) {
        service.update(new AiSettingsUpdate(AiMode.DEEPSEEK, url, model, null, persist, tokens, limit));
    }

    private static AiSettingsService service(SecretStore store, String key) {
        return new AiSettingsService(AiSettings.defaults(), key, store, new DailyAiQuota(Clock.systemUTC()));
    }

    private static final class RecordingSecretStore implements SecretStore {
        private final boolean available;
        private boolean cleared;
        private RecordingSecretStore(boolean available) { this.available = available; }
        public boolean isAvailable() { return available; }
        public Optional<String> load() { return Optional.empty(); }
        public void save(String secret) { }
        public void clear() { cleared = true; }
    }
}
