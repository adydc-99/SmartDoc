package com.smartdoc.reader;

import com.smartdoc.document.DocumentAccessPolicy;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.reader.mapper.ReadingProgressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReaderServiceRaceTest {
    @Test
    void retriesOwnedUpdateWhenConcurrentInsertWinsUniqueKeyRace() {
        ReadingProgressMapper progress = mock(ReadingProgressMapper.class);
        DocumentMapper documents = mock(DocumentMapper.class);
        DocumentRecord document = new DocumentRecord(); document.setId(9L); document.setUserId(1L);
        document.setDocumentType("PDF"); document.setPageCount(5);
        when(documents.selectOwned(9L,1L)).thenReturn(document);
        when(documents.touchLastOpened(eq(9L),eq(1L),any())).thenReturn(1);
        when(progress.updateOwned(eq(1L), eq(9L), eq(2), eq(.2), eq(1.1), any())).thenReturn(0, 1);
        when(progress.insertOwned(eq(1L), eq(9L), eq(2), eq(.2), eq(1.1), any()))
                .thenThrow(new DuplicateKeyException("concurrent insert"));

        ProgressView saved = new ReaderService(progress, documents, mock(DocumentChunkMapper.class), new DocumentAccessPolicy())
                .save(1L, 9L, new ProgressInput(2, .2, 1.1));

        assertEquals(2, saved.getPageNumber());
        verify(progress, times(2)).updateOwned(eq(1L), eq(9L), eq(2), eq(.2), eq(1.1), any());
    }
}
