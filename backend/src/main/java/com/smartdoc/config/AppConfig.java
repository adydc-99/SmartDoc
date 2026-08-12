package com.smartdoc.config;

import com.smartdoc.ai.*;
import com.smartdoc.auth.AuthTokenService;
import com.smartdoc.document.*;
import com.smartdoc.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.UUID;
import java.util.concurrent.Executor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import java.nio.file.Paths;
import java.time.Clock;

@Configuration
public class AppConfig {
    @Bean AuthTokenService authTokenService(@Value("${smartdoc.auth.secret:}") String secret,
            @Value("${smartdoc.auth.ttl-seconds:86400}") long ttl) {
        if (secret == null || secret.length() < 32) secret = UUID.randomUUID().toString() + UUID.randomUUID();
        return new AuthTokenService(secret, ttl, Clock.systemUTC());
    }
    @Bean(name="documentExecutor") Executor documentExecutor() {
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor(); executor.setCorePoolSize(2); executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20); executor.setThreadNamePrefix("document-"); executor.initialize(); return executor;
    }
    @Bean DocumentUploadValidator uploadValidator(@Value("${smartdoc.upload.max-bytes:20971520}") long max) { return new DocumentUploadValidator(max); }
    @Bean DocumentAccessPolicy accessPolicy() { return new DocumentAccessPolicy(); }
    @Bean TextChunker textChunker() { return new TextChunker(1000, 120); }
    @Bean KeywordRetriever keywordRetriever() { return new KeywordRetriever(); }
    @Bean PdfTextExtractor pdfTextExtractor() { return new PdfTextExtractor(); }
    @Bean @ConditionalOnProperty(name="smartdoc.storage.type",havingValue="local",matchIfMissing=true)
    FileStorage localFileStorage(@Value("${smartdoc.storage.local-path:./data/files}") String path) { return new LocalFileStorage(Paths.get(path)); }
    @Bean @ConditionalOnProperty(name="smartdoc.storage.type",havingValue="minio")
    FileStorage minioFileStorage(@Value("${smartdoc.storage.minio.endpoint}") String endpoint,@Value("${smartdoc.storage.minio.access-key}") String access,
            @Value("${smartdoc.storage.minio.secret-key}") String secret,@Value("${smartdoc.storage.minio.bucket:smartdoc}") String bucket){
        return new MinioFileStorage(MinioClient.builder().endpoint(endpoint).credentials(access,secret).build(),bucket);
    }
    @Bean @ConditionalOnProperty(name="smartdoc.ai.type",havingValue="demo",matchIfMissing=true) AiClient demoAiClient() { return new DemoAiClient(); }
    @Bean @ConditionalOnProperty(name="smartdoc.ai.type",havingValue="openai") AiClient openAiClient(ObjectMapper json,
            @Value("${smartdoc.ai.base-url}") String base,@Value("${smartdoc.ai.api-key}") String key,@Value("${smartdoc.ai.model}") String model){
        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(10000); factory.setReadTimeout(60000);
        return new OpenAiCompatibleClient(new RestTemplate(factory),json,base,key,model);
    }
}
