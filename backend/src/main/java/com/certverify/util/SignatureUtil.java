package com.certverify.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * RSA digital signatures over SHA-256 certificate digests (SHA256withRSA).
 * The signed payload is the UTF-8 representation of the hexadecimal digest.
 */
public final class SignatureUtil {

    public static final String ALGORITHM = "SHA256withRSA";

    private SignatureUtil() {
    }

    public static String signHash(String sha256Hex, PrivateKey privateKey) {
        if (sha256Hex == null || privateKey == null) {
            throw new IllegalArgumentException("Hash and private key are required");
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(sha256Hex.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign the certificate hash", e);
        }
    }

    public static boolean verifyHash(String sha256Hex, String signatureBase64, PublicKey publicKey) {
        if (sha256Hex == null || signatureBase64 == null || publicKey == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(sha256Hex.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
