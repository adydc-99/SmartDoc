package com.smartdoc.ai;

public final class AiSettings {
    private final AiMode mode; private final String baseUrl; private final String model;
    private final boolean persistKey; private final int maxOutputTokens; private final int dailyLimit;
    public AiSettings(AiMode mode,String baseUrl,String model,boolean persistKey,int maxOutputTokens,int dailyLimit){
        this.mode=mode;this.baseUrl=baseUrl;this.model=model;this.persistKey=persistKey;this.maxOutputTokens=maxOutputTokens;this.dailyLimit=dailyLimit;
    }
    public static AiSettings defaults(){return new AiSettings(AiMode.DEMO,"https://api.deepseek.com/v1","deepseek-chat",false,1024,50);}
    public AiMode getMode(){return mode;} public String getBaseUrl(){return baseUrl;} public String getModel(){return model;}
    public boolean isPersistKey(){return persistKey;} public int getMaxOutputTokens(){return maxOutputTokens;} public int getDailyLimit(){return dailyLimit;}
}
