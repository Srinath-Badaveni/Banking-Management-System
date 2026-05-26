package com.bank.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordUtil - Hashing helpers for PIN storage.
 *
 * PINs are stored as SHA-256 hexadecimal digests.
 * SHA-256 is used here for simplicity; a production system should
 * use bcrypt or Argon2 with salting.
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Returns the SHA-256 hex digest of the input string.
     *
     * @param raw the plain-text PIN
     * @return 64-character lowercase hex string
     * @throws RuntimeException if SHA-256 is unavailable (never happens on standard JRE)
     */
    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java specification — this can never happen
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison to mitigate timing attacks.
     *
     * @param rawPin     the plain-text PIN entered by the user
     * @param storedHash the stored SHA-256 hash
     * @return true when the PIN matches the stored hash
     */
    public static boolean verify(String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null) return false;
        String computed = hash(rawPin);
        // MessageDigest.isEqual performs a constant-time comparison
        return MessageDigest.isEqual(
                computed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
