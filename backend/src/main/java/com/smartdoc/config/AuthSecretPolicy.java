package com.smartdoc.config;
public final class AuthSecretPolicy { private AuthSecretPolicy(){} public static String requireValid(String secret,String[] profiles){if(secret==null||secret.length()<32)throw new IllegalStateException("SMARTDOC_AUTH_SECRET must contain at least 32 characters");return secret;} }
