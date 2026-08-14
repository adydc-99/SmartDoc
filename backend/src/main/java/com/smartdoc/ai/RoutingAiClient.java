package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import com.smartdoc.ai.provider.*;
import java.util.List;

public class RoutingAiClient implements AiClient {
    private final AiSettingsService settings; private final DailyAiQuota quota; private final DemoAiClient demo; private final DeepSeekClientFactory factory; private final ModelRouter providers;
    public RoutingAiClient(AiSettingsService settings,DailyAiQuota quota,DemoAiClient demo,DeepSeekClientFactory factory){this(settings,quota,demo,factory,null);}
    public RoutingAiClient(AiSettingsService settings,DailyAiQuota quota,DemoAiClient demo,DeepSeekClientFactory factory,ModelRouter providers){this.settings=settings;this.quota=quota;this.demo=demo;this.factory=factory;this.providers=providers;}
    public AiSummary summarize(String text){if(settings.current().getMode()==AiMode.DEMO)return demo.summarize(text);return network().summarize(text);}
    public String answer(String question,List<TextChunk> references){if(settings.current().getMode()==AiMode.DEMO)return demo.answer(question,references);return network().answer(question,references);}
    public String complete(String systemInstruction,String userPrompt){if(settings.current().getMode()==AiMode.DEMO)return demo.complete(systemInstruction,userPrompt);return network().complete(systemInstruction,userPrompt);}
    public AiMode mode(){return settings.current().getMode();}
    public String model(){return settings.current().getModel();}
    public AiTestResult test(){AiSettings s=settings.current();long start=System.nanoTime();if(s.getMode()==AiMode.DEMO)return new AiTestResult((System.nanoTime()-start)/1_000_000,s.getModel(),s.getMode());network().probe();return new AiTestResult((System.nanoTime()-start)/1_000_000,s.getModel(),s.getMode());}
    public String complete(long userId,String systemInstruction,String userPrompt){if(providers==null)return complete(systemInstruction,userPrompt);if(!providers.hasConfiguredDefault(userId,ProviderCapability.TEXT))return demo.complete(systemInstruction,userPrompt);ProviderSession session=providers.require(userId,ProviderCapability.TEXT);AiRoutingConfig routing=providers.routingFor(userId);quota.consume(userId,routing.getDailyLimit());return session.getAdapter().complete(session.getProvider(),session.getApiKey(),new TextCompletionRequest(systemInstruction,userPrompt,routing.getMaxOutputTokens())).getContent();}
    public AiMode mode(long userId){return providers!=null&&providers.hasConfiguredDefault(userId,ProviderCapability.TEXT)?AiMode.DEEPSEEK:AiMode.DEMO;}
    public String model(long userId){if(providers==null||!providers.hasConfiguredDefault(userId,ProviderCapability.TEXT))return "demo";return providers.require(userId,ProviderCapability.TEXT).getProvider().getModel();}
    public String providerIdentity(long userId){if(providers==null||!providers.hasConfiguredDefault(userId,ProviderCapability.TEXT))return "DEMO|demo|demo";AiProviderConfig p=providers.require(userId,ProviderCapability.TEXT).getProvider();return p.getProtocol()+"|"+p.getId()+"|"+p.getModel();}
    private NetworkAiClient network(){AiSettings s=settings.current();String key=settings.currentKey().orElseThrow(()->new IllegalStateException("DeepSeek API key is not configured"));quota.consume(s.getDailyLimit());return factory.create(s,key);}
}
