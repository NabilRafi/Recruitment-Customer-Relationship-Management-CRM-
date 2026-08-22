package com.recruitcrm.web;

import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.factory.UserAccountFactory;
import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.persistence.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public final class AuthRepository {
    private AuthRepository() {}

    public static UserAccount loadAccount(String email) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT role, name, extra FROM accounts WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return toAccount(email, rs.getString("role"), rs.getString("name"), rs.getString("extra"));
            }
        }
    }

    public static AccountCredentials loadCredentials(String email) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT role, name, extra, password_hash, password_salt FROM accounts WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UserAccount account = toAccount(email, rs.getString("role"), rs.getString("name"), rs.getString("extra"));
                return new AccountCredentials(account, rs.getString("password_hash"), rs.getString("password_salt"));
            }
        }
    }

    public static void saveAccount(UserAccount account, String passwordHash, String passwordSalt) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO accounts (email, role, name, password_hash, password_salt, extra)
                 VALUES (?, ?, ?, ?, ?, ?)
                 ON CONFLICT(email) DO UPDATE SET
                   role = excluded.role,
                   name = excluded.name,
                   password_hash = excluded.password_hash,
                   password_salt = excluded.password_salt,
                   extra = excluded.extra
                 """)) {
            ps.setString(1, account.getEmail());
            ps.setString(2, account.getRole());
            ps.setString(3, account.getName());
            ps.setString(4, passwordHash);
            ps.setString(5, passwordSalt);
            ps.setString(6, extraFor(account));
            ps.executeUpdate();
        }
    }

    public static boolean emailExists(String email) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM accounts WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public record AccountCredentials(UserAccount account, String passwordHash, String passwordSalt) {}

    
    private static UserAccount toAccount(String email, String role, String name, String extra) {
        String safeExtra = extra == null ? "" : extra;
        UserAccountFactory factory = UserAccountFactoryRegistry.getInstance().getFactory(role);
        return factory.createAccount(name, email, safeExtra);
    }

    
    private static String extraFor(UserAccount account) {
        return account.getExtra();
    }
}
