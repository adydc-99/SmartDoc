package com.smartdoc.document;

import com.smartdoc.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:executor-rejection;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "smartdoc.storage.local-path=./target/executor-rejection-files"
})
class DocumentExecutorRejectionIntegrationTest {
    @Autowired DocumentService service;
    @Autowired DocumentMapper documents;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("documentExecutor") ThreadPoolTaskExecutor executor;

    @Test
    void realBoundedExecutorRejectionPersistsFailedState() throws Exception {
        CountDownLatch release=new CountDownLatch(1);
        CountDownLatch fourRunning=new CountDownLatch(4);
        Runnable blocker=() -> {fourRunning.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        for(int i=0;i<24;i++)executor.execute(blocker);
        try{
            org.junit.jupiter.api.Assertions.assertTrue(fourRunning.await(2,TimeUnit.SECONDS));
            DocumentRecord uploaded=service.upload(71L,new MockMultipartFile("file","rejected.md","text/markdown","# rejected".getBytes(StandardCharsets.UTF_8)));

            DocumentRecord stored=documents.selectById(uploaded.getId());
            assertNotNull(stored);
            assertEquals("FAILED",stored.getStatus());
            assertNotNull(stored.getErrorMessage());
        }finally{release.countDown();}
    }
}
