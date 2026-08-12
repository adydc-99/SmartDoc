package com.smartdoc.ai;

public final class AiSettingsView {
    private final AiMode mode; private final String baseUrl,model,maskedKey; private final boolean keyConfigured,persistenceAvailable,persistKey; private final int maxOutputTokens,dailyLimit,todayUsed;
    public AiSettingsView(AiSettings s,String maskedKey,boolean keyConfigured,boolean persistenceAvailable,int todayUsed){this.mode=s.getMode();this.baseUrl=s.getBaseUrl();this.model=s.getModel();this.maskedKey=maskedKey;this.keyConfigured=keyConfigured;this.persistenceAvailable=persistenceAvailable;this.persistKey=s.isPersistKey();this.maxOutputTokens=s.getMaxOutputTokens();this.dailyLimit=s.getDailyLimit();this.todayUsed=todayUsed;}
    public AiMode getMode(){return mode;} public String getBaseUrl(){return baseUrl;} public String getModel(){return model;} public String getMaskedKey(){return maskedKey;}
    public boolean isKeyConfigured(){return keyConfigured;} public boolean isPersistenceAvailable(){return persistenceAvailable;} public boolean isPersistKey(){return persistKey;}
    public int getMaxOutputTokens(){return maxOutputTokens;} public int getDailyLimit(){return dailyLimit;} public int getTodayUsed(){return todayUsed;}
}
