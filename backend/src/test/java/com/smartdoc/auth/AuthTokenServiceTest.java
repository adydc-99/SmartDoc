package com.smartdoc.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuesAndParsesSignedToken() {
        AuthTokenService service = new AuthTokenService("a-test-secret-with-at-least-32-bytes", 3600, clock);
        String token = service.issue(7L, "demo");
        AuthPrincipal principal = service.parse(token);
        assertEquals(7L, principal.getUserId());
        assertEquals("demo", principal.getUsername());
    }

    @Test
    void rejectsTamperedToken() {
        AuthTokenService service = new AuthTokenService("a-test-secret-with-at-least-32-bytes", 3600, clock);
        String token = service.issue(7L, "demo");
        assertThrows(InvalidTokenException.class, () -> service.parse(token + "x"));
    }

    @Test
    void rejectsExpiredToken() {
        AuthTokenService issuer = new AuthTokenService("a-test-secret-with-at-least-32-bytes", 1, clock);
        String token = issuer.issue(7L, "demo");
        Clock later = Clock.fixed(Instant.parse("2026-08-12T00:00:02Z"), ZoneOffset.UTC);
        AuthTokenService verifier = new AuthTokenService("a-test-secret-with-at-least-32-bytes", 1, later);
        assertThrows(InvalidTokenException.class, () -> verifier.parse(token));
    }
}
