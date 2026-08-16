package com.smartdoc.ai;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class AiActionResponse {
 private final Long id;private final AiAction action;private final String content,mode;private final boolean cached;private final Source source;private final List<Source> sources;private final LocalDateTime createdAt;
 public AiActionResponse(Long id,AiAction action,String content,String mode,boolean cached,Source source,LocalDateTime createdAt){this(id,action,content,mode,cached,source,source==null?Collections.emptyList():Collections.singletonList(source),createdAt);}
 public AiActionResponse(Long id,AiAction action,String content,String mode,boolean cached,Source source,List<Source> sources,LocalDateTime createdAt){this.id=id;this.action=action;this.content=content;this.mode=mode;this.cached=cached;this.source=source;this.sources=sources==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(sources));this.createdAt=createdAt;}
 public Long getId(){return id;}public AiAction getAction(){return action;}public String getContent(){return content;}public String getMode(){return mode;}public boolean isCached(){return cached;}public Source getSource(){return source;}public List<Source> getSources(){return sources;}public LocalDateTime getCreatedAt(){return createdAt;}
 public static class Source {private final long documentId;private final Integer pageNumber,chunkIndex;private final String text,relevance;public Source(long documentId,Integer pageNumber,String text){this(documentId,pageNumber,null,text,"RELATED");}public Source(long documentId,Integer pageNumber,Integer chunkIndex,String text,String relevance){this.documentId=documentId;this.pageNumber=pageNumber;this.chunkIndex=chunkIndex;this.text=text;this.relevance=relevance;}public long getDocumentId(){return documentId;}public Integer getPageNumber(){return pageNumber;}public Integer getChunkIndex(){return chunkIndex;}public String getText(){return text;}public String getRelevance(){return relevance;}}
}
