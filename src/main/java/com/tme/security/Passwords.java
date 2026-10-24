package com.tme.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private Passwords() { }

    public static boolean acceptable(String value) {
        return value != null && value.length() >= 12 && value.length() <= 128;
    }

    public static String hash(String password) {
        if (!acceptable(password)) throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        return hashForMigration(password);
    }

    /** Preserves existing short passwords during an explicit offline migration. */
    static String hashForMigration(String password) {
        if (password == null || password.isEmpty() || password.length() > 128) {
            throw new IllegalArgumentException("Contraseña existente inválida; requiere restablecimiento.");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || password.isEmpty() || password.length() > 128 || encoded == null) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256")) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return salt.length == 16 && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static boolean isEncoded(String encoded) {
        if (encoded == null) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256")) return false;
            int iterations = Integer.parseInt(parts[1]);
            return iterations >= ITERATIONS && iterations <= 2_000_000
                    && Base64.getDecoder().decode(parts[2]).length == 16
                    && Base64.getDecoder().decode(parts[3]).length == 32;
        } catch (IllegalArgumentException exception) { return false; }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2 no está disponible.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
