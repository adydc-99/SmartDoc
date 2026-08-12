package com.smartdoc.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DocumentLockManager {
    private final ConcurrentHashMap<Long, LockEntry> locks = new ConcurrentHashMap<>();
    public Handle acquire(long documentId) {
        LockEntry entry = locks.compute(documentId,(ignored,current) -> {
            LockEntry selected=current==null?new LockEntry():current;selected.references.incrementAndGet();return selected;
        });
        entry.lock.lock(); return new Handle(documentId, entry);
    }
    public final class Handle implements AutoCloseable {
        private final long id; private final LockEntry entry; private boolean closed;
        private Handle(long id, LockEntry entry) { this.id=id; this.entry=entry; }
        @Override public void close() {
            if (closed) return; closed=true;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) { release(); }
                });
            } else release();
        }
        private void release() {
            entry.lock.unlock();
            locks.computeIfPresent(id,(ignored,current) -> {
                if(current!=entry)return current;
                return entry.references.decrementAndGet()==0?null:entry;
            });
        }
    }
    private static final class LockEntry{final ReentrantLock lock=new ReentrantLock();final AtomicInteger references=new AtomicInteger();}
}
