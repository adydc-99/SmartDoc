package com.smartdoc.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiSecretMaskerTest {
    @Test
    void masksLongKeysWithoutLeakingTheSecret() {
        String key = "sk-test-sentinel-123456";

        String masked = AiSecretMasker.mask(key);

        assertEquals("sk-****3456", masked);
        assertFalse(masked.contains("test-sentinel"));
        assertFalse(masked.contains(key));
    }

    @Test
    void shortKeysExposeAtMostTheFinalFourCharacters() {
        assertEquals("****abc", AiSecretMasker.mask("abc"));
        assertEquals("****1234", AiSecretMasker.mask("1234"));
        assertEquals("", AiSecretMasker.mask(""));
    }
}
