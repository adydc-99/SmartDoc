package com.smartdoc.reader;

import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.reader.mapper.ReadingProgressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reader-red;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class ReaderServiceIntegrationTest {
    @Autowired ReaderService service;
    @Autowired DocumentMapper documents;
    @Autowired ReadingProgressMapper progress;

    @Test
    void upsertsProgressForOwnedDocument() {
        DocumentRecord document = document(1L, "book.pdf", "PDF", 10);

        service.save(1L, document.getId(), new ProgressInput(3, 0.45, 1.2));
        service.save(1L, document.getId(), new ProgressInput(4, 0.10, 1.0));

        ProgressView saved = service.get(1L, document.getId());
        assertEquals(4, saved.getPageNumber());
        assertEquals(0.10, saved.getScrollRatio());
        assertEquals(1.0, saved.getZoom());
    }

    @Test
    void rejectsProgressOutsideDocumentAndReaderBounds() {
        DocumentRecord pdf = document(1L, "book.pdf", "PDF", 3);
        DocumentRecord text = document(1L, "notes.txt", "TEXT", 1);

        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(0, 0.5, 1.0)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(4, 0.5, 1.0)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, text.getId(), new ProgressInput(2, 0.5, 1.0)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(1, 1.01, 1.0)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(1, 0.5, 3.01)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(1, Double.NaN, 1.0)));
        assertThrows(InvalidDocumentException.class,
                () -> service.save(1L, pdf.getId(), new ProgressInput(1, 0.5, Double.POSITIVE_INFINITY)));
    }

    @Test
    void defaultReadDoesNotInsertAndOtherOwnersCannotReadOrWrite() {
        DocumentRecord document = document(7L, "private.pdf", "PDF", 2);

        ProgressView view = service.get(7L, document.getId());

        assertEquals(1, view.getPageNumber());
        assertEquals(0, view.getScrollRatio());
        assertEquals(1, view.getZoom());
        assertNull(view.getUpdatedAt());
        assertNull(progress.selectOwned(7L, document.getId()));
        assertThrows(com.smartdoc.document.DocumentNotFoundException.class, () -> service.get(8L, document.getId()));
        assertThrows(com.smartdoc.document.DocumentNotFoundException.class,
                () -> service.save(8L, document.getId(), new ProgressInput(1, 0.0, 1.0)));
    }

    @Test
    void savingUpdatesRecentReadingAndRecentIsOwnerScopedOrderedAndClamped() throws Exception {
        DocumentRecord older = document(3L, "older.pdf", "PDF", 2);
        DocumentRecord newer = document(3L, "newer.pdf", "PDF", 2);
        DocumentRecord other = document(4L, "other.pdf", "PDF", 2);
        service.save(3L, older.getId(), new ProgressInput(1, 0.0, 1.0));
        Thread.sleep(5);
        service.save(4L, other.getId(), new ProgressInput(1, 0.0, 1.0));
        Thread.sleep(5);
        service.save(3L, newer.getId(), new ProgressInput(1, 0.0, 1.0));

        List<DocumentRecord> recent = service.recent(3L, 1000);

        assertEquals(List.of(newer.getId(), older.getId()), List.of(recent.get(0).getId(), recent.get(1).getId()));
        assertEquals(1, service.recent(3L, 0).size());
    }

    private DocumentRecord document(long owner, String name, String type, int pageCount) {
        LocalDateTime now = LocalDateTime.now();
        DocumentRecord document = new DocumentRecord();
        document.setUserId(owner); document.setName(name); document.setDocumentType(type);
        document.setMimeType("application/octet-stream"); document.setSizeBytes(1L);
        document.setPageCount(pageCount); document.setStorageKey("test/" + name);
        document.setStatus("READY"); document.setFavorite(false);
        document.setCreatedAt(now); document.setUpdatedAt(now);
        documents.insert(document);
        return document;
    }
}
