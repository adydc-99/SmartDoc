package com.smartdoc.ai;

import com.smartdoc.document.TextChunk;
import java.util.List;

public class RoutingAiClient implements AiClient {
    private final AiSettingsService settings; private final DailyAiQuota quota; private final DemoAiClient demo; private final DeepSeekClientFactory factory;
    public RoutingAiClient(AiSettingsService settings,DailyAiQuota quota,DemoAiClient demo,DeepSeekClientFactory factory){this.settings=settings;this.quota=quota;this.demo=demo;this.factory=factory;}
    public AiSummary summarize(String text){if(settings.current().getMode()==AiMode.DEMO)return demo.summarize(text);return network().summarize(text);}
    public String answer(String question,List<TextChunk> references){if(settings.current().getMode()==AiMode.DEMO)return demo.answer(question,references);return network().answer(question,references);}
    public String complete(String systemInstruction,String userPrompt){if(settings.current().getMode()==AiMode.DEMO)return demo.complete(systemInstruction,userPrompt);return network().complete(systemInstruction,userPrompt);}
    public AiMode mode(){return settings.current().getMode();}
    public String model(){return settings.current().getModel();}
    public AiTestResult test(){AiSettings s=settings.current();long start=System.nanoTime();if(s.getMode()==AiMode.DEMO)return new AiTestResult((System.nanoTime()-start)/1_000_000,s.getModel(),s.getMode());network().probe();return new AiTestResult((System.nanoTime()-start)/1_000_000,s.getModel(),s.getMode());}
    private NetworkAiClient network(){AiSettings s=settings.current();String key=settings.currentKey().orElseThrow(()->new IllegalStateException("DeepSeek API key is not configured"));quota.consume(s.getDailyLimit());return factory.create(s,key);}
}
