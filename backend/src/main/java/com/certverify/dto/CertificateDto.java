package com.certverify.dto;

import com.certverify.entity.Certificate;

import java.time.Instant;
import java.time.LocalDate;

public record CertificateDto(
        Long id,
        String certificateId,
        String studentName,
        String studentEmail,
        String courseTitle,
        String institutionName,
        LocalDate issueDate,
        LocalDate expiryDate,
        String grade,
        String fileName,
        String fileType,
        Long fileSize,
        String sha256Hash,
        String status,
        boolean revoked,
        String revocationReason,
        Instant revokedAt,
        Instant issuedAt,
        String issuerName,
        String issuerEmail,
        String verificationUrl,
        String qrCodeUrl,
        String downloadUrl
) {
    public static CertificateDto from(Certificate certificate, String frontendUrl) {
        String status = certificate.isRevoked()
                ? "REVOKED"
                : (certificate.getExpiryDate() != null && certificate.getExpiryDate().isBefore(LocalDate.now())
                ? "EXPIRED" : "VALID");

        String base = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;

        return new CertificateDto(
                certificate.getId(),
                certificate.getCertificateId(),
                certificate.getStudentName(),
                certificate.getStudentEmail(),
                certificate.getCourseTitle(),
                certificate.getInstitutionName(),
                certificate.getIssueDate(),
                certificate.getExpiryDate(),
                certificate.getGrade(),
                certificate.getFileName(),
                certificate.getFileType(),
                certificate.getFileSize(),
                certificate.getSha256Hash(),
                status,
                certificate.isRevoked(),
                certificate.getRevocationReason(),
                certificate.getRevokedAt(),
                certificate.getIssuedAt(),
                certificate.getIssuer() != null ? certificate.getIssuer().getName() : null,
                certificate.getIssuer() != null ? certificate.getIssuer().getEmail() : null,
                base + "/verify/" + certificate.getCertificateId(),
                "/api/verify/" + certificate.getCertificateId() + "/qr",
                "/api/issuer/certificates/" + certificate.getCertificateId() + "/download"
        );
    }
}
