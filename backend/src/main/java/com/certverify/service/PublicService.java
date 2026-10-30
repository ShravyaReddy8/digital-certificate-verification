package com.certverify.service;

import com.certverify.dto.PublicStatsDto;
import com.certverify.entity.Role;
import com.certverify.entity.UserStatus;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.UserRepository;
import com.certverify.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicService {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final VerificationLogRepository verificationLogRepository;

    @Transactional(readOnly = true)
    public PublicStatsDto stats() {
        return new PublicStatsDto(
                certificateRepository.count(),
                verificationLogRepository.count(),
                userRepository.countByRoleAndStatus(Role.ISSUER, UserStatus.APPROVED));
    }
}
