package com.certverify.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HashUtilTest {

    @Test
    void hashesKnownStringToExpectedSha256() {
        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                HashUtil.sha256Hex("hello"));
    }

    @Test
    void hashesEmptyInputToExpectedSha256() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashUtil.sha256Hex(new byte[0]));
    }

    @Test
    void streamAndByteArrayHashesMatch() throws Exception {
        byte[] payload = "certificate-content".getBytes(StandardCharsets.UTF_8);
        String fromBytes = HashUtil.sha256Hex(payload);
        String fromStream = HashUtil.sha256Hex(new ByteArrayInputStream(payload));

        assertEquals(fromBytes, fromStream);
        assertEquals(64, fromBytes.length());
    }

    @Test
    void differentContentProducesDifferentHash() {
        assertNotEquals(HashUtil.sha256Hex("certificate-a"), HashUtil.sha256Hex("certificate-b"));
    }

    @Test
    void hexRoundTrip() {
        byte[] original = {0x00, 0x0f, 0x10, (byte) 0xff, 0x7a};
        String hex = HashUtil.toHex(original);
        assertEquals("000f10ff7a", hex);
        assertEquals(HashUtil.toHex(original), HashUtil.toHex(HashUtil.fromHex(hex)));
    }

    @Test
    void rejectsNullAndInvalidHex() {
        assertThrows(IllegalArgumentException.class, () -> HashUtil.sha256Hex((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> HashUtil.fromHex("abc"));
        assertThrows(IllegalArgumentException.class, () -> HashUtil.fromHex("zz"));
    }
}
