package com.certverify.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads (or creates on first startup) the RSA key pair used to sign certificate
 * hashes. Keys are stored under {@code <upload-dir>/keys}.
 */
@Service
public class CryptoKeyService {

    private static final Logger log = LoggerFactory.getLogger(CryptoKeyService.class);

    private final Path keysDirectory;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public CryptoKeyService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.keysDirectory = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("keys");
    }

    @PostConstruct
    public void initializeKeys() {
        try {
            Files.createDirectories(keysDirectory);
            Path privateKeyFile = keysDirectory.resolve("private.key");
            Path publicKeyFile = keysDirectory.resolve("public.key");

            if (Files.exists(privateKeyFile) && Files.exists(publicKeyFile)) {
                privateKey = loadPrivateKey(privateKeyFile);
                publicKey = loadPublicKey(publicKeyFile);
                log.info("Loaded existing RSA signing key pair from {}", keysDirectory);
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                privateKey = keyPair.getPrivate();
                publicKey = keyPair.getPublic();

                Files.writeString(privateKeyFile, Base64.getEncoder().encodeToString(privateKey.getEncoded()));
                Files.writeString(publicKeyFile, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
                log.info("Generated new RSA signing key pair at {}", keysDirectory);
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to initialize RSA signing keys", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PrivateKey loadPrivateKey(Path file) throws IOException, GeneralSecurityException {
        byte[] decoded = Base64.getMimeDecoder().decode(Files.readString(file).trim());
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(Path file) throws IOException, GeneralSecurityException {
        byte[] decoded = Base64.getMimeDecoder().decode(Files.readString(file).trim());
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }
}
