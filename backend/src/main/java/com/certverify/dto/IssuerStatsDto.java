package com.certverify.dto;

public record IssuerStatsDto(
        long totalCertificates,
        long validCertificates,
        long revokedCertificates,
        long expiredCertificates
) {
}
