package com.certverify.service;

import com.certverify.dto.CertificateDto;
import com.certverify.dto.CertificateRequest;
import com.certverify.dto.IssuerStatsDto;
import com.certverify.dto.PageResponse;
import com.certverify.entity.Certificate;
import com.certverify.entity.User;
import com.certverify.exception.BadRequestException;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.UserRepository;
import com.certverify.util.HashUtil;
import com.certverify.util.PdfGenerator;
import com.certverify.util.QrCodeUtil;
import com.certverify.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final char[] ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CryptoKeyService cryptoKeyService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ------------------------------------------------------------------
    // Issuing
    // ------------------------------------------------------------------

    @Transactional
    public CertificateDto issueGenerated(CertificateRequest request, User principal) {
        return issueGenerated(request, principal, null);
    }

    /**
     * Issues an auto-generated certificate PDF. When {@code preferredId} is
     * supplied (seed data) it is used, otherwise a unique ID is generated.
     */
    @Transactional
    public CertificateDto issueGenerated(CertificateRequest request, User principal, String preferredId) {
        User issuer = requireIssuer(principal);
        validateRequest(request);

        String certificateId = resolveCertificateId(preferredId);
        String verifyUrl = buildVerifyUrl(certificateId);
        byte[] qrPng = QrCodeUtil.generatePng(verifyUrl, 320);

        byte[] pdfBytes;
        try {
            pdfBytes = PdfGenerator.generateCertificatePdf(
                    request.institutionName(),
                    request.studentName(),
                    request.courseTitle(),
                    request.grade(),
                    request.issueDate(),
                    request.expiryDate(),
                    certificateId,
                    issuer.getName(),
                    qrPng,
                    verifyUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate the certificate PDF", e);
        }

        FileStorageService.StoredFile storedPdf = fileStorageService.storeBytes(
                pdfBytes, "pdf", "certificates", certificateId);
        FileStorageService.StoredFile storedQr = fileStorageService.storeQrCode(certificateId, qrPng);

        String hash = HashUtil.sha256Hex(pdfBytes);
        String signature = SignatureUtil.signHash(hash, cryptoKeyService.getPrivateKey());

        Certificate certificate = Certificate.builder()
                .certificateId(certificateId)
                .studentName(request.studentName().trim())
                .studentEmail(trimToNull(request.studentEmail()))
                .courseTitle(request.courseTitle().trim())
                .institutionName(request.institutionName().trim())
                .issueDate(request.issueDate())
                .expiryDate(request.expiryDate())
                .grade(trimToNull(request.grade()))
                .fileName("certificate-" + certificateId + ".pdf")
                .storedFileName(storedPdf.storedFileName())
                .filePath(storedPdf.path().toString())
                .fileType("application/pdf")
                .fileSize(storedPdf.size())
                .sha256Hash(hash)
                .digitalSignature(signature)
                .qrPath(storedQr.path().toString())
                .revoked(false)
                .issuedAt(Instant.now())
                .issuer(issuer)
                .build();

        return CertificateDto.from(certificateRepository.save(certificate), frontendUrl);
    }

    @Transactional
    public CertificateDto issueUploaded(CertificateRequest request, MultipartFile file, User principal) {
        User issuer = requireIssuer(principal);
        validateRequest(request);
        FileStorageService.StoredFile stored = fileStorageService.storeCertificate(file);

        String certificateId = resolveCertificateId(null);
        byte[] fileBytes = fileStorageService.readBytes(stored.path());
        String hash = HashUtil.sha256Hex(fileBytes);
        String signature = SignatureUtil.signHash(hash, cryptoKeyService.getPrivateKey());

        String verifyUrl = buildVerifyUrl(certificateId);
        byte[] qrPng = QrCodeUtil.generatePng(verifyUrl, 320);
        FileStorageService.StoredFile storedQr = fileStorageService.storeQrCode(certificateId, qrPng);

        Certificate certificate = Certificate.builder()
                .certificateId(certificateId)
                .studentName(request.studentName().trim())
                .studentEmail(trimToNull(request.studentEmail()))
                .courseTitle(request.courseTitle().trim())
                .institutionName(request.institutionName().trim())
                .issueDate(request.issueDate())
                .expiryDate(request.expiryDate())
                .grade(trimToNull(request.grade()))
                .fileName(stored.originalFileName())
                .storedFileName(stored.storedFileName())
                .filePath(stored.path().toString())
                .fileType(stored.contentType())
                .fileSize(stored.size())
                .sha256Hash(hash)
                .digitalSignature(signature)
                .qrPath(storedQr.path().toString())
                .revoked(false)
                .issuedAt(Instant.now())
                .issuer(issuer)
                .build();

        return CertificateDto.from(certificateRepository.save(certificate), frontendUrl);
    }

    // ------------------------------------------------------------------
    // Issuer queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CertificateDto> listIssuerCertificates(User principal, String search,
                                                               String status, int page, int size) {
        User issuer = requireIssuer(principal);
        Pageable pageable = pageRequest(page, size);
        Page<Certificate> certificates = certificateRepository.searchByIssuer(
                issuer.getId(), normalizeSearch(search), normalizeStatus(status), LocalDate.now(), pageable);
        return PageResponse.from(certificates, certificate -> CertificateDto.from(certificate, frontendUrl));
    }

    @Transactional(readOnly = true)
    public CertificateDto getIssuerCertificate(String certificateId, User principal) {
        User issuer = requireIssuer(principal);
        Certificate certificate = certificateRepository
                .findFirstByCertificateIdIgnoreCaseAndIssuerId(certificateId.trim(), issuer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));
        return CertificateDto.from(certificate, frontendUrl);
    }

    @Transactional(readOnly = true)
    public IssuerStatsDto issuerStats(User principal) {
        User issuer = requireIssuer(principal);
        long total = certificateRepository.countByIssuerId(issuer.getId());
        long revoked = certificateRepository.countByIssuerIdAndRevokedTrue(issuer.getId());
        long expired = certificateRepository.countByIssuerIdAndExpiryDateBeforeAndRevokedFalse(
                issuer.getId(), LocalDate.now());
        long valid = Math.max(0, total - revoked - expired);
        return new IssuerStatsDto(total, valid, revoked, expired);
    }

    // ------------------------------------------------------------------
    // Revocation
    // ------------------------------------------------------------------

    @Transactional
    public CertificateDto revokeCertificate(String certificateId, String reason, User principal) {
        User issuer = requireIssuer(principal);
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A revocation reason is required.");
        }
        Certificate certificate = certificateRepository
                .findFirstByCertificateIdIgnoreCaseAndIssuerId(certificateId.trim(), issuer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));
        if (certificate.isRevoked()) {
            throw new BadRequestException("This certificate has already been revoked.");
        }
        certificate.setRevoked(true);
        certificate.setRevocationReason(reason.trim());
        certificate.setRevokedAt(Instant.now());
        return CertificateDto.from(certificateRepository.save(certificate), frontendUrl);
    }

    // ------------------------------------------------------------------
    // Files (download / QR)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DownloadFile downloadCertificate(String certificateId, User principal) {
        User issuer = requireIssuer(principal);
        Certificate certificate = certificateRepository
                .findFirstByCertificateIdIgnoreCaseAndIssuerId(certificateId.trim(), issuer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));

        Path path = Paths.get(certificate.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("The stored certificate file could not be found.");
        }
        String contentType = certificate.getFileType() != null ? certificate.getFileType() : "application/octet-stream";
        return new DownloadFile(fileStorageService.readBytes(path), certificate.getFileName(), contentType);
    }

    @Transactional
    public byte[] qrCodeForCertificate(String certificateId) {
        Certificate certificate = certificateRepository
                .findFirstByCertificateIdIgnoreCase(certificateId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));

        if (certificate.getQrPath() != null) {
            Path qrPath = Paths.get(certificate.getQrPath());
            if (Files.exists(qrPath)) {
                return fileStorageService.readBytes(qrPath);
            }
        }
        byte[] qrPng = QrCodeUtil.generatePng(buildVerifyUrl(certificate.getCertificateId()), 320);
        FileStorageService.StoredFile stored = fileStorageService.storeQrCode(certificate.getCertificateId(), qrPng);
        certificate.setQrPath(stored.path().toString());
        certificateRepository.save(certificate);
        return qrPng;
    }

    // ------------------------------------------------------------------
    // Admin queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CertificateDto> listAllCertificates(String search, String status, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Page<Certificate> certificates = certificateRepository.searchAll(
                normalizeSearch(search), normalizeStatus(status), LocalDate.now(), pageable);
        return PageResponse.from(certificates, certificate -> CertificateDto.from(certificate, frontendUrl));
    }

    @Transactional(readOnly = true)
    public Certificate findCertificateEntity(String certificateId) {
        return certificateRepository.findFirstByCertificateIdIgnoreCase(certificateId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found."));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User requireIssuer(User principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("Authenticated issuer not found.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Issuer account not found."));
    }

    private void validateRequest(CertificateRequest request) {
        if (request.issueDate() == null) {
            throw new BadRequestException("Issue date is required.");
        }
        if (request.expiryDate() != null && request.expiryDate().isBefore(request.issueDate())) {
            throw new BadRequestException("Expiry date must be on or after the issue date.");
        }
    }

    private String resolveCertificateId(String preferredId) {
        if (preferredId != null && !certificateRepository.existsByCertificateId(preferredId)) {
            return preferredId;
        }
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "CERT-" + LocalDate.now().getYear() + "-" + randomToken(6);
            if (!certificateRepository.existsByCertificateId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique certificate ID. Please retry.");
    }

    private String randomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ID_ALPHABET[RANDOM.nextInt(ID_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private String buildVerifyUrl(String certificateId) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/verify/" + certificateId;
    }

    private Pageable pageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return PageRequest.of(safePage, safeSize);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("VALID") && !normalized.equals("REVOKED") && !normalized.equals("EXPIRED")) {
            throw new BadRequestException("Status filter must be one of VALID, REVOKED or EXPIRED.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record DownloadFile(byte[] content, String fileName, String contentType) {
    }
}
