package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextDocumentExtractorTest {
    private final TextDocumentExtractor extractor = new TextDocumentExtractor();

    @Test
    void readsValidUtf8() {
        byte[] content = "public class 示例 {}".getBytes(StandardCharsets.UTF_8);

        assertEquals("public class 示例 {}", extractor.read(content));
    }

    @Test
    void rejectsNulBytesAsBinaryContent() {
        byte[] content = new byte[]{'a', 0, 'b'};

        assertThrows(InvalidDocumentException.class, () -> extractor.read(content));
    }

    @Test
    void rejectsMalformedUtf8() {
        byte[] content = new byte[]{(byte) 0xC3, (byte) 0x28};

        assertThrows(InvalidDocumentException.class, () -> extractor.read(content));
    }
}
