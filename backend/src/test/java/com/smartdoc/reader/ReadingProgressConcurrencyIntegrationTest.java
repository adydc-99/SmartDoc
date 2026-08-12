package com.smartdoc.reader;

import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.reader.mapper.ReadingProgressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:progress-race;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.hikari.maximum-pool-size=8"})
class ReadingProgressConcurrencyIntegrationTest {
    @Autowired ReaderService service;
    @Autowired DocumentMapper documents;
    @Autowired ReadingProgressMapper progress;

    @Test
    void simultaneousFirstSavesUseDatabaseUniqueKeyAndCommitExactlyOneValidRow() throws Exception {
        assertTrue(AopUtils.isAopProxy(service));
        DocumentRecord document=document();
        int workers=8;ExecutorService pool=Executors.newFixedThreadPool(workers);CyclicBarrier barrier=new CyclicBarrier(workers);
        List<Future<ProgressView>> futures=new ArrayList<>();
        try {
            for(int index=0;index<workers;index++){final int page=index+1;futures.add(pool.submit(()->{barrier.await();return service.save(21L,document.getId(),new ProgressInput(page,page/10.0,1.0));}));}
            for(Future<ProgressView> future:futures)assertNotNull(future.get(20,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}

        List<ReadingProgressRecord> rows=progress.selectList(null);
        assertEquals(1,rows.size());
        assertTrue(rows.get(0).getPageNumber()>=1&&rows.get(0).getPageNumber()<=workers);
        assertTrue(rows.get(0).getScrollRatio()>=.1&&rows.get(0).getScrollRatio()<=.8);
        assertNotNull(rows.get(0).getUpdatedAt());
        assertEquals(rows.get(0).getUpdatedAt(),documents.selectOwned(document.getId(),21L).getLastOpenedAt());
    }

    private DocumentRecord document(){LocalDateTime now=LocalDateTime.now();DocumentRecord d=new DocumentRecord();d.setUserId(21L);d.setName("race.pdf");d.setDocumentType("PDF");d.setMimeType("application/pdf");d.setSizeBytes(1L);d.setPageCount(10);d.setStorageKey("test/race.pdf");d.setStatus("READY");d.setFavorite(false);d.setCreatedAt(now);d.setUpdatedAt(now);documents.insert(d);return d;}
}
