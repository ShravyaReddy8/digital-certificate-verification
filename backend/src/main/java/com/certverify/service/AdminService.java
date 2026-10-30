package com.certverify.service;

import com.certverify.dto.AdminDashboardDto;
import com.certverify.dto.IssuerStatusRequest;
import com.certverify.dto.PageResponse;
import com.certverify.dto.UserDto;
import com.certverify.dto.VerificationLogDto;
import com.certverify.entity.Role;
import com.certverify.entity.User;
import com.certverify.entity.UserStatus;
import com.certverify.entity.VerificationLog;
import com.certverify.entity.VerificationResult;
import com.certverify.exception.BadRequestException;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.UserRepository;
import com.certverify.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final VerificationLogRepository verificationLogRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard() {
        long totalIssuers = userRepository.countByRole(Role.ISSUER);
        long pendingIssuers = userRepository.countByRoleAndStatus(Role.ISSUER, UserStatus.PENDING);
        long approvedIssuers = userRepository.countByRoleAndStatus(Role.ISSUER, UserStatus.APPROVED);
        long totalCertificates = certificateRepository.count();
        long totalVerifications = verificationLogRepository.count();
        long validVerifications = verificationLogRepository.countByResult(VerificationResult.VALID);
        long revokedCertificates = certificateRepository.countByRevokedTrue();
        long expiredCertificates = certificateRepository.countByExpiryDateBeforeAndRevokedFalse(LocalDate.now());

        return new AdminDashboardDto(
                totalIssuers,
                pendingIssuers,
                approvedIssuers,
                totalCertificates,
                totalVerifications,
                validVerifications,
                Math.max(0, totalVerifications - validVerifications),
                revokedCertificates,
                expiredCertificates,
                verificationLogRepository.findTop10ByOrderByVerifiedAtDesc()
                        .stream().map(VerificationLogDto::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listIssuers(String status, int page, int size) {
        UserStatus filter = parseStatus(status);
        Pageable pageable = pageRequest(page, size);
        Page<User> issuers = userRepository.findByRoleAndOptionalStatus(Role.ISSUER, filter, pageable);
        return PageResponse.from(issuers, UserDto::from);
    }

    @Transactional
    public UserDto updateIssuerStatus(Long issuerId, IssuerStatusRequest request) {
        User issuer = userRepository.findById(issuerId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer account not found."));
        if (issuer.getRole() != Role.ISSUER) {
            throw new BadRequestException("Only issuer accounts can be approved or rejected.");
        }
        if (request.status() == UserStatus.PENDING) {
            throw new BadRequestException("Status must be APPROVED or REJECTED.");
        }
        if (request.status() == UserStatus.REJECTED
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BadRequestException("Please provide a reason when rejecting an issuer.");
        }

        issuer.setStatus(request.status());
        if (request.status() == UserStatus.REJECTED) {
            issuer.setRejectionReason(request.reason().trim());
        } else {
            issuer.setRejectionReason(null);
        }
        return UserDto.from(userRepository.save(issuer));
    }

    @Transactional(readOnly = true)
    public PageResponse<VerificationLogDto> listLogs(String result, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Page<VerificationLog> logs;
        if (result == null || result.isBlank() || result.equalsIgnoreCase("ALL")) {
            logs = verificationLogRepository.findAllByOrderByVerifiedAtDesc(pageable);
        } else {
            VerificationResult parsed;
            try {
                parsed = VerificationResult.valueOf(result.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Result filter must be one of VALID, TAMPERED, REVOKED, EXPIRED or NOT_FOUND.");
            }
            logs = verificationLogRepository.findByResultOrderByVerifiedAtDesc(parsed, pageable);
        }
        return PageResponse.from(logs, VerificationLogDto::from);
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Status filter must be PENDING, APPROVED or REJECTED.");
        }
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    }
}
