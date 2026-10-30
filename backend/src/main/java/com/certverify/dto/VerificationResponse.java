package com.certverify.dto;

import com.certverify.entity.VerificationMethod;
import com.certverify.entity.VerificationResult;

import java.time.Instant;

public record VerificationResponse(
        VerificationResult status,
        String message,
        CertificateDto certificate,
        VerificationMethod method,
        Instant verifiedAt
) {
}
