package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import com.smartdoc.ai.provider.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class RoutingAiClientTest {
    @Test
    void userAwareCompletionUsesOwnedDefaultProviderOrOfflineDemoAndNeverFallsBackForMissingKey() {
        DailyAiQuota quota = new DailyAiQuota(Clock.systemUTC());
        AiSettingsService settings = new AiSettingsService(AiSettings.defaults(), "", unavailable(), quota);
        AiProviderMapper providers = mock(AiProviderMapper.class);
        AiRoutingMapper routes = mock(AiRoutingMapper.class);
        ProviderSecretVault vault = new ProviderSecretVault(providers, Optional.empty());
        ProviderAdapter adapter = new ProviderAdapter() {
            public AiProviderProtocol protocol() { return AiProviderProtocol.OPENAI_CHAT_COMPLETIONS; }
            public ProviderResponse complete(AiProviderConfig c, String key, TextCompletionRequest request) { return new ProviderResponse("provider-answer"); }
            public ProviderResponse vision(AiProviderConfig c, String key, VisionCompletionRequest request) { throw new UnsupportedOperationException(); }
            public ProviderResponse probe(AiProviderConfig c, String key) { return new ProviderResponse("OK"); }
        };
        ModelRouter router = new ModelRouter(providers, routes, vault, new ProviderAdapterRegistry(List.of(adapter)));
        RoutingAiClient client = new RoutingAiClient(settings, quota, new DemoAiClient(), (snapshot, key) -> { throw new AssertionError(); }, router);

        assertNotEquals("provider-answer", client.complete(41L, "system", "question"));
        assertEquals(0, quota.used(41L));

        AiProviderConfig configured = AiProviderConfig.textProvider(41L, "Mine", "CUSTOM", "https://example.cn/v1", "model-a");
        configured.setId(7L);
        AiRoutingConfig routing = new AiRoutingConfig(); routing.setDefaultTextProviderId(7L); routing.setDailyLimit(2); routing.setMaxOutputTokens(128);
        when(routes.selectOwned(41L)).thenReturn(routing);
        when(providers.selectOwnedEnabled(7L, 41L)).thenReturn(configured);
        vault.save(41L, 7L, "sk-key", configured);
        assertEquals("provider-answer", client.complete(41L, "system", "question"));
        assertEquals(1, quota.used(41L));
        assertEquals("OPENAI_CHAT_COMPLETIONS|7|model-a", client.providerIdentity(41L));

        AiProviderConfig keyless = AiProviderConfig.textProvider(41L, "Keyless", "CUSTOM", "https://example.cn/v1", "model-b"); keyless.setId(8L);
        routing.setDefaultTextProviderId(8L); when(providers.selectOwnedEnabled(8L, 41L)).thenReturn(keyless);
        assertThrows(ProviderKeyMissingException.class, () -> client.complete(41L, "system", "question"));
    }
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
