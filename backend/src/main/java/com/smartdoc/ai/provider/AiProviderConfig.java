package com.smartdoc.ai.provider;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@TableName("ai_provider_config")
public class AiProviderConfig {
 @TableId(type=IdType.AUTO) private Long id; @JsonIgnore private Long userId;
 private String displayName,presetCode,protocol,baseUrl,model; private Boolean supportsText,supportsVision,enabled;
 @JsonIgnore private String encryptedApiKey; private LocalDateTime createdAt,updatedAt;
 public static AiProviderConfig textProvider(long userId,String name,String preset,String baseUrl,String model){AiProviderConfig r=new AiProviderConfig();r.userId=userId;r.displayName=name;r.presetCode=preset;r.protocol=AiProviderProtocol.OPENAI_CHAT_COMPLETIONS.name();r.baseUrl=baseUrl;r.model=model;r.supportsText=true;r.supportsVision=false;r.enabled=true;r.createdAt=LocalDateTime.now();r.updatedAt=r.createdAt;return r;}
 public boolean supports(ProviderCapability c){return c==ProviderCapability.TEXT?Boolean.TRUE.equals(supportsText):Boolean.TRUE.equals(supportsVision);}
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;} public String getPresetCode(){return presetCode;} public void setPresetCode(String v){presetCode=v;} public String getProtocol(){return protocol;} public void setProtocol(String v){protocol=v;} public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;} public String getModel(){return model;} public void setModel(String v){model=v;} public Boolean getSupportsText(){return supportsText;} public void setSupportsText(Boolean v){supportsText=v;} public Boolean getSupportsVision(){return supportsVision;} public void setSupportsVision(Boolean v){supportsVision=v;} public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;} public String getEncryptedApiKey(){return encryptedApiKey;} public void setEncryptedApiKey(String v){encryptedApiKey=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
