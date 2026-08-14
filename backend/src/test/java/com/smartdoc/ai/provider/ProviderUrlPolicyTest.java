package com.smartdoc.ai.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderUrlPolicyTest {
    @Test void rejectsRemoteHttpAndLocalOrMetadataTargetsByDefault() {
        ProviderUrlPolicy policy = new ProviderUrlPolicy(false, false);
        assertDoesNotThrow(() -> policy.validateAndNormalize("https://example.cn/v1/"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateAndNormalize("http://example.cn/v1"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateAndNormalize("https://user@example.cn/v1"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateAndNormalize("https://example.cn/v1?q=x"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateAndNormalize("http://127.0.0.1:8080/v1"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateAndNormalize("http://169.254.169.254/latest"));
    }

    @Test void permitsExplicitlyEnabledLoopbackAndNormalizesTrailingSlash() {
        ProviderUrlPolicy policy = new ProviderUrlPolicy(true, false);
        assertEquals("http://127.0.0.1:8080/v1", policy.validateAndNormalize("http://127.0.0.1:8080/v1/"));
    }
}
