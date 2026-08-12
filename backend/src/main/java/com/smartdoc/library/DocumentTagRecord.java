package com.smartdoc.library;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("document_tag")
public class DocumentTagRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long tagId;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; } public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getTagId() { return tagId; } public void setTagId(Long tagId) { this.tagId = tagId; }
}
