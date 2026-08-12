package com.smartdoc.reader;

import com.smartdoc.document.DocumentChunkRecord;
import com.smartdoc.document.DocumentNotFoundException;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:document-search;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DocumentSearchIntegrationTest {
    @Autowired ReaderService service;
    @Autowired DocumentMapper documents;
    @Autowired DocumentChunkMapper chunks;

    @Test
    void searchesOwnedChunksCaseInsensitivelyInPageAndChunkOrder() {
        DocumentRecord document = document(10L, "guide.pdf");
        chunk(document.getId(), 3, 5, "Later NEEDLE result");
        chunk(document.getId(), 2, 4, "Earlier needle result");
        chunk(document.getId(), 1, 6, "needle last");

        List<SearchHit> result = service.search(10L, document.getId(), "  NeEdLe  ", 50);

        assertEquals(3, result.size());
        assertEquals(List.of(4, 5, 6), List.of(result.get(0).getPageNumber(), result.get(1).getPageNumber(), result.get(2).getPageNumber()));
        assertEquals("needle", result.get(0).getSnippet().substring(result.get(0).getMatchStart(), result.get(0).getMatchStart() + result.get(0).getMatchLength()).toLowerCase());
    }

    @Test
    void snippetUsesEightyCodePointsOfContextAndUtf16OffsetsForNonBmpText() {
        DocumentRecord document = document(11L, "unicode.pdf");
        String before = "x".repeat(79) + "😀";
        chunk(document.getId(), 0, 1, "discard" + before + "Needle" + "y".repeat(90));

        SearchHit hit = service.search(11L, document.getId(), "needle", 1).get(0);

        assertEquals(80, hit.getSnippet().codePointCount(0, hit.getMatchStart()));
        assertEquals("Needle", hit.getSnippet().substring(hit.getMatchStart(), hit.getMatchStart() + hit.getMatchLength()));
        assertEquals(81, hit.getMatchStart()); // 79 BMP chars plus a surrogate pair: Java UTF-16 index.
        assertEquals(6, hit.getMatchLength());
        assertEquals(80, hit.getSnippet().codePointCount(hit.getMatchStart() + hit.getMatchLength(), hit.getSnippet().length()));
    }

    @Test
    void validatesQueryAndLimitAndHidesOtherOwnersDocuments() {
        DocumentRecord document = document(12L, "private.pdf");
        chunk(document.getId(), 0, 1, "literal ' OR 1=1 -- token");
        assertThrows(InvalidDocumentException.class, () -> service.search(12L, document.getId(), "x", 10));
        assertThrows(InvalidDocumentException.class, () -> service.search(12L, document.getId(), "x".repeat(101), 10));
        assertThrows(InvalidDocumentException.class, () -> service.search(12L, document.getId(), "token", 0));
        assertThrows(DocumentNotFoundException.class, () -> service.search(13L, document.getId(), "token", 10));
        assertEquals(1, service.search(12L, document.getId(), "' OR 1=1 --", 10).size());
    }

    @Test
    void treatsSqlWildcardsAndBackslashLiterallyAndBoundsTheDatabaseResult() {
        DocumentRecord document = document(14L, "literal.pdf");
        for (int index = 0; index < 12; index++) chunk(document.getId(), index, 1, "literal %_\\ marker " + index);
        chunk(document.getId(), 99, 1, "literal XX marker");

        List<SearchHit> hits = service.search(14L, document.getId(), "%_\\", 3);

        assertEquals(3, hits.size());
        assertEquals(List.of(0, 1, 2), List.of(hits.get(0).getChunkIndex(), hits.get(1).getChunkIndex(), hits.get(2).getChunkIndex()));
        assertEquals(3, chunks.selectOwnedMatching(14L, document.getId(), "%_\\", "%!%!_\\%", 3).size());
    }

    private DocumentRecord document(long owner, String name) {
        LocalDateTime now=LocalDateTime.now(); DocumentRecord d=new DocumentRecord();d.setUserId(owner);d.setName(name);d.setDocumentType("PDF");d.setMimeType("application/pdf");d.setSizeBytes(1L);d.setPageCount(10);d.setStorageKey("test/"+name);d.setStatus("READY");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d;
    }
    private void chunk(long documentId,int index,int page,String content){DocumentChunkRecord c=new DocumentChunkRecord();c.setDocumentId(documentId);c.setChunkIndex(index);c.setPageNumber(page);c.setContent(content);chunks.insert(c);}
}
