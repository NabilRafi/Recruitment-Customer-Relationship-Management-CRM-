package com.recruitcrm.web;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/** PBKDF2 password hashing — no external crypto libraries needed. */
public final class PasswordUtil {
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {}

    public record HashResult(String hash, String salt) {}

    public static HashResult hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        String hash = pbkdf2(password, salt);
        return new HashResult(hash, Base64.getEncoder().encodeToString(salt));
    }

    public static boolean verify(String password, String storedHash, String storedSalt) {
        byte[] salt = Base64.getDecoder().decode(storedSalt);
        return constantTimeEquals(pbkdf2(password, salt), storedHash);
    }

    private static String pbkdf2(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
