package com.smartdoc.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartdoc.document.mapper.*;
import com.smartdoc.storage.FileStorage;
import com.smartdoc.chat.QuestionRecord;
import com.smartdoc.chat.mapper.QuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {
    private final DocumentMapper documents; private final DocumentChunkMapper chunks; private final FileStorage storage;
    private final QuestionMapper questions; private final DocumentUploadValidator validator; private final DocumentAccessPolicy access;
    private final DocumentProcessor processor; private final DocumentLockManager locks;
    public DocumentService(DocumentMapper documents, DocumentChunkMapper chunks, FileStorage storage,
                           QuestionMapper questions, DocumentUploadValidator validator, DocumentAccessPolicy access,
                           DocumentProcessor processor, DocumentLockManager locks) {
        this.documents=documents; this.chunks=chunks; this.storage=storage; this.questions=questions;
        this.validator=validator; this.access=access; this.processor=processor; this.locks=locks;
    }
    public DocumentRecord upload(long userId, MultipartFile file) throws Exception { return upload(userId,file,null); }
    public DocumentRecord upload(long userId, MultipartFile file, Long folderId) throws Exception {
        validator.validate(file);
        String key;
        try(InputStream input=file.getInputStream()){key=storage.save(file.getOriginalFilename(),input);}
        DocumentRecord doc = new DocumentRecord();
        try {
            DocumentType type=DocumentType.fromFilename(file.getOriginalFilename());
            doc.setUserId(userId); doc.setName(file.getOriginalFilename());
            doc.setSizeBytes(file.getSize()); doc.setStorageKey(key); doc.setStatus("PROCESSING");
            doc.setDocumentType(type.name());doc.setMimeType(file.getContentType());doc.setFavorite(false);doc.setFolderId(folderId);
            doc.setCreatedAt(LocalDateTime.now()); doc.setUpdatedAt(doc.getCreatedAt());
            int inserted=documents.insert(doc);
            if(inserted!=1 || doc.getId()==null)throw new IllegalStateException("Document insert did not persist exactly one row with an id");
        } catch (Exception e) {
            try { storage.delete(key); } catch (Exception cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
        submit(doc);
        return doc;
    }
    public DocumentRecord retry(long userId,long id){
        DocumentRecord doc=get(userId,id);if(!"FAILED".equals(doc.getStatus()))throw new InvalidDocumentException("只有解析失败的文档可以重试");
        long expectedVersion=doc.getProcessingVersion()==null?0L:doc.getProcessingVersion();
        try(DocumentLockManager.Handle ignored=locks.acquire(id)){
            LocalDateTime claimedAt=LocalDateTime.now();
            int claimed=documents.claimFailed(id,userId,expectedVersion,claimedAt);
            if(claimed!=1)throw new InvalidDocumentException("文档重试状态已变化，请刷新后重试");
            chunks.delete(new LambdaQueryWrapper<DocumentChunkRecord>().eq(DocumentChunkRecord::getDocumentId,id));
            doc.setStatus("PROCESSING");doc.setErrorMessage(null);doc.setContentText(null);doc.setPageCount(null);doc.setSummary(null);doc.setKeywords(null);doc.setUpdatedAt(claimedAt);doc.setProcessingVersion(expectedVersion+1);
            submit(doc);return doc;
        }
    }
    public DeleteImpact deleteImpact(long userId,long id){get(userId,id);long count=questions.selectCount(new LambdaQueryWrapper<QuestionRecord>().eq(QuestionRecord::getDocumentId,id));return new DeleteImpact(0,0,count,0,0);}
    private void submit(DocumentRecord doc){
        try{processor.process(doc.getId());}
        catch(java.util.concurrent.RejectedExecutionException rejected){
            doc.setStatus("FAILED");doc.setErrorMessage("文档处理队列繁忙，请稍后重试");doc.setUpdatedAt(LocalDateTime.now());
            int updated=documents.updateById(doc);DocumentRecord stored=documents.selectById(doc.getId());
            if(updated!=1 || stored==null || !"FAILED".equals(stored.getStatus()))throw new IllegalStateException("Unable to persist FAILED state after executor rejection",rejected);
        }
    }
    public List<DocumentRecord> list(long userId) {
        return documents.selectList(new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getUserId, userId)
                .orderByDesc(DocumentRecord::getCreatedAt));
    }
    public DocumentRecord get(long userId, long id) { DocumentRecord doc=documents.selectById(id); access.requireOwner(doc,userId); return doc; }
    public java.io.InputStream openContent(long userId,long id) throws Exception {DocumentRecord doc=get(userId,id);return storage.open(doc.getStorageKey());}
    @Transactional public void delete(long userId, long id) throws Exception {
        try (DocumentLockManager.Handle ignored = locks.acquire(id)) {
            DocumentRecord doc=get(userId,id);
            if ("PROCESSING".equals(doc.getStatus())) throw new InvalidDocumentException("文档正在解析，请稍后再删除");
            questions.delete(new LambdaQueryWrapper<QuestionRecord>().eq(QuestionRecord::getDocumentId,id));
            chunks.delete(new LambdaQueryWrapper<DocumentChunkRecord>().eq(DocumentChunkRecord::getDocumentId,id));
            documents.deleteById(id);
            String storageKey=doc.getStorageKey();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { try { storage.delete(storageKey); } catch (Exception ignored) { /* orphan can be cleaned safely */ } }
            });
        }
    }
    public static final class DeleteImpact{
        private final long notes,excerpts,questions,aiResults,reviewItems;
        DeleteImpact(long notes,long excerpts,long questions,long aiResults,long reviewItems){this.notes=notes;this.excerpts=excerpts;this.questions=questions;this.aiResults=aiResults;this.reviewItems=reviewItems;}
        public long getNotes(){return notes;}public long getExcerpts(){return excerpts;}public long getQuestions(){return questions;}public long getAiResults(){return aiResults;}public long getReviewItems(){return reviewItems;}
    }
}
