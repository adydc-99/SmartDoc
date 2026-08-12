package com.smartdoc.document;
public class DocumentAccessPolicy {
    public void requireOwner(DocumentRecord document, long userId) {
        if (document == null || document.getUserId() == null || document.getUserId() != userId) {
            throw new DocumentNotFoundException();
        }
    }
}
