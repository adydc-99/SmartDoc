package com.smartdoc.document;

import java.util.Locale;
import java.util.Set;

public enum DocumentType {
    PDF,
    MARKDOWN,
    TEXT,
    CODE;

    private static final Set<String> EXTENSIONS = Set.of(
            "pdf", "md", "markdown", "txt", "java", "xml", "yml", "yaml", "sql",
            "js", "ts", "json", "properties", "sh", "ps1");

    public static DocumentType fromFilename(String filename) {
        if (filename == null) {
            throw new InvalidDocumentException("文件名不能为空");
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (lowerFilename.endsWith(".pdf")) return PDF;
        if (lowerFilename.endsWith(".md") || lowerFilename.endsWith(".markdown")) return MARKDOWN;
        if (lowerFilename.endsWith(".txt")) return TEXT;
        if (lowerFilename.endsWith(".java") || lowerFilename.endsWith(".xml")
                || lowerFilename.endsWith(".yml") || lowerFilename.endsWith(".yaml")
                || lowerFilename.endsWith(".sql") || lowerFilename.endsWith(".js")
                || lowerFilename.endsWith(".ts") || lowerFilename.endsWith(".json")
                || lowerFilename.endsWith(".properties") || lowerFilename.endsWith(".sh")
                || lowerFilename.endsWith(".ps1")) return CODE;
        throw new InvalidDocumentException("暂不支持该文件类型");
    }

    public boolean isText() {
        return this != PDF;
    }

    public static String extensionFromFilename(String filename) {
        fromFilename(filename);
        String name = filename.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        if (!EXTENSIONS.contains(extension)) throw new InvalidDocumentException("暂不支持该文件类型");
        return extension;
    }
}
