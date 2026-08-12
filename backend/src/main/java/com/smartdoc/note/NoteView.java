package com.smartdoc.note;
import com.smartdoc.library.TagView;
import java.time.LocalDateTime;
import java.util.List;
public class NoteView {
    private final Long id,documentId; private final Integer pageNumber; private final String sourceText,contentMarkdown;
    private final Boolean favorite; private final LocalDateTime createdAt,updatedAt; private final List<TagView> tags;
    public NoteView(NoteRecord n,List<TagView> tags){id=n.getId();documentId=n.getDocumentId();pageNumber=n.getPageNumber();sourceText=n.getSourceText();contentMarkdown=n.getContentMarkdown();favorite=n.getFavorite();createdAt=n.getCreatedAt();updatedAt=n.getUpdatedAt();this.tags=tags;}
    public Long getId(){return id;} public Long getDocumentId(){return documentId;} public Integer getPageNumber(){return pageNumber;}
    public String getSourceText(){return sourceText;} public String getContentMarkdown(){return contentMarkdown;} public Boolean getFavorite(){return favorite;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public List<TagView> getTags(){return tags;}
}
