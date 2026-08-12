package com.smartdoc.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthTokenService tokens;
    public AuthController(AuthTokenService tokens) { this.tokens = tokens; }
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        if (!"demo".equals(request.username) || !"smartdoc123".equals(request.password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return Map.of("token", tokens.issue(1L, "demo"), "username", "demo");
    }
    public static class LoginRequest {
        @NotBlank public String username; @NotBlank public String password;
    }
}
