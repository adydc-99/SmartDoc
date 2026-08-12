package com.smartdoc.storage;

import com.smartdoc.document.DocumentType;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class LocalFileStorage implements FileStorage {
    private final Path root;
    public LocalFileStorage(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public String save(String originalName, InputStream input) throws Exception {
        Files.createDirectories(root);
        String key = UUID.randomUUID() + "." + DocumentType.extensionFromFilename(originalName);
        Files.copy(input, safe(key), StandardCopyOption.REPLACE_EXISTING);
        return key;
    }
    public InputStream open(String key) throws Exception { return Files.newInputStream(safe(key)); }
    public void delete(String key) throws Exception { Files.deleteIfExists(safe(key)); }
    private Path safe(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("非法存储路径");
        return target;
    }
}
