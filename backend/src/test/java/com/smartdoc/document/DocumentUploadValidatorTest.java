package com.smartdoc.document;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocumentUploadValidatorTest {
    private final DocumentUploadValidator validator = new DocumentUploadValidator(20 * 1024 * 1024);

    @Test
    void acceptsPdf() {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "guide.pdf", "application/pdf", new byte[]{1})));
    }

    @Test
    void rejectsEmptyNonPdfAndOversizedFiles() {
        assertThrows(InvalidDocumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])));
        assertThrows(InvalidDocumentException.class, () -> validator.validate(
                new MockMultipartFile("file", "note.txt", "text/plain", new byte[]{1})));
        DocumentUploadValidator tiny = new DocumentUploadValidator(1);
        assertThrows(InvalidDocumentException.class, () -> tiny.validate(
                new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[]{1, 2})));
    }
}
