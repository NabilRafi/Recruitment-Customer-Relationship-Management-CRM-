package com.recruitcrm.web;

import com.recruitcrm.domain.UserAccount;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

/** Resolves the logged-in user and enforces role checks on protected routes. */
public final class AuthUtil {
    private AuthUtil() {}

    public static Optional<UserAccount> currentUser(HttpExchange exchange) {
        try {
            String token = RequestUtil.getCookie(exchange, SessionManager.COOKIE_NAME);
            return SessionManager.resolveSession(token);
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve session", e);
        }
    }

    public static UserAccount requireUser(HttpExchange exchange) throws IOException {
        return currentUser(exchange).orElseGet(() -> {
            try {
                RequestUtil.sendError(exchange, 401, "Login required");
            } catch (IOException e) {
                // fall through
            }
            throw new IllegalStateException("Unauthorized");
        });
    }

    public static UserAccount requireRole(HttpExchange exchange, String role) throws IOException {
        UserAccount user = requireUser(exchange);
        if (!user.getRole().equals(role)) {
            RequestUtil.sendError(exchange, 403, "Requires " + role + " role");
            throw new IllegalStateException("Forbidden");
        }
        return user;
    }
}
