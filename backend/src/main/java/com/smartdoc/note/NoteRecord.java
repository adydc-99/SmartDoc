package com.smartdoc.note;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("note")
public class NoteRecord {
    @TableId(type=IdType.AUTO) private Long id;
    private Long documentId; private Integer pageNumber; private String sourceText; private String contentMarkdown;
    private Boolean favorite; private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long value){documentId=value;}
    public Integer getPageNumber(){return pageNumber;} public void setPageNumber(Integer value){pageNumber=value;}
    public String getSourceText(){return sourceText;} public void setSourceText(String value){sourceText=value;}
    public String getContentMarkdown(){return contentMarkdown;} public void setContentMarkdown(String value){contentMarkdown=value;}
    public Boolean getFavorite(){return favorite;} public void setFavorite(Boolean value){favorite=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
