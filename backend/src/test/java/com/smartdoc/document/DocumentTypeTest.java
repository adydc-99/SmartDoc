package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTypeTest {

    @Test
    void detectsSupportedDocumentTypes() {
        assertEquals(DocumentType.PDF, DocumentType.fromFilename("jvm.pdf"));
        assertEquals(DocumentType.MARKDOWN, DocumentType.fromFilename("notes.md"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("Demo.java"));
        assertThrows(InvalidDocumentException.class, () -> DocumentType.fromFilename("archive.zip"));
    }

    @Test
    void classifiesEveryAllowedExtensionCaseInsensitively() {
        assertEquals(DocumentType.PDF, DocumentType.fromFilename("document.PDF"));
        assertEquals(DocumentType.MARKDOWN, DocumentType.fromFilename("document.MD"));
        assertEquals(DocumentType.MARKDOWN, DocumentType.fromFilename("document.MARKDOWN"));
        assertEquals(DocumentType.TEXT, DocumentType.fromFilename("document.TXT"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.JAVA"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.XML"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.YML"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.YAML"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.SQL"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.JS"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.TS"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.JSON"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.PROPERTIES"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.SH"));
        assertEquals(DocumentType.CODE, DocumentType.fromFilename("document.PS1"));
    }

    @Test
    void reportsExactMessagesForNullAndUnsupportedFilenames() {
        InvalidDocumentException nullFilename = assertThrows(InvalidDocumentException.class,
                () -> DocumentType.fromFilename(null));
        assertEquals("文件名不能为空", nullFilename.getMessage());

        InvalidDocumentException unsupportedFilename = assertThrows(InvalidDocumentException.class,
                () -> DocumentType.fromFilename("archive.zip"));
        assertEquals("暂不支持该文件类型", unsupportedFilename.getMessage());
    }

    @Test
    void treatsOnlyPdfAsNonText() {
        assertFalse(DocumentType.PDF.isText());
        assertTrue(DocumentType.MARKDOWN.isText());
        assertTrue(DocumentType.TEXT.isText());
        assertTrue(DocumentType.CODE.isText());
    }
}
