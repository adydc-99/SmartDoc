package com.smartdoc.ai;

public final class AiSettingsUpdate {
    private AiMode mode; private String baseUrl; private String model; private String apiKey;
    private boolean persistKey; private int maxOutputTokens; private int dailyLimit;
    public AiSettingsUpdate() {}
    public AiSettingsUpdate(AiMode mode,String baseUrl,String model,String apiKey,boolean persistKey,int maxOutputTokens,int dailyLimit){this.mode=mode;this.baseUrl=baseUrl;this.model=model;this.apiKey=apiKey;this.persistKey=persistKey;this.maxOutputTokens=maxOutputTokens;this.dailyLimit=dailyLimit;}
    public AiMode getMode(){return mode;} public void setMode(AiMode v){mode=v;} public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
    public String getModel(){return model;} public void setModel(String v){model=v;} public String getApiKey(){return apiKey;} public void setApiKey(String v){apiKey=v;}
    public boolean isPersistKey(){return persistKey;} public void setPersistKey(boolean v){persistKey=v;} public int getMaxOutputTokens(){return maxOutputTokens;} public void setMaxOutputTokens(int v){maxOutputTokens=v;}
    public int getDailyLimit(){return dailyLimit;} public void setDailyLimit(int v){dailyLimit=v;}
}
