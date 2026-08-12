package com.smartdoc.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

public class AuthTokenService {
    private final byte[] secret;
    private final long ttlSeconds;
    private final Clock clock;

    public AuthTokenService(String secret, long ttlSeconds, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    public String issue(long userId, String username) {
        long expiresAt = clock.instant().getEpochSecond() + ttlSeconds;
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (userId + "|" + username + "|" + expiresAt).getBytes(StandardCharsets.UTF_8));
        return payload + "." + sign(payload);
    }

    public AuthPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]).getBytes(StandardCharsets.UTF_8),
                    parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw new InvalidTokenException("令牌无效");
            }
            String[] fields = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split("\\|", -1);
            if (fields.length != 3 || Long.parseLong(fields[2]) <= clock.instant().getEpochSecond()) {
                throw new InvalidTokenException("令牌已过期");
            }
            return new AuthPrincipal(Long.parseLong(fields[0]), fields[1]);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("令牌无效");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法签发令牌", e);
        }
    }
}
