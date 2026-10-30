package com.certverify.dto;

import com.certverify.entity.VerificationLog;
import com.certverify.entity.VerificationMethod;
import com.certverify.entity.VerificationResult;

import java.time.Instant;

public record VerificationLogDto(
        Long id,
        String certificateId,
        VerificationMethod method,
        VerificationResult result,
        String message,
        String ipAddress,
        String userAgent,
        Instant verifiedAt
) {
    public static VerificationLogDto from(VerificationLog log) {
        return new VerificationLogDto(
                log.getId(),
                log.getCertificateId(),
                log.getMethod(),
                log.getResult(),
                log.getMessage(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getVerifiedAt()
        );
    }
}
