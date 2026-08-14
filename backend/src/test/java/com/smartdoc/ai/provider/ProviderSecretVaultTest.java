package com.smartdoc.ai.provider;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProviderSecretVaultTest {
    @Test void usesMemoryOnlyWithoutMasterKeyAndClearRemovesTheKey() {
        AiProviderMapper mapper = mock(AiProviderMapper.class);
        when(mapper.clearEncryptedKeyOwned(eq(7L), eq(41L), any())).thenReturn(1);
        AiProviderConfig row = AiProviderConfig.textProvider(41, "one", "CUSTOM", "https://example.cn/v1", "m"); row.setId(7L);
        ProviderSecretVault vault = new ProviderSecretVault(mapper, Optional.empty());
        vault.save(41, 7, "sk-memory", row);
        assertEquals("sk-memory", vault.load(41, 7, row).orElseThrow());
        verify(mapper).clearEncryptedKeyOwned(eq(7L), eq(41L), any());
        vault.clear(41, 7);
        assertTrue(vault.load(41, 7, row).isEmpty());
    }
    @Test void configuredCipherPersistsOnlyCiphertextAndFailsClosedOnZeroRows() {
        AiProviderMapper mapper = mock(AiProviderMapper.class);
        AiProviderConfig row = AiProviderConfig.textProvider(41, "one", "CUSTOM", "https://example.cn/v1", "m"); row.setId(7L);
        when(mapper.updateEncryptedKeyOwned(eq(7L), eq(41L), anyString(), any())).thenReturn(1);
        ProviderSecretVault vault = new ProviderSecretVault(mapper, Optional.of(AesGcmSecretCipher.fromBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")));
        vault.save(41, 7, "sk-persist", row);
        verify(mapper).updateEncryptedKeyOwned(eq(7L), eq(41L), argThat(v -> !"sk-persist".equals(v)), any());
        when(mapper.clearEncryptedKeyOwned(eq(7L), eq(41L), any())).thenReturn(0);
        assertThrows(com.smartdoc.ai.SecretPersistenceException.class, () -> vault.clear(41, 7));
    }
    @Test void memoryOnlySaveClearsOldCiphertextBeforeKeepingVolatileKey() {
        AiProviderMapper mapper = mock(AiProviderMapper.class); when(mapper.clearEncryptedKeyOwned(eq(7L),eq(41L),any())).thenReturn(1);
        AiProviderConfig row = AiProviderConfig.textProvider(41, "one", "CUSTOM", "https://example.cn/v1", "m"); row.setId(7L); row.setEncryptedApiKey("old-ciphertext");
        ProviderSecretVault vault = new ProviderSecretVault(mapper, Optional.empty()); vault.save(41,7,"memory-key",row);
        verify(mapper).clearEncryptedKeyOwned(eq(7L),eq(41L),any()); assertEquals("memory-key",vault.load(41,7,row).orElseThrow());
    }
}
