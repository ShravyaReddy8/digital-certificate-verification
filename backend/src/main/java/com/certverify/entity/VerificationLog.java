package com.certverify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "verification_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String certificateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationResult result;

    @Column(length = 400)
    private String message;

    @Column(length = 80)
    private String ipAddress;

    @Column(length = 300)
    private String userAgent;

    @Column(nullable = false)
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_ref_id")
    private Certificate certificate;
}
