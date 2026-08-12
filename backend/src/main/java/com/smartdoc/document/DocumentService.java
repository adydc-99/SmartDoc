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
    public DocumentRecord upload(long userId, MultipartFile file) throws Exception {
        validator.validate(file);
        String key = storage.save(file.getOriginalFilename(), file.getInputStream());
        try {
            DocumentRecord doc = new DocumentRecord(); doc.setUserId(userId); doc.setName(file.getOriginalFilename());
            doc.setSizeBytes(file.getSize()); doc.setStorageKey(key); doc.setStatus("PROCESSING");
            doc.setCreatedAt(LocalDateTime.now()); doc.setUpdatedAt(doc.getCreatedAt()); documents.insert(doc);
            try { processor.process(doc.getId()); }
            catch (RuntimeException rejected) { doc.setStatus("FAILED"); doc.setErrorMessage("处理队列繁忙，请删除后重试"); documents.updateById(doc); }
            return doc;
        } catch (Exception e) {
            try { storage.delete(key); } catch (Exception cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
    public List<DocumentRecord> list(long userId) {
        return documents.selectList(new LambdaQueryWrapper<DocumentRecord>().eq(DocumentRecord::getUserId, userId)
                .orderByDesc(DocumentRecord::getCreatedAt));
    }
    public DocumentRecord get(long userId, long id) { DocumentRecord doc=documents.selectById(id); access.requireOwner(doc,userId); return doc; }
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
}
