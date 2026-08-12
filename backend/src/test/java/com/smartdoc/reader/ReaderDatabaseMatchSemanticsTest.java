package com.smartdoc.reader;

import com.smartdoc.document.DocumentAccessPolicy;
import com.smartdoc.document.DocumentChunkRecord;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.reader.mapper.ReadingProgressMapper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReaderDatabaseMatchSemanticsTest {
    @Test
    void readerMapperUsesFixedMysqlCompatibleDatabaseMatchMetadata() throws Exception {
        Select annotation = DocumentChunkMapper.class
                .getMethod("selectOwnedMatching", long.class, long.class, String.class, String.class, int.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", annotation.value()).toLowerCase();

        assertTrue(sql.contains(" like lower(#{pattern}) escape '!'"), sql);
        assertTrue(sql.contains("locate(lower(#{needle}),lower(c.content))"), sql);
        assertTrue(sql.contains("substring(c.content,1,locate("), sql);
        assertTrue(sql.contains("limit #{limit}"), sql);
        assertFalse(sql.contains("${"), sql);
    }

    @Test
    void keepsEveryDatabaseSelectedCandidateWhenJavaCaseMatchingWouldDisagree() {
        ReadingProgressMapper progress = mock(ReadingProgressMapper.class);
        DocumentMapper documents = mock(DocumentMapper.class);
        DocumentChunkMapper chunks = mock(DocumentChunkMapper.class);
        DocumentRecord owned = new DocumentRecord();
        owned.setId(20L);
        owned.setUserId(10L);
        when(documents.selectOwned(20L, 10L)).thenReturn(owned);

        DocumentChunkRecord selectedByDatabase = new DocumentChunkRecord();
        selectedByDatabase.setPageNumber(3);
        selectedByDatabase.setChunkIndex(4);
        selectedByDatabase.setContent("prefix café suffix");
        selectedByDatabase.setMatchPosition(8);
        selectedByDatabase.setMatchPrefix("prefix ");
        when(chunks.selectOwnedMatching(eq(10L), eq(20L), eq("cafe"), eq("%cafe%"), eq(1)))
                .thenReturn(List.of(selectedByDatabase));

        List<SearchHit> hits = new ReaderService(progress, documents, chunks, new DocumentAccessPolicy())
                .search(10L, 20L, "cafe", 1);

        assertEquals(1, hits.size(), "an SQL-selected row must not be vetoed by Java collation rules");
    }
}
