package com.smartdoc.ai;

import com.smartdoc.ai.mapper.AiResultMapper;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.document.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiActionService {
 private static final String SYSTEM="你是 SmartDoc 学习助手。仅依据用户提供的当前文档内容回答；证据不足时明确说明。使用 Markdown，不得声称已执行代码。";
 private static final Set<AiAction> SELECTED_ACTIONS=EnumSet.of(AiAction.EXPLAIN,AiAction.SUMMARIZE,AiAction.EXPLAIN_CODE,AiAction.LINE_BY_LINE,AiAction.COMPLEXITY,AiAction.FIND_ISSUES,AiAction.GENERATE_EXAMPLE,AiAction.INTERVIEW_QUESTION);
 private static final Set<AiAction> CODE_ACTIONS=EnumSet.of(AiAction.EXPLAIN_CODE,AiAction.LINE_BY_LINE,AiAction.COMPLEXITY,AiAction.FIND_ISSUES,AiAction.GENERATE_EXAMPLE,AiAction.INTERVIEW_QUESTION);
 private static final Pattern CODE_MARKER=Pattern.compile("(?s)(\\b(class|interface|enum|public|private|protected|function|def|return|SELECT|INSERT|UPDATE|DELETE|CREATE)\\b|=>|\\{[^}]*}|\\b[A-Za-z_$][\\w$]*\\s*=.+;|^[ \\t]*(if|for|while|switch)\\s*\\()",Pattern.MULTILINE);
 private final DocumentMapper documents;private final DocumentChunkMapper chunks;private final AiResultMapper results;private final AiClient ai;private final DocumentLockManager locks;private final int contextLimit,sourceLimit;
 @Autowired public AiActionService(DocumentMapper documents,DocumentChunkMapper chunks,AiResultMapper results,AiClient ai,DocumentLockManager locks,
   @Value("${smartdoc.ai.context-code-points:24000}")int contextLimit,@Value("${smartdoc.ai.source-code-points:4000}")int sourceLimit){this.documents=documents;this.chunks=chunks;this.results=results;this.ai=ai;this.locks=locks;this.contextLimit=contextLimit;this.sourceLimit=sourceLimit;}
 @Transactional public AiActionResponse execute(long userId,long documentId,AiActionRequest request){
  try(DocumentLockManager.Handle ignored=locks.acquire(documentId)){
   DocumentRecord document=documents.selectOwnedForUpdate(documentId,userId);if(document==null)throw new DocumentNotFoundException();if(!"READY".equals(document.getStatus()))throw new InvalidDocumentException("文档尚未解析完成");
   if(request==null||request.getAction()==null)throw new InvalidDocumentException("AI 操作不能为空");
   AiAction action=request.getAction();String question=normalize(request.getQuestion());if(action==AiAction.ASK&&(points(question)<2||points(question)>500))throw new InvalidDocumentException("问题长度应为 2-500 字");
   String selectedOriginal=normalizeLineEndings(request.getSelectedText()),selected=selectedOriginal.trim();if(SELECTED_ACTIONS.contains(action)&&(points(selected)<1||points(selected)>12_000))throw new InvalidDocumentException("选中内容长度应为 1-12000 字");
   if(action==AiAction.CURRENT_PAGE_SUMMARY&&!validPage(document,request.getPageNumber()))throw new InvalidDocumentException("当前页码无效");
   if(SELECTED_ACTIONS.contains(action)&&request.getPageNumber()!=null&&!validPage(document,request.getPageNumber()))throw new InvalidDocumentException("选区页码无效");
   if(CODE_ACTIONS.contains(action)&&!"CODE".equals(document.getDocumentType())&&!looksLikeCode(selected))throw new InvalidDocumentException("该操作仅适用于代码文档或代码选区");
   String mode=ai.mode().name(),model=ai.model();String key=hash(documentId+"|"+action+"|"+value(request.getPageNumber())+"|"+question+"|"+selected+"|"+mode+"|"+model);
   if(!request.isForce()){AiResultRecord cached=results.selectLatestByCacheKey(documentId,key);if(cached!=null)return response(cached,true);}
   List<DocumentChunkRecord> ordered=chunks.selectOwnedOrdered(userId,documentId);List<DocumentChunkRecord> relevant=action==AiAction.CURRENT_PAGE_SUMMARY?page(ordered,request.getPageNumber()):ordered;
   String context=SELECTED_ACTIONS.contains(action)?truncate(selectedOriginal,contextLimit):boundedContext(relevant,contextLimit);
   String source=SELECTED_ACTIONS.contains(action)?truncate(selectedOriginal,sourceLimit):truncate(exactText(relevant),sourceLimit);
   String content=truncate(ai.complete(SYSTEM,prompt(action,question,context)),100_000);
   AiResultRecord row=new AiResultRecord();row.setDocumentId(documentId);row.setAction(action.name());row.setCacheKey(key);row.setSourcePage(action==AiAction.CURRENT_PAGE_SUMMARY||SELECTED_ACTIONS.contains(action)?request.getPageNumber():null);row.setSourceText(source);row.setContentMarkdown(content);row.setMode(mode);row.setModel(model);row.setCreatedAt(LocalDateTime.now());
   if(results.insert(row)!=1||row.getId()==null)throw new IllegalStateException("AI result insert failed");AiResultRecord stored=results.selectById(row.getId());if(stored==null)throw new IllegalStateException("AI result insert verification failed");return response(stored,false);
  }
 }
 private static AiActionResponse response(AiResultRecord row,boolean cached){return new AiActionResponse(row.getId(),AiAction.valueOf(row.getAction()),row.getContentMarkdown(),row.getMode(),cached,new AiActionResponse.Source(row.getDocumentId(),row.getSourcePage(),row.getSourceText()),row.getCreatedAt());}
 private static String normalize(String value){return value==null?"":value.replace("\r\n","\n").replace('\r','\n').trim();}
 private static String normalizeLineEndings(String value){return value==null?"":value.replace("\r\n","\n").replace('\r','\n');}
 private static int points(String value){return value.codePointCount(0,value.length());}
 private static String value(Integer v){return v==null?"":v.toString();}
 private static boolean validPage(DocumentRecord document,Integer page){return page!=null&&page>=1&&document.getPageCount()!=null&&page<=document.getPageCount();}
 private static boolean looksLikeCode(String value){return CODE_MARKER.matcher(value).find();}
 private static List<DocumentChunkRecord> page(List<DocumentChunkRecord> chunks,int page){List<DocumentChunkRecord> selected=new ArrayList<>();for(DocumentChunkRecord chunk:chunks)if(chunk.getPageNumber()!=null&&chunk.getPageNumber()==page)selected.add(chunk);if(selected.isEmpty())throw new InvalidDocumentException("当前页没有可用文本");return selected;}
 private static String exactText(List<DocumentChunkRecord> chunks){StringBuilder out=new StringBuilder();for(DocumentChunkRecord chunk:chunks){if(out.length()>0)out.append('\n');out.append(chunk.getContent());}return out.toString();}
 private static String prompt(AiAction action,String question,String context){String instruction;switch(action){
  case ASK:instruction="仅根据文档证据回答问题；引用相关页码，证据不足时明确说明。\n问题："+question;break;
  case DOCUMENT_SUMMARY:instruction="总结整篇文档，提炼主题、关键观点和结论。";break;
  case CURRENT_PAGE_SUMMARY:instruction="总结当前页，保留关键事实和术语。";break;
  case EXPLAIN:instruction="用清晰易懂的语言解释选中内容。";break;
  case SUMMARIZE:instruction="简洁概括选中内容。";break;
  case EXPLAIN_CODE:instruction="解释代码的用途、输入、输出和关键逻辑；不得声称已执行代码。";break;
  case LINE_BY_LINE:instruction="使用 Markdown 表格逐行解释代码，列为行号、代码、说明；不得声称已执行代码。";break;
  case COMPLEXITY:instruction="分析时间复杂度和空间复杂度，说明推导依据；不得声称已执行代码。";break;
  case FIND_ISSUES:instruction="按严重程度列表列出正确性、安全性、性能和可维护性问题；不得声称已执行代码。";break;
  case GENERATE_EXAMPLE:instruction="生成使用示例并解释预期结果；示例未经执行，不得声称已执行代码。";break;
  case INTERVIEW_QUESTION:instruction="基于选中代码生成面试题、考察点和参考答案；不得声称已执行代码。";break;
  default:throw new IllegalArgumentException("Unsupported AI action");}
  return instruction+"\n\n材料：\n"+context;
 }
 private static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder(64);for(byte b:bytes)out.append(String.format("%02x",b));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 private static String boundedContext(List<DocumentChunkRecord> chunks,int limit){StringBuilder out=new StringBuilder();for(DocumentChunkRecord chunk:chunks){String part="[第 "+chunk.getPageNumber()+" 页]\n"+chunk.getContent()+"\n\n";int remaining=limit-points(out.toString());if(remaining<=0)break;out.append(truncate(part,remaining));}return out.toString().trim();}
 private static String truncate(String value,int max){if(value==null)return "";int count=points(value);if(count<=max)return value;int end=value.offsetByCodePoints(0,max);return value.substring(0,end);}
}
