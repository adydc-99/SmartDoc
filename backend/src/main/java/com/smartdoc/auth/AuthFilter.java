package com.smartdoc.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@Component
public class AuthFilter extends OncePerRequestFilter {
    public static final String PRINCIPAL_ATTRIBUTE = "smartdocPrincipal";
    private final AuthTokenService tokens;
    private final ObjectMapper objectMapper;
    public AuthFilter(AuthTokenService tokens, ObjectMapper objectMapper) { this.tokens = tokens; this.objectMapper = objectMapper; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/auth/login") || request.getRequestURI().startsWith("/h2-console");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        try {
            if (header == null || !header.startsWith("Bearer ")) throw new InvalidTokenException("请先登录");
            request.setAttribute(PRINCIPAL_ATTRIBUTE, tokens.parse(header.substring(7)));
            chain.doFilter(request, response);
        } catch (InvalidTokenException e) {
            response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE); response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of("message", e.getMessage()));
        }
    }
}
