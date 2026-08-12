package com.smartdoc.document;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentUploadValidatorTest {
    private final DocumentUploadValidator validator = new DocumentUploadValidator(20 * 1024 * 1024);

    @Test
    void acceptsPdf() {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "GUIDE.PDF", "application/octet-stream", "%PDF-1.4".getBytes(StandardCharsets.US_ASCII))));
    }

    @Test
    void rejectsEmptyNonPdfAndOversizedFiles() {
        assertThrows(InvalidDocumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])));
        assertThrows(InvalidDocumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "program.exe", "application/octet-stream", new byte[]{1})));
        DocumentUploadValidator tiny = new DocumentUploadValidator(1);
        assertThrows(InvalidDocumentException.class, () -> tiny.validate(
                new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[]{1, 2})));
    }

    @Test
    void acceptsExactBoundariesAndUppercaseSupportedExtensions() {
        byte[] textBoundary = new byte[5 * 1024 * 1024];
        java.util.Arrays.fill(textBoundary, (byte) 'a');
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "README.MD", "text/plain", textBoundary)));

        byte[] pdfBoundary = new byte[20 * 1024 * 1024];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, pdfBoundary, 0, 5);
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "BOOK.PDF", "application/pdf", pdfBoundary)));
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "Demo.JAVA", "text/x-java-source", "class Demo {}".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsBytesAboveEachBoundaryAndForgedPdf() {
        assertThrows(InvalidDocumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "too-large.txt", "text/plain", new byte[5 * 1024 * 1024 + 1])));
        assertThrows(InvalidDocumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "forged.pdf", "application/pdf", "not a PDF".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void configuredPdfLimitCannotExceedTwentyMiB() {
        byte[] oversized = new byte[20 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, oversized, 0, 5);

        assertThrows(InvalidDocumentException.class, () -> new DocumentUploadValidator(30L * 1024 * 1024)
                .validate(new MockMultipartFile("file", "too-large.pdf", "application/pdf", oversized)));
    }

    @Test
    void rejectsMalformedUtf8AndNulDuringUploadValidation() {
        assertThrows(InvalidDocumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "broken.txt", "text/plain", new byte[]{(byte) 0xC3, (byte) 0x28})));
        assertThrows(InvalidDocumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "binary.sql", "text/plain", new byte[]{'S', 0, 'Q', 'L'})));
    }
}
