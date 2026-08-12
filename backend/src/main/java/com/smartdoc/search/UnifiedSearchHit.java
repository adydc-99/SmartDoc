package com.smartdoc.search;

public class UnifiedSearchHit {
    private final String type;
    private final Long documentId;
    private final Long noteId;
    private final Integer pageNumber;
    private final String title;
    private final String snippet;

    public UnifiedSearchHit(String type, Long documentId, Long noteId, Integer pageNumber, String title, String snippet) {
        this.type=type;this.documentId=documentId;this.noteId=noteId;this.pageNumber=pageNumber;this.title=title;this.snippet=snippet;
    }
    public String getType(){return type;} public Long getDocumentId(){return documentId;} public Long getNoteId(){return noteId;}
    public Integer getPageNumber(){return pageNumber;} public String getTitle(){return title;} public String getSnippet(){return snippet;}
}
