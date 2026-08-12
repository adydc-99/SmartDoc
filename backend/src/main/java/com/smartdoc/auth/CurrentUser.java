package com.smartdoc.auth;
import javax.servlet.http.HttpServletRequest;
public final class CurrentUser {
    private CurrentUser() {}
    public static AuthPrincipal from(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(AuthFilter.PRINCIPAL_ATTRIBUTE);
    }
}
