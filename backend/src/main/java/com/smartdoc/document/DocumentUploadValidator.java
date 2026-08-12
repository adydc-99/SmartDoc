package com.smartdoc.document;

import org.springframework.web.multipart.MultipartFile;
import java.util.Locale;

public class DocumentUploadValidator {
    private final long maxBytes;
    public DocumentUploadValidator(long maxBytes) { this.maxBytes = maxBytes; }
    public void validate(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (file.isEmpty()) throw new InvalidDocumentException("文件不能为空");
        if (file.getSize() > maxBytes) throw new InvalidDocumentException("PDF 不能超过 20 MB");
        if (!name.endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new InvalidDocumentException("首版仅支持 PDF 文件");
        }
    }
}
