package com.certverify.repository;

import com.certverify.entity.VerificationLog;
import com.certverify.entity.VerificationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    Page<VerificationLog> findAllByOrderByVerifiedAtDesc(Pageable pageable);

    Page<VerificationLog> findByResultOrderByVerifiedAtDesc(VerificationResult result, Pageable pageable);

    long countByResult(VerificationResult result);

    long countByResultIn(List<VerificationResult> results);

    long countByCertificateId(String certificateId);

    List<VerificationLog> findTop10ByOrderByVerifiedAtDesc();
}
