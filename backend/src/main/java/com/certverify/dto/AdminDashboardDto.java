package com.certverify.dto;

import java.util.List;

public record AdminDashboardDto(
        long totalIssuers,
        long pendingIssuers,
        long approvedIssuers,
        long totalCertificates,
        long totalVerifications,
        long validVerifications,
        long invalidVerifications,
        long revokedCertificates,
        long expiredCertificates,
        List<VerificationLogDto> recentVerifications
) {
}
