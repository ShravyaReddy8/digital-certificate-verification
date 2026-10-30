package com.certverify.service;

import com.certverify.dto.CertificateDto;
import com.certverify.dto.VerificationResponse;
import com.certverify.entity.Certificate;
import com.certverify.entity.VerificationLog;
import com.certverify.entity.VerificationMethod;
import com.certverify.entity.VerificationResult;
import com.certverify.exception.BadRequestException;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.VerificationLogRepository;
import com.certverify.util.HashUtil;
import com.certverify.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final CertificateRepository certificateRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final CryptoKeyService cryptoKeyService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public VerificationResponse verifyByFile(MultipartFile file, ClientInfo client) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please upload a certificate file to verify.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Unable to read the uploaded file.");
        }
        if (bytes.length == 0) {
            throw new BadRequestException("The uploaded file is empty.");
        }

        String hash = HashUtil.sha256Hex(bytes);
        Certificate certificate = certificateRepository
                .findFirstBySha256HashOrderByIssuedAtDesc(hash)
                .orElse(null);

        if (certificate == null) {
            String message = "TAMPERED / NOT AUTHENTIC: no issued certificate matches this file. "
                    + "It may have been modified, re-saved or was never issued by this system.";
            return logAndRespond(null, VerificationMethod.FILE, VerificationResult.TAMPERED,
                    message, client);
        }

        VerificationResult integrity = checkIntegrity(certificate);
        if (integrity == VerificationResult.TAMPERED) {
            return logAndRespond(certificate, VerificationMethod.FILE, VerificationResult.TAMPERED,
                    "Certificate integrity check failed. The file or its digital signature has been altered.",
                    client);
        }

        return evaluateStatus(certificate, VerificationMethod.FILE, client);
    }

    @Transactional
    public VerificationResponse verifyByCertificateId(String certificateId, VerificationMethod method,
                                                      ClientInfo client) {
        if (certificateId == null || certificateId.isBlank()) {
            throw new BadRequestException("Certificate ID is required.");
        }
        Certificate certificate = certificateRepository
                .findFirstByCertificateIdIgnoreCase(certificateId.trim())
                .orElse(null);

        if (certificate == null) {
            return logAndRespond(null, method, VerificationResult.NOT_FOUND,
                    "No certificate found with ID " + certificateId.trim().toUpperCase(Locale.ROOT) + ".",
                    client);
        }

        VerificationResult integrity = checkIntegrity(certificate);
        if (integrity == VerificationResult.TAMPERED) {
            return logAndRespond(certificate, method, VerificationResult.TAMPERED,
                    "Certificate integrity check failed. The stored file or its digital signature has been altered.",
                    client);
        }

        return evaluateStatus(certificate, method, client);
    }

    private VerificationResult checkIntegrity(Certificate certificate) {
        boolean signatureValid = SignatureUtil.verifyHash(
                certificate.getSha256Hash(),
                certificate.getDigitalSignature(),
                cryptoKeyService.getPublicKey());
        if (!signatureValid) {
            return VerificationResult.TAMPERED;
        }
        try {
            Path storedFile = Paths.get(certificate.getFilePath());
            if (!Files.exists(storedFile)) {
                return VerificationResult.TAMPERED;
            }
            String storedHash = HashUtil.sha256Hex(Files.readAllBytes(storedFile));
            if (!storedHash.equalsIgnoreCase(certificate.getSha256Hash())) {
                return VerificationResult.TAMPERED;
            }
        } catch (IOException e) {
            return VerificationResult.TAMPERED;
        }
        return VerificationResult.VALID;
    }

    private VerificationResponse evaluateStatus(Certificate certificate, VerificationMethod method,
                                                ClientInfo client) {
        if (certificate.isRevoked()) {
            String reason = certificate.getRevocationReason() == null
                    ? "" : " Reason: " + certificate.getRevocationReason();
            return logAndRespond(certificate, method, VerificationResult.REVOKED,
                    "This certificate has been revoked by the issuing institution." + reason, client);
        }

        if (certificate.getExpiryDate() != null && certificate.getExpiryDate().isBefore(LocalDate.now())) {
            String formatted = certificate.getExpiryDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            return logAndRespond(certificate, method, VerificationResult.EXPIRED,
                    "This certificate is authentic but expired on " + formatted + ".", client);
        }

        return logAndRespond(certificate, method, VerificationResult.VALID,
                "Certificate is authentic, digitally signed and currently valid.", client);
    }

    private VerificationResponse logAndRespond(Certificate certificate, VerificationMethod method,
                                               VerificationResult result, String message, ClientInfo client) {
        Instant now = Instant.now();
        VerificationLog log = VerificationLog.builder()
                .certificateId(certificate != null ? certificate.getCertificateId() : null)
                .certificate(certificate)
                .method(method)
                .result(result)
                .message(truncate(message, 400))
                .ipAddress(truncate(client.ipAddress(), 80))
                .userAgent(truncate(client.userAgent(), 300))
                .verifiedAt(now)
                .build();
        verificationLogRepository.save(log);

        CertificateDto dto = certificate != null ? CertificateDto.from(certificate, frontendUrl) : null;
        return new VerificationResponse(result, message, dto, method, now);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ClientInfo(String ipAddress, String userAgent) {
    }
}
