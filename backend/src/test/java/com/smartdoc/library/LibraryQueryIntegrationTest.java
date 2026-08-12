package com.smartdoc.library;

import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.mapper.DocumentTagMapper;
import com.smartdoc.library.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:library;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class LibraryQueryIntegrationTest {
    @Autowired LibraryService service;
    @Autowired DocumentMapper documents;
    @Autowired TagMapper tags;
    @Autowired DocumentTagMapper documentTags;

    @Test
    void combinesOwnerFolderTagFavoriteTypeAndCaseInsensitiveNameFilters() {
        TagRecord tag = new TagRecord(); tag.setName("Exam"); tag.setColor("#336699"); tag.setCreatedAt(LocalDateTime.now()); tags.insert(tag);
        DocumentRecord matching = document(1L, "Operating SYSTEMS Notes", 11L, true, "PDF", LocalDateTime.now().minusDays(1));
        DocumentRecord wrongOwner = document(2L, "Operating Systems Secret", 11L, true, "PDF", LocalDateTime.now());
        DocumentRecord wrongFavorite = document(1L, "Operating Systems Draft", 11L, false, "PDF", LocalDateTime.now());
        documents.insert(matching); documents.insert(wrongOwner); documents.insert(wrongFavorite);
        link(matching.getId(), tag.getId()); link(wrongOwner.getId(), tag.getId()); link(wrongFavorite.getId(), tag.getId());

        List<DocumentListItem> result = service.listDocuments(1L, 11L, tag.getId(), true, "PDF", "name,asc", "systems");

        assertEquals(1, result.size());
        assertEquals(matching.getId(), result.get(0).getId());
        assertEquals("Exam", result.get(0).getTags().get(0).getName());
    }

    private DocumentRecord document(long userId, String name, long folderId, boolean favorite, String type, LocalDateTime updatedAt) {
        DocumentRecord document = new DocumentRecord(); document.setUserId(userId); document.setName(name); document.setFolderId(folderId);
        document.setFavorite(favorite); document.setDocumentType(type); document.setMimeType("application/pdf"); document.setSizeBytes(1L);
        document.setStorageKey("test/" + name); document.setStatus("READY"); document.setCreatedAt(updatedAt.minusDays(1)); document.setUpdatedAt(updatedAt);
        return document;
    }

    private void link(long documentId, long tagId) {
        DocumentTagRecord link = new DocumentTagRecord(); link.setDocumentId(documentId); link.setTagId(tagId); documentTags.insert(link);
    }
}
