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
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import java.nio.file.Paths;
import java.time.Clock;
import org.springframework.core.env.Environment;
import com.smartdoc.ai.provider.*;
import java.util.*;

@Configuration
public class AppConfig {
    @Bean Clock clock() { return Clock.systemDefaultZone(); }
    @Bean AuthTokenService authTokenService(@Value("${smartdoc.auth.secret:}") String secret,
            @Value("${smartdoc.auth.ttl-seconds:86400}") long ttl, Environment environment) {
        return new AuthTokenService(AuthSecretPolicy.requireValid(secret, environment.getActiveProfiles()), ttl, Clock.systemUTC());
    }
    @Bean(name="documentExecutor") Executor documentExecutor() {
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor(); executor.setCorePoolSize(2); executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20); executor.setThreadNamePrefix("document-"); executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); executor.initialize(); return executor;
    }
    @Bean DocumentUploadValidator uploadValidator(@Value("${smartdoc.upload.max-bytes:20971520}") long max) { return new DocumentUploadValidator(max); }
    @Bean DocumentAccessPolicy accessPolicy() { return new DocumentAccessPolicy(); }
    @Bean TextChunker textChunker() { return new TextChunker(1000, 120); }
    @Bean KeywordRetriever keywordRetriever() { return new KeywordRetriever(); }
    @Bean PdfTextExtractor pdfTextExtractor() { return new PdfTextExtractor(); }
    @Bean TextDocumentExtractor textDocumentExtractor() { return new TextDocumentExtractor(); }
    @Bean @ConditionalOnProperty(name="smartdoc.storage.type",havingValue="local",matchIfMissing=true)
    FileStorage localFileStorage(@Value("${smartdoc.storage.local-path:./data/files}") String path) { return new LocalFileStorage(Paths.get(path)); }
    @Bean @ConditionalOnProperty(name="smartdoc.storage.type",havingValue="minio")
    FileStorage minioFileStorage(@Value("${smartdoc.storage.minio.endpoint}") String endpoint,@Value("${smartdoc.storage.minio.access-key}") String access,
            @Value("${smartdoc.storage.minio.secret-key}") String secret,@Value("${smartdoc.storage.minio.bucket:smartdoc}") String bucket){
        return new MinioFileStorage(MinioClient.builder().endpoint(endpoint).credentials(access,secret).build(),bucket);
    }
    @Bean DailyAiQuota dailyAiQuota(Clock clock){return new DailyAiQuota(clock);}
    @Bean SecretStore secretStore(@Value("${smartdoc.ai.key-file:./data/ai-key.bin}") String path){return new DpapiSecretStore(Paths.get(path));}
    @Bean AiSettingsService aiSettingsService(SecretStore store,DailyAiQuota quota,@Value("${smartdoc.ai.api-key:}") String key){return new AiSettingsService(AiSettings.defaults(),key,store,quota);}
    @Bean DeepSeekClientFactory deepSeekClientFactory(ObjectMapper json){return (settings,key)->{SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(30000);return new OpenAiCompatibleClient(new RestTemplate(factory),json,settings.getBaseUrl(),key,settings.getModel(),settings.getMaxOutputTokens());};}
    @Bean DemoAiClient demoAiClient(){return new DemoAiClient();}
    @Bean @Primary RoutingAiClient aiClient(AiSettingsService settings,DailyAiQuota quota,DemoAiClient demo,DeepSeekClientFactory factory,ModelRouter router){return new RoutingAiClient(settings,quota,demo,factory,router);}
    @Bean ProviderUrlPolicy providerUrlPolicy(@Value("${smartdoc.ai.allow-loopback:false}") boolean loopback,@Value("${smartdoc.ai.allow-private-network:false}") boolean privateNetwork){return new ProviderUrlPolicy(loopback,privateNetwork);}
    @Bean ProviderSecretVault providerSecretVault(AiProviderMapper mapper,@Value("${smartdoc.ai.master-key-base64:}") String master){return new ProviderSecretVault(mapper,master==null||master.trim().isEmpty()?Optional.empty():Optional.of(AesGcmSecretCipher.fromBase64(master)));}
    @Bean RestTemplate providerRestTemplate(){SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory(){@Override protected void prepareConnection(java.net.HttpURLConnection c,String method)throws java.io.IOException{super.prepareConnection(c,method);c.setInstanceFollowRedirects(false);}};factory.setConnectTimeout(5000);factory.setReadTimeout(30000);return new RestTemplate(factory);}
    @Bean OpenAiChatCompletionsAdapter openAiChatCompletionsAdapter(RestTemplate providerRestTemplate,ObjectMapper json){return new OpenAiChatCompletionsAdapter(providerRestTemplate,json,2*1024*1024);}
    @Bean ProviderAdapterRegistry providerAdapterRegistry(OpenAiChatCompletionsAdapter adapter){return new ProviderAdapterRegistry(List.of(adapter));}
    @Bean ModelRouter modelRouter(AiProviderMapper p,AiRoutingMapper r,ProviderSecretVault v,ProviderAdapterRegistry a){return new ModelRouter(p,r,v,a);}
    @Bean AiProviderService aiProviderService(AiProviderMapper p,AiRoutingMapper r,ProviderSecretVault v,ProviderUrlPolicy u,ModelRouter m,DailyAiQuota q){return new AiProviderService(p,r,v,u,m,q);}
}
