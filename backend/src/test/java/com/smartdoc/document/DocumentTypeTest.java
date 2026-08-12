package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTypeTest {

    @Test
    void detectsSupportedDocumentTypes() {
        assertEquals(DocumentType.PDF, DocumentType.fromFilename("jvm.pdf"));
        assertEquals(DocumentType.MARKDOWN, DocumentType.fromFilename("notes.md"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("Demo.java"));
        assertThrows(InvalidDocumentException.class, () -> DocumentType.fromFilename("archive.zip"));
    }
}
