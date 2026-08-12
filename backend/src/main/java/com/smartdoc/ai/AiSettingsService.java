package com.smartdoc.ai;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class AiSettingsService {
    private final AtomicReference<AiSettings> settings; private final AtomicReference<String> key; private final SecretStore store; private final DailyAiQuota quota;
    public AiSettingsService(AiSettings initial,String initialKey,SecretStore store,DailyAiQuota quota){this.settings=new AtomicReference<>(initial);this.store=store;this.quota=quota;this.key=new AtomicReference<>(store.load().orElse(initialKey==null?"":initialKey));}
    public AiSettings current(){return settings.get();} public Optional<String> currentKey(){return Optional.ofNullable(key.get()).filter(v->!v.isBlank());}
    public AiSettingsView view(){AiSettings s=current();String k=key.get();return new AiSettingsView(s,AiSecretMasker.mask(k),k!=null&&!k.isBlank(),store.isAvailable(),quota.used());}
    public synchronized AiSettingsView update(AiSettingsUpdate u){validate(u);String nextKey=u.getApiKey()==null||u.getApiKey().trim().isEmpty()?key.get():u.getApiKey().trim();
        if(u.isPersistKey()){if(!store.isAvailable())throw new IllegalArgumentException("Secure key persistence is unavailable");if(nextKey==null||nextKey.isBlank())throw new IllegalArgumentException("An API key is required for persistence");store.save(nextKey);} else if(current().isPersistKey()){store.clear();}
        key.set(nextKey==null?"":nextKey);settings.set(new AiSettings(u.getMode(),normalize(u.getBaseUrl()),u.getModel().trim(),u.isPersistKey(),u.getMaxOutputTokens(),u.getDailyLimit()));return view();}
    public synchronized void clearKey(){key.set("");store.clear();AiSettings s=current();settings.set(new AiSettings(s.getMode(),s.getBaseUrl(),s.getModel(),false,s.getMaxOutputTokens(),s.getDailyLimit()));}
    private void validate(AiSettingsUpdate u){if(u==null||u.getMode()==null)throw new IllegalArgumentException("AI mode is required");if(u.getModel()==null||u.getModel().trim().isEmpty()||u.getModel().trim().length()>100)throw new IllegalArgumentException("Model must contain 1-100 characters");if(u.getMaxOutputTokens()<128||u.getMaxOutputTokens()>4096)throw new IllegalArgumentException("maxOutputTokens must be 128-4096");if(u.getDailyLimit()<1||u.getDailyLimit()>500)throw new IllegalArgumentException("dailyLimit must be 1-500");validateUrl(u.getBaseUrl());if(u.isPersistKey()&&!store.isAvailable())throw new IllegalArgumentException("Secure key persistence is unavailable");}
    private static void validateUrl(String raw){try{URI uri=URI.create(raw);String scheme=uri.getScheme(),host=uri.getHost();if(host==null||uri.getUserInfo()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();boolean loop="localhost".equalsIgnoreCase(host)||"127.0.0.1".equals(host);if(!"https".equalsIgnoreCase(scheme)&&!(loop&&"http".equalsIgnoreCase(scheme)))throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("Base URL must use HTTPS (HTTP is allowed only for loopback)");}}
    private static String normalize(String raw){return raw.replaceAll("/+$","");}
}
