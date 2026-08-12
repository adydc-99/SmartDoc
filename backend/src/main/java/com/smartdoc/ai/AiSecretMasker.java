package com.smartdoc.ai;

public final class AiSecretMasker {
    private AiSecretMasker() {}

    public static String mask(String key) {
        if (key == null || key.isEmpty()) return "";
        String suffix = key.substring(Math.max(0, key.length() - 4));
        return key.startsWith("sk-") && key.length() > 7 ? "sk-****" + suffix : "****" + suffix;
    }
}
