package com.smartdoc.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DocumentLockManager {
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    public Handle acquire(long documentId) {
        ReentrantLock lock = locks.computeIfAbsent(documentId, ignored -> new ReentrantLock());
        lock.lock(); return new Handle(documentId, lock);
    }
    public final class Handle implements AutoCloseable {
        private final long id; private final ReentrantLock lock; private boolean closed;
        private Handle(long id, ReentrantLock lock) { this.id=id; this.lock=lock; }
        @Override public void close() {
            if (closed) return; closed=true;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) { release(); }
                });
            } else release();
        }
        private void release() { lock.unlock(); if (!lock.hasQueuedThreads()) locks.remove(id, lock); }
    }
}
