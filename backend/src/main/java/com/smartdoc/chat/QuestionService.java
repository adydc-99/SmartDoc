package com.smartdoc.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoc.ai.AiClient;
import com.smartdoc.document.*;
import com.smartdoc.document.mapper.DocumentChunkMapper;
import com.smartdoc.chat.mapper.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final DocumentService documents; private final DocumentChunkMapper chunkMapper; private final QuestionMapper questions;
    private final KeywordRetriever retriever; private final AiClient ai; private final ObjectMapper json; private final DocumentLockManager locks;
    public QuestionService(DocumentService documents,DocumentChunkMapper chunkMapper,QuestionMapper questions,KeywordRetriever retriever,AiClient ai,ObjectMapper json,DocumentLockManager locks){
        this.documents=documents;this.chunkMapper=chunkMapper;this.questions=questions;this.retriever=retriever;this.ai=ai;this.json=json;this.locks=locks;
    }
    @Transactional public AnswerResponse ask(long userId,long documentId,String question) throws Exception {
        try (DocumentLockManager.Handle ignored = locks.acquire(documentId)) {
        if(question==null||question.trim().isEmpty()||question.length()>500) throw new InvalidDocumentException("问题长度应为 1-500 字");
        DocumentRecord doc=documents.getForUpdate(userId,documentId); if(!"READY".equals(doc.getStatus())) throw new InvalidDocumentException("文档尚未解析完成");
        List<TextChunk> all=chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunkRecord>().eq(DocumentChunkRecord::getDocumentId,documentId)
                .orderByAsc(DocumentChunkRecord::getChunkIndex)).stream().map(r->new TextChunk(r.getChunkIndex(),r.getPageNumber(),r.getContent())).collect(Collectors.toList());
        List<TextChunk> refs=retriever.retrieve(question,all,3); String answer=ai.answer(question,refs);
        QuestionRecord row=new QuestionRecord(); row.setDocumentId(documentId);row.setUserId(userId);row.setQuestion(question);row.setAnswer(answer);
        row.setReferencesJson(json.writeValueAsString(refs));row.setCreatedAt(LocalDateTime.now());questions.insert(row);
        return new AnswerResponse(row.getId(),answer,refs,row.getCreatedAt());
        }
    }
    public List<QuestionRecord> history(long userId,long documentId){documents.get(userId,documentId);return questions.selectList(new LambdaQueryWrapper<QuestionRecord>()
            .eq(QuestionRecord::getUserId,userId).eq(QuestionRecord::getDocumentId,documentId).orderByAsc(QuestionRecord::getCreatedAt));}
    public static class AnswerResponse {
        public final Long id; public final String answer; public final List<TextChunk> references; public final LocalDateTime createdAt;
        public AnswerResponse(Long id,String answer,List<TextChunk> references,LocalDateTime createdAt){this.id=id;this.answer=answer;this.references=references;this.createdAt=createdAt;}
    }
}
