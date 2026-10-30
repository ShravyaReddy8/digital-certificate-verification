package com.certverify.repository;

import com.certverify.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateId(String certificateId);

    Optional<Certificate> findFirstByCertificateIdIgnoreCase(String certificateId);

    Optional<Certificate> findByCertificateIdAndIssuerId(String certificateId, Long issuerId);

    Optional<Certificate> findFirstByCertificateIdIgnoreCaseAndIssuerId(String certificateId, Long issuerId);

    Optional<Certificate> findFirstBySha256HashOrderByIssuedAtDesc(String sha256Hash);

    boolean existsByCertificateId(String certificateId);

    List<Certificate> findByIssuerIdOrderByIssuedAtDesc(Long issuerId);

    long countByIssuerId(Long issuerId);

    long countByRevokedTrue();

    long countByIssuerIdAndRevokedTrue(Long issuerId);

    long countByExpiryDateBeforeAndRevokedFalse(LocalDate date);

    long countByIssuerIdAndExpiryDateBeforeAndRevokedFalse(Long issuerId, LocalDate date);

    @Query("""
            select c from Certificate c join c.issuer i
            where (:search is null
                   or lower(c.certificateId) like lower(concat('%', :search, '%'))
                   or lower(c.studentName) like lower(concat('%', :search, '%'))
                   or lower(c.studentEmail) like lower(concat('%', :search, '%'))
                   or lower(c.courseTitle) like lower(concat('%', :search, '%'))
                   or lower(c.institutionName) like lower(concat('%', :search, '%'))
                   or lower(i.email) like lower(concat('%', :search, '%')))
              and (:status is null
                   or (:status = 'REVOKED' and c.revoked = true)
                   or (:status = 'EXPIRED' and c.revoked = false and c.expiryDate < :today)
                   or (:status = 'VALID' and c.revoked = false and (c.expiryDate is null or c.expiryDate >= :today)))
            order by c.issuedAt desc
            """)
    Page<Certificate> searchAll(@Param("search") String search,
                                @Param("status") String status,
                                @Param("today") LocalDate today,
                                Pageable pageable);

    @Query("""
            select c from Certificate c join c.issuer i
            where i.id = :issuerId
              and (:search is null
                   or lower(c.certificateId) like lower(concat('%', :search, '%'))
                   or lower(c.studentName) like lower(concat('%', :search, '%'))
                   or lower(c.studentEmail) like lower(concat('%', :search, '%'))
                   or lower(c.courseTitle) like lower(concat('%', :search, '%')))
              and (:status is null
                   or (:status = 'REVOKED' and c.revoked = true)
                   or (:status = 'EXPIRED' and c.revoked = false and c.expiryDate < :today)
                   or (:status = 'VALID' and c.revoked = false and (c.expiryDate is null or c.expiryDate >= :today)))
            order by c.issuedAt desc
            """)
    Page<Certificate> searchByIssuer(@Param("issuerId") Long issuerId,
                                     @Param("search") String search,
                                     @Param("status") String status,
                                     @Param("today") LocalDate today,
                                     Pageable pageable);
}
