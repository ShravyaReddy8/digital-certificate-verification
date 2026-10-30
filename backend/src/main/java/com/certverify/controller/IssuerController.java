package com.certverify.controller;

import com.certverify.dto.CertificateDto;
import com.certverify.dto.CertificateRequest;
import com.certverify.dto.CertificateUploadForm;
import com.certverify.dto.IssuerStatsDto;
import com.certverify.dto.PageResponse;
import com.certverify.dto.RevokeRequest;
import com.certverify.entity.User;
import com.certverify.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/issuer")
@RequiredArgsConstructor
public class IssuerController {

    private final CertificateService certificateService;

    @PostMapping("/certificates")
    public ResponseEntity<CertificateDto> issueGenerated(
            @Valid @RequestBody CertificateRequest request,
            @AuthenticationPrincipal User issuer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.issueGenerated(request, issuer));
    }

    @PostMapping(value = "/certificates/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificateDto> issueUploaded(
            @Valid @ModelAttribute CertificateUploadForm form,
            @AuthenticationPrincipal User issuer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.issueUploaded(form.toRequest(), form.getFile(), issuer));
    }

    @GetMapping("/certificates")
    public ResponseEntity<PageResponse<CertificateDto>> listCertificates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User issuer) {
        return ResponseEntity.ok(certificateService.listIssuerCertificates(issuer, search, status, page, size));
    }

    @GetMapping("/certificates/{certificateId}")
    public ResponseEntity<CertificateDto> getCertificate(@PathVariable String certificateId,
                                                         @AuthenticationPrincipal User issuer) {
        return ResponseEntity.ok(certificateService.getIssuerCertificate(certificateId, issuer));
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    public ResponseEntity<CertificateDto> revokeCertificate(@PathVariable String certificateId,
                                                            @Valid @RequestBody RevokeRequest request,
                                                            @AuthenticationPrincipal User issuer) {
        return ResponseEntity.ok(certificateService.revokeCertificate(certificateId, request.reason(), issuer));
    }

    @GetMapping("/certificates/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String certificateId,
                                                      @AuthenticationPrincipal User issuer) {
        CertificateService.DownloadFile file = certificateService.downloadCertificate(certificateId, issuer);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }

    @GetMapping("/certificates/{certificateId}/qr")
    public ResponseEntity<byte[]> downloadQrCode(@PathVariable String certificateId) {
        byte[] png = certificateService.qrCodeForCertificate(certificateId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("qr-" + certificateId + ".png", StandardCharsets.UTF_8)
                                .build().toString())
                .body(png);
    }

    @GetMapping("/stats")
    public ResponseEntity<IssuerStatsDto> stats(@AuthenticationPrincipal User issuer) {
        return ResponseEntity.ok(certificateService.issuerStats(issuer));
    }

    private MediaType mediaType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
