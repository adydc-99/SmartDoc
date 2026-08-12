package com.smartdoc.document;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLockManagerTest {
    @Test
    void keepsEntryUntilEveryHandleReleasesIt() throws Exception {
        DocumentLockManager manager=new DocumentLockManager();
        DocumentLockManager.Handle first=manager.acquire(42L);
        DocumentLockManager.Handle second=manager.acquire(42L);
        first.close();
        ExecutorService caller=Executors.newSingleThreadExecutor();
        CountDownLatch acquired=new CountDownLatch(1);
        Future<?> third=caller.submit(() -> {try(DocumentLockManager.Handle ignored=manager.acquire(42L)){acquired.countDown();}});
        try{
            assertFalse(acquired.await(100,TimeUnit.MILLISECONDS),"third caller bypassed the still-held lock");
            second.close();
            assertTrue(acquired.await(1,TimeUnit.SECONDS));
            third.get(1,TimeUnit.SECONDS);
        }finally{second.close();caller.shutdownNow();}
    }
}
