package com.smartdoc.search;

import com.smartdoc.document.DocumentChunkRecord;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.DocumentTagRecord;
import com.smartdoc.library.TagRecord;
import com.smartdoc.library.mapper.DocumentTagMapper;
import com.smartdoc.library.mapper.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:unified-search;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class SearchServiceIntegrationTest {
    @Autowired SearchService service;
    @Autowired DocumentMapper documents;
    @Autowired DocumentChunkMapper chunks;
    @Autowired TagMapper tags;
    @Autowired DocumentTagMapper documentTags;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clear() {
        jdbc.update("DELETE FROM note_tag"); jdbc.update("DELETE FROM note"); jdbc.update("DELETE FROM document_tag");
        jdbc.update("DELETE FROM tag"); jdbc.update("DELETE FROM document_chunk"); jdbc.update("DELETE FROM reading_progress"); jdbc.update("DELETE FROM document_record");
    }

    @Test
    void defaultsToAllTypesRanksNameBeforeBodyAndDeduplicatesDocuments() {
        LocalDateTime now = LocalDateTime.now();
        DocumentRecord title = document(1L, "Needle Handbook", "unrelated", now.minusDays(3));
        DocumentRecord body = document(1L, "Recent guide", "needle appears in body", now);
        chunk(title.getId(), 0, 1, "another needle in a chunk");
        chunk(title.getId(), 1, 2, "needle repeated in another chunk");
        TagRecord tag = tag("Needle tag"); link(title.getId(), tag.getId());
        long noteId = note(body.getId(), 2, "needle source", "needle note", now.plusMinutes(1));

        List<UnifiedSearchHit> hits = service.search(1L, "needle", null, 30);

        assertEquals("DOCUMENT", hits.get(0).getType());
        assertEquals(title.getId(), hits.get(0).getDocumentId());
        assertEquals(1, hits.stream().filter(h -> "DOCUMENT".equals(h.getType()) && title.getId().equals(h.getDocumentId())).count());
        assertTrue(hits.stream().anyMatch(h -> "NOTE".equals(h.getType()) && Long.valueOf(noteId).equals(h.getNoteId())));
        assertTrue(hits.stream().anyMatch(h -> "TAG".equals(h.getType()) && title.getId().equals(h.getDocumentId())));
    }

    @Test
    void typeWhitelistIsCaseInsensitiveAndRejectsUnknownOrInvalidLimits() {
        DocumentRecord document = document(1L, "Needle", null, LocalDateTime.now());
        assertEquals(List.of("DOCUMENT"), service.search(1L, "needle", " document ", 30).stream().map(UnifiedSearchHit::getType).distinct().collect(java.util.stream.Collectors.toList()));
        assertThrows(InvalidDocumentException.class, () -> service.search(1L, "needle", "document,script", 30));
        assertThrows(InvalidDocumentException.class, () -> service.search(1L, "x", null, 30));
        assertThrows(InvalidDocumentException.class, () -> service.search(1L, "needle", null, 31));
    }

    @Test
    void hidesOtherOwnersDocumentsNotesAndTagsAndTreatsInjectionShapeLiterally() {
        DocumentRecord owned = document(2L, "literal ' OR 1=1 -- token", null, LocalDateTime.now());
        DocumentRecord privateDoc = document(3L, "private needle", "needle", LocalDateTime.now().plusDays(1));
        TagRecord privateTag = tag("needle secret"); link(privateDoc.getId(), privateTag.getId());
        note(privateDoc.getId(), 1, "needle", "needle private", LocalDateTime.now());

        List<UnifiedSearchHit> literal = service.search(2L, "' OR 1=1 --", null, 30);
        List<UnifiedSearchHit> secret = service.search(2L, "needle", null, 30);

        assertEquals(1, literal.size());
        assertEquals(owned.getId(), literal.get(0).getDocumentId());
        assertTrue(secret.isEmpty());
        assertTrue(literal.get(0).getSnippet().indexOf("<mark>") < 0);
    }

    @Test
    void sameRankUsesUpdatedAtDescendingAndLimitAppliesAfterMerge() {
        DocumentRecord older = document(1L, "Needle old", null, LocalDateTime.now().minusDays(1));
        DocumentRecord newer = document(1L, "Needle new", null, LocalDateTime.now());

        List<UnifiedSearchHit> hits = service.search(1L, "needle", "document", 1);

        assertEquals(1, hits.size()); assertEquals(newer.getId(), hits.get(0).getDocumentId());
    }

    @Test
    void findsTagsAttachedOnlyToNotesOfOwnedDocuments() {
        DocumentRecord owned = document(6L, "Owned", null, LocalDateTime.now());
        DocumentRecord privateDocument = document(7L, "Private", null, LocalDateTime.now());
        long ownedNote = note(owned.getId(), 1, null, "plain", LocalDateTime.now());
        long privateNote = note(privateDocument.getId(), 1, null, "plain", LocalDateTime.now());
        TagRecord ownedTag = tag("Needle note tag");
        TagRecord privateTag = tag("Needle private tag");
        jdbc.update("INSERT INTO note_tag(note_id,tag_id) VALUES (?,?)", ownedNote, ownedTag.getId());
        jdbc.update("INSERT INTO note_tag(note_id,tag_id) VALUES (?,?)", privateNote, privateTag.getId());

        List<UnifiedSearchHit> hits = service.search(6L, "needle", "tag", 30);

        assertEquals(1, hits.size());
        assertEquals("TAG", hits.get(0).getType());
        assertEquals(owned.getId(), hits.get(0).getDocumentId());
        assertEquals(ownedNote, hits.get(0).getNoteId());
    }

    private DocumentRecord document(long owner,String name,String content,LocalDateTime updated){DocumentRecord d=new DocumentRecord();d.setUserId(owner);d.setName(name);d.setContentText(content);d.setDocumentType("PDF");d.setMimeType("application/pdf");d.setSizeBytes(1L);d.setStorageKey("test/"+name);d.setStatus("READY");d.setFavorite(false);d.setCreatedAt(updated.minusDays(1));d.setUpdatedAt(updated);documents.insert(d);return d;}
    private void chunk(long documentId,int index,int page,String content){DocumentChunkRecord c=new DocumentChunkRecord();c.setDocumentId(documentId);c.setChunkIndex(index);c.setPageNumber(page);c.setContent(content);chunks.insert(c);}
    private TagRecord tag(String name){TagRecord t=new TagRecord();t.setName(name);t.setColor("#112233");t.setCreatedAt(LocalDateTime.now());tags.insert(t);return t;}
    private void link(long documentId,long tagId){DocumentTagRecord l=new DocumentTagRecord();l.setDocumentId(documentId);l.setTagId(tagId);documentTags.insert(l);}
    private long note(long documentId,int page,String source,String content,LocalDateTime updated){KeyHolder key=new GeneratedKeyHolder();jdbc.update(connection->{java.sql.PreparedStatement statement=connection.prepareStatement("INSERT INTO note(document_id,page_number,source_text,content_markdown,favorite,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);statement.setLong(1,documentId);statement.setInt(2,page);statement.setString(3,source);statement.setString(4,content);statement.setBoolean(5,false);statement.setTimestamp(6,java.sql.Timestamp.valueOf(updated.minusHours(1)));statement.setTimestamp(7,java.sql.Timestamp.valueOf(updated));return statement;},key);return key.getKey().longValue();}
}
