package com.certverify.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilTest {

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();
    }

    @Test
    void signsAndVerifiesCertificateHash() {
        String hash = HashUtil.sha256Hex("sample certificate pdf bytes");
        String signature = SignatureUtil.signHash(hash, keyPair.getPrivate());

        assertNotNull(signature);
        assertTrue(SignatureUtil.verifyHash(hash, signature, keyPair.getPublic()));
    }

    @Test
    void verificationFailsWhenHashChanges() {
        String hash = HashUtil.sha256Hex("original certificate");
        String signature = SignatureUtil.signHash(hash, keyPair.getPrivate());

        assertFalse(SignatureUtil.verifyHash(HashUtil.sha256Hex("tampered certificate"),
                signature, keyPair.getPublic()));
    }

    @Test
    void verificationFailsWithWrongPublicKey() {
        String hash = HashUtil.sha256Hex("sample certificate");
        String signature = SignatureUtil.signHash(hash, keyPair.getPrivate());

        assertFalse(SignatureUtil.verifyHash(hash, signature, otherKeyPair.getPublic()));
    }

    @Test
    void verificationFailsForMalformedSignature() {
        String hash = HashUtil.sha256Hex("sample certificate");

        assertFalse(SignatureUtil.verifyHash(hash, "not-a-valid-base64-signature@@", keyPair.getPublic()));
        assertFalse(SignatureUtil.verifyHash(hash, null, keyPair.getPublic()));
        assertFalse(SignatureUtil.verifyHash(hash, "", keyPair.getPublic()));
    }

    @Test
    void sameHashProducesVerifiableSignatureEachTime() {
        String hash = HashUtil.sha256Hex("deterministic test payload");
        String first = SignatureUtil.signHash(hash, keyPair.getPrivate());
        String second = SignatureUtil.signHash(hash, keyPair.getPrivate());

        assertTrue(SignatureUtil.verifyHash(hash, first, keyPair.getPublic()));
        assertTrue(SignatureUtil.verifyHash(hash, second, keyPair.getPublic()));
    }
}
