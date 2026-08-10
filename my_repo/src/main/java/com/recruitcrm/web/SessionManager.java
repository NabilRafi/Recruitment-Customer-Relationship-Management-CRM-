package com.recruitcrm.web;

import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.persistence.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Manages login sessions stored in SQLite. */
public final class SessionManager {
    private static final long SESSION_TTL_MS = 24 * 60 * 60 * 1000L;
    public static final String COOKIE_NAME = "session";

    private SessionManager() {}

    public static String createSession(String email) throws SQLException {
        String token = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + SESSION_TTL_MS;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO sessions (token, email, expires_at) VALUES (?, ?, ?)")) {
            ps.setString(1, token);
            ps.setString(2, email);
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        }
        return token;
    }

    public static Optional<UserAccount> resolveSession(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        purgeExpired();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT email FROM sessions WHERE token = ? AND expires_at > ?")) {
            ps.setString(1, token);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(AuthRepository.loadAccount(rs.getString("email")));
            }
        }
    }

    public static void deleteSession(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return;
        }
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    private static void purgeExpired() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE expires_at <= ?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
}
