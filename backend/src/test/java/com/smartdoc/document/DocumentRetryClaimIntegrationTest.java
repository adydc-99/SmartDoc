package com.smartdoc.document;

import com.smartdoc.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:retry-claim;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
class DocumentRetryClaimIntegrationTest {
    @Autowired DocumentMapper documents;

    @Test
    void sameFailedAttemptCanBeClaimedOnlyOnceEvenAfterRapidFailure() {
        DocumentRecord document=new DocumentRecord();document.setUserId(9L);document.setName("retry.md");document.setSizeBytes(8L);
        document.setStorageKey("retry.md");document.setStatus("FAILED");document.setDocumentType("MARKDOWN");document.setFavorite(false);
        document.setProcessingVersion(0L);document.setCreatedAt(LocalDateTime.now());document.setUpdatedAt(document.getCreatedAt());documents.insert(document);

        assertEquals(1,documents.claimFailed(document.getId(),9L,0L,LocalDateTime.now()));
        DocumentRecord rapidlyFailed=documents.selectById(document.getId());rapidlyFailed.setStatus("FAILED");documents.updateById(rapidlyFailed);
        assertEquals(0,documents.claimFailed(document.getId(),9L,0L,LocalDateTime.now()));
        assertEquals(1L,documents.selectById(document.getId()).getProcessingVersion());
    }
}
