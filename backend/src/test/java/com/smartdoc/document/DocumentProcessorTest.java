package com.smartdoc.document;

import com.smartdoc.ai.AiClient;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentProcessorTest {
    @Test
    void parsesTextPersistsContentAndDoesNotCallAi() throws Exception {
        DocumentMapper documents = mock(DocumentMapper.class);
        DocumentChunkMapper chunks = mock(DocumentChunkMapper.class);
        FileStorage storage = mock(FileStorage.class);
        AiClient ai = mock(AiClient.class);
        DocumentRecord document = document("CODE", "examples/demo.java");
        when(documents.selectById(42L)).thenReturn(document);
        when(storage.open("examples/demo.java")).thenReturn(new ByteArrayInputStream("class 示例 {}".getBytes(StandardCharsets.UTF_8)));
        DocumentProcessor processor = new DocumentProcessor(documents, chunks, storage,
                new PdfTextExtractor(), new TextDocumentExtractor(), new TextChunker(1000, 120), ai);

        processor.process(42L);

        assertEquals("READY", document.getStatus());
        assertEquals(1, document.getPageCount());
        assertEquals("class 示例 {}", document.getContentText());
        verify(chunks).insert(argThat(row -> row.getPageNumber() == 1 && row.getContent().equals("class 示例 {}")));
        verifyNoInteractions(ai);
    }

    @Test
    void invalidStoredUtf8FailsWithoutReplacementCharacters() throws Exception {
        DocumentMapper documents = mock(DocumentMapper.class);
        DocumentChunkMapper chunks = mock(DocumentChunkMapper.class);
        FileStorage storage = mock(FileStorage.class);
        DocumentRecord document = document("TEXT", "broken.txt");
        when(documents.selectById(42L)).thenReturn(document);
        when(storage.open("broken.txt")).thenReturn(new ByteArrayInputStream(new byte[]{(byte) 0xC3, (byte) 0x28}));
        DocumentProcessor processor = new DocumentProcessor(documents, chunks, storage,
                new PdfTextExtractor(), new TextDocumentExtractor(), new TextChunker(1000, 120), mock(AiClient.class));

        processor.process(42L);

        assertEquals("FAILED", document.getStatus());
        verify(chunks, never()).insert(any());
    }

    private DocumentRecord document(String type, String key) {
        DocumentRecord document = new DocumentRecord();
        document.setId(42L);
        document.setDocumentType(type);
        document.setStorageKey(key);
        document.setStatus("PROCESSING");
        return document;
    }
}
