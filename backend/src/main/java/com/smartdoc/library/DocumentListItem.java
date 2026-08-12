package com.smartdoc.library;
import com.smartdoc.document.DocumentRecord;
import java.time.LocalDateTime;
import java.util.List;
public class DocumentListItem {
    private final Long id; private final String name; private final Long sizeBytes; private final Integer pageCount;
    private final String status; private final String documentType; private final String mimeType; private final Boolean favorite;
    private final Long folderId; private final LocalDateTime createdAt; private final LocalDateTime updatedAt; private final List<TagView> tags;
    public DocumentListItem(DocumentRecord d,List<TagView> tags){id=d.getId();name=d.getName();sizeBytes=d.getSizeBytes();pageCount=d.getPageCount();status=d.getStatus();documentType=d.getDocumentType();mimeType=d.getMimeType();favorite=d.getFavorite();folderId=d.getFolderId();createdAt=d.getCreatedAt();updatedAt=d.getUpdatedAt();this.tags=tags;}
    public Long getId(){return id;} public String getName(){return name;} public Long getSizeBytes(){return sizeBytes;} public Integer getPageCount(){return pageCount;}
    public String getStatus(){return status;} public String getDocumentType(){return documentType;} public String getMimeType(){return mimeType;} public Boolean getFavorite(){return favorite;}
    public Long getFolderId(){return folderId;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public List<TagView> getTags(){return tags;}
}
