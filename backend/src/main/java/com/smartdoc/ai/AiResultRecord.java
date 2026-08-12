package com.smartdoc.ai;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("ai_result") public class AiResultRecord {
 @TableId(type=IdType.AUTO) private Long id;private Long documentId;private String action,cacheKey;private Integer sourcePage;private String sourceText,contentMarkdown,mode,model;private LocalDateTime createdAt;
 public Long getId(){return id;}public void setId(Long v){id=v;}public Long getDocumentId(){return documentId;}public void setDocumentId(Long v){documentId=v;}
 public String getAction(){return action;}public void setAction(String v){action=v;}public String getCacheKey(){return cacheKey;}public void setCacheKey(String v){cacheKey=v;}
 public Integer getSourcePage(){return sourcePage;}public void setSourcePage(Integer v){sourcePage=v;}public String getSourceText(){return sourceText;}public void setSourceText(String v){sourceText=v;}
 public String getContentMarkdown(){return contentMarkdown;}public void setContentMarkdown(String v){contentMarkdown=v;}public String getMode(){return mode;}public void setMode(String v){mode=v;}public String getModel(){return model;}public void setModel(String v){model=v;}public LocalDateTime getCreatedAt(){return createdAt;}public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
