package com.certverify.config;

import com.certverify.dto.CertificateDto;
import com.certverify.dto.CertificateRequest;
import com.certverify.entity.Role;
import com.certverify.entity.User;
import com.certverify.entity.UserStatus;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.UserRepository;
import com.certverify.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Creates the default administrator, a demo issuer account and two sample
 * certificates on first startup so the app is immediately usable.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@cert.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ISSUER_EMAIL = "issuer@cert.com";
    private static final String ISSUER_PASSWORD = "Issuer@123";
    private static final String DEMO_INSTITUTION = "Springfield Institute of Technology";

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        User demoIssuer = seedDemoIssuer();
        seedDemoCertificates(demoIssuer);
    }

    private void seedAdmin() {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }
        User admin = User.builder()
                .name("System Administrator")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .status(UserStatus.APPROVED)
                .institutionName("Certificate Authority")
                .createdAt(Instant.now())
                .build();
        userRepository.save(admin);
        log.info("Seeded default administrator account: {}", ADMIN_EMAIL);
    }

    private User seedDemoIssuer() {
        return userRepository.findByEmailIgnoreCase(ISSUER_EMAIL).orElseGet(() -> {
            User issuer = User.builder()
                    .name("Dr. Sarah Mitchell")
                    .email(ISSUER_EMAIL)
                    .password(passwordEncoder.encode(ISSUER_PASSWORD))
                    .role(Role.ISSUER)
                    .status(UserStatus.APPROVED)
                    .institutionName(DEMO_INSTITUTION)
                    .createdAt(Instant.now())
                    .build();
            User saved = userRepository.save(issuer);
            log.info("Seeded demo issuer account: {}", ISSUER_EMAIL);
            return saved;
        });
    }

    private void seedDemoCertificates(User demoIssuer) {
        if (demoIssuer == null || certificateRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        CertificateRequest first = new CertificateRequest(
                "Alice Johnson",
                "alice.johnson@example.com",
                "B.Tech in Computer Science",
                DEMO_INSTITUTION,
                today.minusMonths(8),
                today.plusYears(2),
                "A+");

        CertificateRequest second = new CertificateRequest(
                "Michael Chen",
                "michael.chen@example.com",
                "Advanced Data Science & Machine Learning",
                DEMO_INSTITUTION,
                today.minusMonths(3),
                today.plusYears(1),
                "A");

        CertificateDto firstDto = certificateService.issueGenerated(first, demoIssuer, "CERT-2026-DEMO01");
        CertificateDto secondDto = certificateService.issueGenerated(second, demoIssuer, "CERT-2026-DEMO02");

        log.info("Seeded demo certificates: {} ({}) and {} ({})",
                firstDto.certificateId(), firstDto.studentName(),
                secondDto.certificateId(), secondDto.studentName());
    }
}
