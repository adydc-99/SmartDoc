package com.smartdoc.ai.provider;

import com.smartdoc.ai.SecretPersistenceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmSecretCipherTest {
    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test void encryptsWithRandomNonceAndRejectsTamperingAndWrongOwner() {
        SecretCipher cipher = AesGcmSecretCipher.fromBase64(KEY);
        String first = cipher.encrypt("sk-secret", "41:7");
        String second = cipher.encrypt("sk-secret", "41:7");
        assertNotEquals(first, second);
        assertEquals("sk-secret", cipher.decrypt(first, "41:7"));
        assertThrows(SecretPersistenceException.class, () -> cipher.decrypt(first, "42:7"));
        assertThrows(SecretPersistenceException.class,
                () -> cipher.decrypt(first.substring(0, first.length() - 2) + "AA", "41:7"));
    }

    @Test void refusesKeysThatAreNotExactly256Bits() {
        assertThrows(IllegalArgumentException.class, () -> AesGcmSecretCipher.fromBase64("c2hvcnQ="));
    }
}
