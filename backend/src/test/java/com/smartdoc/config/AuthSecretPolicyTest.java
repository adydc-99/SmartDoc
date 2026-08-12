package com.smartdoc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthSecretPolicyTest {
    @Test
    void productionAndComposeProfilesFailFastForMissingOrShortSecrets() {
        assertThrows(IllegalStateException.class, () -> AuthSecretPolicy.requireValid("", new String[]{"prod"}));
        assertThrows(IllegalStateException.class, () -> AuthSecretPolicy.requireValid("short", new String[]{"mysql"}));
        assertEquals("a-clearly-fake-test-secret-with-32-chars", AuthSecretPolicy.requireValid(
                "a-clearly-fake-test-secret-with-32-chars", new String[]{"prod"}));
    }

    @Test
    void localProfileAllowsOnlyAnExplicitAtLeast32CharacterDevelopmentSecret() {
        assertThrows(IllegalStateException.class, () -> AuthSecretPolicy.requireValid("", new String[]{"local"}));
        assertEquals("dev-only-smartdoc-auth-secret-please-change", AuthSecretPolicy.requireValid(
                "dev-only-smartdoc-auth-secret-please-change", new String[]{"local"}));
    }
}
