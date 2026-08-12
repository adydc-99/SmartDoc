package com.smartdoc.document;

import java.util.Locale;

public enum DocumentType {
    PDF,
    MARKDOWN,
    TEXT,
    CODE;

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
}
