package com.recruitcrm.web;

import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.factory.UserAccountFactory;
import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.patterns.singleton.DataStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication routes backed by SQLite sessions.
 *
 * Routes:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/logout
 *   GET  /api/auth/me
 */
public class AuthHandler implements HttpHandler {
    private final DataStore store = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] segments = exchange.getRequestURI().getPath().split("/");
        // "", "api", "auth", "{action}"

        try {
            if (segments.length != 4) {
                RequestUtil.sendError(exchange, 404, "Unknown route");
                return;
            }
            switch (segments[3]) {
                case "register" -> {
                    if (!method.equals("POST")) {
                        RequestUtil.sendError(exchange, 405, "Use POST");
                        return;
                    }
                    register(exchange);
                }
                case "login" -> {
                    if (!method.equals("POST")) {
                        RequestUtil.sendError(exchange, 405, "Use POST");
                        return;
                    }
                    login(exchange);
                }
                case "logout" -> {
                    if (!method.equals("POST")) {
                        RequestUtil.sendError(exchange, 405, "Use POST");
                        return;
                    }
                    logout(exchange);
                }
                case "me" -> {
                    if (!method.equals("GET")) {
                        RequestUtil.sendError(exchange, 405, "Use GET");
                        return;
                    }
                    me(exchange);
                }
                default -> RequestUtil.sendError(exchange, 404, "Unknown route");
            }
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            RequestUtil.sendError(exchange, 500, "Database error: " + e.getMessage());
        }
    }

    private void register(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> form = RequestUtil.parseFormBody(exchange);
        String type = require(form, "type");
        String name = require(form, "name");
        String email = require(form, "email").toLowerCase();
        String password = require(form, "password");
        String extra = form.getOrDefault("extra", "");

        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (AuthRepository.emailExists(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        // A candidate may upload a PDF instead of pasting a link. If a file
        // was sent it wins, and its URL becomes the account's "extra" field.
        String uploadedPdf = form.get("resumeFile");
        if (uploadedPdf != null && !uploadedPdf.isBlank()) {
            extra = ResumeStorage.saveBase64Pdf(uploadedPdf);
        }

        UserAccountFactory factory = UserAccountFactoryRegistry.getInstance().getFactory(type);
        UserAccount account = factory.createAccount(name, email, extra);
        PasswordUtil.HashResult creds = PasswordUtil.hashPassword(password);
        store.saveRegisteredAccount(account, creds.hash(), creds.salt());

        String token = SessionManager.createSession(email);
        RequestUtil.sendJsonWithCookie(exchange, 201,
                userJson(account),
                SessionManager.COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
    }

    private void login(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> form = RequestUtil.parseFormBody(exchange);
        String email = require(form, "email").toLowerCase();
        String password = require(form, "password");

        AuthRepository.AccountCredentials creds = AuthRepository.loadCredentials(email);
        if (creds == null || !PasswordUtil.verify(password, creds.passwordHash(), creds.passwordSalt())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = SessionManager.createSession(email);
        RequestUtil.sendJsonWithCookie(exchange, 200,
                userJson(creds.account()),
                SessionManager.COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400");
    }

    private void logout(HttpExchange exchange) throws IOException, SQLException {
        String token = RequestUtil.getCookie(exchange, SessionManager.COOKIE_NAME);
        SessionManager.deleteSession(token);
        RequestUtil.sendJsonWithCookie(exchange, 200,
                JsonWriter.obj().put("ok", true).toString(),
                SessionManager.COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }

    private void me(HttpExchange exchange) throws IOException {
        Optional<UserAccount> user;
        try {
            user = SessionManager.resolveSession(RequestUtil.getCookie(exchange, SessionManager.COOKIE_NAME));
        } catch (Exception e) {
            RequestUtil.sendError(exchange, 500, "Database error: " + e.getMessage());
            return;
        }
        if (user.isEmpty()) {
            RequestUtil.sendJson(exchange, 200, JsonWriter.obj().put("authenticated", false).toString());
            return;
        }
        RequestUtil.sendJson(exchange, 200, userJson(user.get()));
    }

    private String userJson(UserAccount account) {
        return JsonWriter.obj()
                .put("authenticated", true)
                .put("name", account.getName())
                .put("email", account.getEmail())
                .put("role", account.getRole())
                .toString();
    }

    private String require(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }
}
