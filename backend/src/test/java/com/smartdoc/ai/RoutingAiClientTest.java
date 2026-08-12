package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RoutingAiClientTest {
    @Test
    void demoNeverUsesNetworkOrQuotaAndDeepseekUsesCurrentSettings() {
        DailyAiQuota quota = new DailyAiQuota(Clock.systemUTC());
        AiSettingsService settings = new AiSettingsService(AiSettings.defaults(), "", unavailable(), quota);
        AtomicInteger networkCalls = new AtomicInteger();
        DeepSeekClientFactory factory = (snapshot, key) -> new FakeNetworkClient(networkCalls);
        RoutingAiClient routing = new RoutingAiClient(settings, quota, new DemoAiClient(), factory);

        routing.summarize("demo text");
        assertEquals(0, networkCalls.get());
        assertEquals(0, quota.used());

        settings.update(new AiSettingsUpdate(AiMode.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat",
                "sk-test-sentinel-5678", false, 321, 2));
        routing.answer("question", List.of(new TextChunk(0, 1, "reference")));
        assertEquals(1, networkCalls.get());
        assertEquals(1, quota.used());
    }

    @Test
    void validationFailuresDoNotConsumeButNetworkFailuresDoAndErrorsNeverLeakKey() {
        DailyAiQuota quota = new DailyAiQuota(Clock.systemUTC());
        AiSettingsService settings = new AiSettingsService(AiSettings.defaults(), "", unavailable(), quota);
        settings.update(new AiSettingsUpdate(AiMode.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat", null, false, 256, 2));
        RoutingAiClient routing = new RoutingAiClient(settings, quota, new DemoAiClient(), (snapshot, key) -> {
            throw new AssertionError("factory must not run without a key");
        });
        assertThrows(IllegalStateException.class, () -> routing.summarize("x"));
        assertEquals(0, quota.used());

        String key = "sk-test-sentinel-8765";
        settings.update(new AiSettingsUpdate(AiMode.DEEPSEEK, "https://api.deepseek.com/v1", "deepseek-chat", key, false, 256, 2));
        RoutingAiClient failing = new RoutingAiClient(settings, quota, new DemoAiClient(), (snapshot, suppliedKey) -> new NetworkAiClient() {
            public AiSummary summarize(String text) { throw new IllegalStateException("safe AI request failure"); }
            public String answer(String question, List<TextChunk> references) { throw new IllegalStateException("safe AI request failure"); }
            public void probe() { throw new IllegalStateException("safe AI request failure"); }
        });
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> failing.summarize("x"));
        assertFalse(error.toString().contains(key));
        assertEquals(1, quota.used());
    }

    private static SecretStore unavailable() {
        return new SecretStore() {
            public boolean isAvailable() { return false; }
            public Optional<String> load() { return Optional.empty(); }
            public void save(String secret) { throw new UnsupportedOperationException(); }
            public void clear() { }
        };
    }

    private static final class FakeNetworkClient implements NetworkAiClient {
        private final AtomicInteger calls;
        private FakeNetworkClient(AtomicInteger calls) { this.calls = calls; }
        public AiSummary summarize(String text) { calls.incrementAndGet(); return new AiSummary("ok", List.of()); }
        public String answer(String question, List<TextChunk> references) { calls.incrementAndGet(); return "ok"; }
        public void probe() { calls.incrementAndGet(); }
    }
}
