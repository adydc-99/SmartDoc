package com.smartdoc.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {
    @TempDir Path root;

    @Test
    void keyContainsOnlyGeneratedNameAndWhitelistedLowercaseExtension() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(root);

        String key = storage.save("../../secrets/Demo.JAVA", new ByteArrayInputStream("class Demo {}".getBytes(StandardCharsets.UTF_8)));

        assertTrue(key.matches("[0-9a-f-]+\\.java"), key);
        assertFalse(key.contains("Demo"));
        assertFalse(key.contains(".."));
    }
}
