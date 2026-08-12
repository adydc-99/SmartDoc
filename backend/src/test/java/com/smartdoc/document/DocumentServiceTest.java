package com.smartdoc.document;

import com.smartdoc.chat.QuestionRecord;
import com.smartdoc.chat.mapper.QuestionMapper;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentServiceTest {
    @Test
    void compensatesStoredObjectWhenInsertFailsAndPreservesOriginalFailure() throws Exception {
        Fixture fixture = new Fixture();
        RuntimeException insertFailure = new RuntimeException("insert failed");
        doThrow(insertFailure).when(fixture.documents).insert(any(DocumentRecord.class));
        when(fixture.storage.save(any(), any())).thenReturn("stored.md");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> fixture.service.upload(7L, markdown(), null));

        assertSame(insertFailure, thrown);
        verify(fixture.storage).delete("stored.md");
    }

    @Test
    void cleanupFailureIsSuppressedOnOriginalInsertFailure() throws Exception {
        Fixture fixture = new Fixture();
        RuntimeException insertFailure = new RuntimeException("insert failed");
        doThrow(insertFailure).when(fixture.documents).insert(any(DocumentRecord.class));
        when(fixture.storage.save(any(), any())).thenReturn("stored.md");
        doThrow(new IllegalStateException("cleanup failed")).when(fixture.storage).delete("stored.md");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> fixture.service.upload(7L, markdown(), null));

        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("cleanup failed", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    void executorRejectionImmediatelyMarksUploadedDocumentFailed() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.storage.save(any(), any())).thenReturn("stored.md");
        doAnswer(invocation -> { ((DocumentRecord) invocation.getArgument(0)).setId(42L); return 1; })
                .when(fixture.documents).insert(any(DocumentRecord.class));
        doThrow(new TaskRejectedException("full")).when(fixture.processor).process(42L);

        DocumentRecord result = fixture.service.upload(7L, markdown(), null);

        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getErrorMessage().contains("队列"));
        verify(fixture.documents).updateById(result);
    }

    @Test
    void postInsertFailureNeverDeletesBackingObject() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.storage.save(any(), any())).thenReturn("stored.md");
        doAnswer(invocation -> { ((DocumentRecord) invocation.getArgument(0)).setId(42L); return 1; })
                .when(fixture.documents).insert(any(DocumentRecord.class));
        doThrow(new TaskRejectedException("full")).when(fixture.processor).process(42L);
        doThrow(new IllegalStateException("failed-state update failed")).when(fixture.documents).updateById(any(DocumentRecord.class));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> fixture.service.upload(7L, markdown(), null));

        assertEquals("failed-state update failed",thrown.getMessage());
        verify(fixture.storage,never()).delete(any());
    }

    @Test
    void retriesOnlyFailedDocumentsAfterClearingStaleState() {
        Fixture fixture = new Fixture();
        DocumentRecord failed = ownedDocument("FAILED");
        failed.setErrorMessage("old error");
        failed.setContentText("old text");
        failed.setPageCount(9);
        failed.setSummary("old summary");
        failed.setKeywords("old keywords");
        when(fixture.documents.selectById(42L)).thenReturn(failed);

        DocumentRecord result = fixture.service.retry(7L, 42L);

        assertEquals("PROCESSING", result.getStatus());
        assertNull(result.getErrorMessage());
        assertNull(result.getContentText());
        assertNull(result.getPageCount());
        assertNull(result.getSummary());
        assertNull(result.getKeywords());
        verify(fixture.chunks).delete(any());
        verify(fixture.processor).process(42L);
    }

    @Test
    void retryRejectionNeverLeavesDocumentProcessing() {
        Fixture fixture = new Fixture();
        DocumentRecord failed = ownedDocument("FAILED");
        when(fixture.documents.selectById(42L)).thenReturn(failed);
        doThrow(new TaskRejectedException("full")).when(fixture.processor).process(42L);

        DocumentRecord result = fixture.service.retry(7L,42L);

        assertEquals("FAILED",result.getStatus());
        assertTrue(result.getErrorMessage().contains("队列"));
    }

    @Test
    void concurrentRetriesSubmitExactlyOnce() throws Exception {
        Fixture fixture = new Fixture();
        AtomicReference<String> status = new AtomicReference<>("FAILED");
        when(fixture.documents.selectById(42L)).thenAnswer(invocation -> {
            String snapshot=status.get();
            return ownedDocument(snapshot);
        });
        doAnswer(invocation -> {status.set(((DocumentRecord)invocation.getArgument(0)).getStatus());return 1;})
                .when(fixture.documents).updateById(any(DocumentRecord.class));
        ExecutorService callers=Executors.newFixedThreadPool(2);
        try{
            Future<?> first=callers.submit(() -> retryIgnoringExpectedConflict(fixture.service));
            Future<?> second=callers.submit(() -> retryIgnoringExpectedConflict(fixture.service));
            first.get(2,TimeUnit.SECONDS);second.get(2,TimeUnit.SECONDS);
        }finally{callers.shutdownNow();}

        verify(fixture.processor,times(1)).process(42L);
    }

    @Test
    void rejectsRetryUnlessDocumentFailed() {
        Fixture fixture = new Fixture();
        when(fixture.documents.selectById(42L)).thenReturn(ownedDocument("READY"));

        assertThrows(InvalidDocumentException.class, () -> fixture.service.retry(7L, 42L));
        verifyNoInteractions(fixture.processor);
    }

    @Test
    void deleteImpactCountsQuestionHistoryAndLeavesFutureCountsZero() {
        Fixture fixture = new Fixture();
        when(fixture.documents.selectById(42L)).thenReturn(ownedDocument("READY"));
        when(fixture.questions.selectCount(any())).thenReturn(3L);

        DocumentService.DeleteImpact impact = fixture.service.deleteImpact(7L, 42L);

        assertEquals(0, impact.getNotes());
        assertEquals(0, impact.getExcerpts());
        assertEquals(3, impact.getQuestions());
        assertEquals(0, impact.getAiResults());
        assertEquals(0, impact.getReviewItems());
    }

    private static DocumentRecord ownedDocument(String status) {
        DocumentRecord document = new DocumentRecord();
        document.setId(42L);
        document.setUserId(7L);
        document.setStatus(status);
        return document;
    }

    private static void retryIgnoringExpectedConflict(DocumentService service){try{service.retry(7L,42L);}catch(InvalidDocumentException expected){/* competing retry lost */}}

    private static MockMultipartFile markdown() {
        return new MockMultipartFile("file", "README.MD", "text/markdown", "# Hello".getBytes(StandardCharsets.UTF_8));
    }

    private static final class Fixture {
        final DocumentMapper documents = mock(DocumentMapper.class);
        final DocumentChunkMapper chunks = mock(DocumentChunkMapper.class);
        final FileStorage storage = mock(FileStorage.class);
        final QuestionMapper questions = mock(QuestionMapper.class);
        final DocumentProcessor processor = mock(DocumentProcessor.class);
        final DocumentService service = new DocumentService(documents, chunks, storage, questions,
                new DocumentUploadValidator(20L * 1024 * 1024), new DocumentAccessPolicy(), processor, new DocumentLockManager());
    }
}
