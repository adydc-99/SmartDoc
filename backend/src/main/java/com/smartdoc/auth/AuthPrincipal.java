package com.smartdoc.auth;

public class AuthPrincipal {
    private final long userId;
    private final String username;

    public AuthPrincipal(long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public long getUserId() { return userId; }
    public String getUsername() { return username; }
}
