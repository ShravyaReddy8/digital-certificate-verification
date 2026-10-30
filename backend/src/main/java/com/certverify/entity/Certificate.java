package com.certverify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_certificate_cert_id", columnList = "certificateId", unique = true),
        @Index(name = "idx_certificate_hash", columnList = "sha256Hash")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String certificateId;

    @Column(nullable = false, length = 160)
    private String studentName;

    @Column(length = 160)
    private String studentEmail;

    @Column(nullable = false, length = 200)
    private String courseTitle;

    @Column(nullable = false, length = 200)
    private String institutionName;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Column(length = 100)
    private String grade;

    @Column(nullable = false)
    private String fileName;

    private String storedFileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(length = 120)
    private String fileType;

    private Long fileSize;

    @Column(nullable = false, length = 64)
    private String sha256Hash;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(length = 500)
    private String qrPath;

    @Column(nullable = false)
    private boolean revoked;

    @Column(length = 500)
    private String revocationReason;

    private Instant revokedAt;

    @Column(nullable = false)
    private Instant issuedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;
}
