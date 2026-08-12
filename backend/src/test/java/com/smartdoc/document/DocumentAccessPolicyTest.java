package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAccessPolicyTest {
    @Test
    void hidesDocumentsOwnedByAnotherUser() {
        DocumentRecord record = new DocumentRecord();
        record.setUserId(2L);
        assertThrows(DocumentNotFoundException.class, () -> new DocumentAccessPolicy().requireOwner(record, 1L));
        assertDoesNotThrow(() -> new DocumentAccessPolicy().requireOwner(record, 2L));
    }
}
