package com.smartdoc.document;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@TableName("document_record")
public class DocumentRecord {
    @TableId(type = IdType.AUTO) private Long id;
    @JsonIgnore private Long userId;
    private String name;
    private Long sizeBytes;
    private Integer pageCount;
    @JsonIgnore private String storageKey;
    private String status;
    private String summary;
    private String keywords;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Long getSizeBytes() { return sizeBytes; } public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Integer getPageCount() { return pageCount; } public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getStorageKey() { return storageKey; } public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
    public String getKeywords() { return keywords; } public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
