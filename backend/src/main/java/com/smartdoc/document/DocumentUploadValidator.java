package com.smartdoc.document;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;

public class DocumentUploadValidator {
    private static final long TEXT_MAX_BYTES = 5L * 1024 * 1024;
    private static final long PDF_MAX_BYTES = 20L * 1024 * 1024;
    private static final byte[] PDF_HEADER = new byte[]{'%', 'P', 'D', 'F', '-'};
    private final long pdfMaxBytes;
    private final TextDocumentExtractor textExtractor;
    public DocumentUploadValidator(long pdfMaxBytes) { this(pdfMaxBytes, new TextDocumentExtractor()); }
    DocumentUploadValidator(long pdfMaxBytes, TextDocumentExtractor textExtractor) {
        this.pdfMaxBytes = Math.min(pdfMaxBytes, PDF_MAX_BYTES);
        this.textExtractor = textExtractor;
    }
    public void validate(MultipartFile file) {
        if (file.isEmpty()) throw new InvalidDocumentException("文件不能为空");
        DocumentType type = DocumentType.fromFilename(file.getOriginalFilename());
        long limit = type == DocumentType.PDF ? pdfMaxBytes : TEXT_MAX_BYTES;
        if (file.getSize() > limit) {
            throw new InvalidDocumentException(type == DocumentType.PDF ? "PDF 不能超过 20 MB" : "文本文件不能超过 5 MB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (type == DocumentType.PDF) {
                if (bytes.length < PDF_HEADER.length || !Arrays.equals(PDF_HEADER, Arrays.copyOf(bytes, PDF_HEADER.length))) {
                    throw new InvalidDocumentException("PDF 文件签名无效");
                }
            } else {
                textExtractor.read(bytes);
            }
        } catch (IOException e) {
            throw new InvalidDocumentException("无法读取上传文件");
        }
    }
}
