package com.smartdoc.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DpapiSecretStoreTest {
    @TempDir Path temp;

    @Test
    void sendsPlaintextOnlyOnStdinAndStoresOnlyProtectedOutput() throws Exception {
        String key = "sk-test-sentinel-4242";
        CapturingRunner runner = new CapturingRunner();
        runner.outputs.add("available");
        runner.outputs.add(Base64.getEncoder().encodeToString("protected-bytes".getBytes()));
        Path file = temp.resolve("ai-key.bin");
        DpapiSecretStore store = new DpapiSecretStore(file, runner, true);

        assertTrue(store.isAvailable());
        store.save(key);

        assertEquals(key, runner.stdins.get(1));
        assertTrue(runner.arguments.stream().flatMap(Collection::stream).noneMatch(arg -> arg.contains(key)));
        assertEquals("protected-bytes", Files.readString(file));
        assertFalse(Files.readString(file).contains(key));
    }

    @Test
    void capabilityOrProtectionFailureDisablesPersistenceWithoutPlaintextFallback() {
        CapturingRunner runner = new CapturingRunner();
        runner.outputs.add("available");
        runner.failureOnCall = 2;
        Path file = temp.resolve("ai-key.bin");
        DpapiSecretStore store = new DpapiSecretStore(file, runner, true);

        assertThrows(SecretPersistenceException.class, () -> store.save("sk-test-sentinel-0000"));
        assertFalse(store.isAvailable());
        assertFalse(Files.exists(file));
    }

    private static final class CapturingRunner implements DpapiSecretStore.CommandRunner {
        private final List<List<String>> arguments = new ArrayList<>();
        private final List<String> stdins = new ArrayList<>();
        private final Queue<String> outputs = new ArrayDeque<>();
        private int calls;
        private int failureOnCall = -1;
        public String run(List<String> args, String stdin) {
            calls++; arguments.add(new ArrayList<>(args)); stdins.add(stdin);
            if (calls == failureOnCall) throw new SecretPersistenceException("DPAPI unavailable");
            return outputs.remove();
        }
    }
}
