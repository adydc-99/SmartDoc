package com.smartdoc.ai;
import java.time.LocalDateTime;
public class AiActionResponse {
 private final Long id;private final AiAction action;private final String content,mode;private final boolean cached;private final Source source;private final LocalDateTime createdAt;
 public AiActionResponse(Long id,AiAction action,String content,String mode,boolean cached,Source source,LocalDateTime createdAt){this.id=id;this.action=action;this.content=content;this.mode=mode;this.cached=cached;this.source=source;this.createdAt=createdAt;}
 public Long getId(){return id;}public AiAction getAction(){return action;}public String getContent(){return content;}public String getMode(){return mode;}public boolean isCached(){return cached;}public Source getSource(){return source;}public LocalDateTime getCreatedAt(){return createdAt;}
 public static class Source {private final long documentId;private final Integer pageNumber;private final String text;public Source(long documentId,Integer pageNumber,String text){this.documentId=documentId;this.pageNumber=pageNumber;this.text=text;}public long getDocumentId(){return documentId;}public Integer getPageNumber(){return pageNumber;}public String getText(){return text;}}
}
