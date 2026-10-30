package com.certverify.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing SHA-256 digests in hexadecimal form.
 */
public final class HashUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HashUtil() {
    }

    public static String sha256Hex(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public static String sha256Hex(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text must not be null");
        }
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX[(b >> 4) & 0x0F]);
            sb.append(HEX[b & 0x0F]);
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hexadecimal character");
            }
            result[i / 2] = (byte) ((high << 4) | low);
        }
        return result;
    }
}
