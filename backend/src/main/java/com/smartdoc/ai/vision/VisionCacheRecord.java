package com.smartdoc.ai.vision;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("ai_vision_cache")
public class VisionCacheRecord {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId,documentId,providerId;
    private String contentSha256,model,promptVersion,observation;
    private LocalDateTime createdAt,expiresAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;} public Long getProviderId(){return providerId;} public void setProviderId(Long v){providerId=v;}
    public String getContentSha256(){return contentSha256;} public void setContentSha256(String v){contentSha256=v;} public String getModel(){return model;} public void setModel(String v){model=v;}
    public String getPromptVersion(){return promptVersion;} public void setPromptVersion(String v){promptVersion=v;} public String getObservation(){return observation;} public void setObservation(String v){observation=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime v){expiresAt=v;}
}
