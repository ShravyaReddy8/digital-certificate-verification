package com.certverify.dto;

public record PublicStatsDto(
        long totalCertificates,
        long totalVerifications,
        long totalIssuers
) {
}
