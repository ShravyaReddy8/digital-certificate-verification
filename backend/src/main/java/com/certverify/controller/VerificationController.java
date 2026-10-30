package com.certverify.controller;

import com.certverify.dto.VerificationResponse;
import com.certverify.entity.VerificationMethod;
import com.certverify.exception.BadRequestException;
import com.certverify.service.CertificateService;
import com.certverify.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final CertificateService certificateService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> verifyFile(@RequestPart("file") MultipartFile file,
                                                           HttpServletRequest request) {
        return ResponseEntity.ok(verificationService.verifyByFile(file, clientInfo(request)));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<VerificationResponse> verifyById(
            @PathVariable String certificateId,
            @RequestParam(defaultValue = "CERTIFICATE_ID") String method,
            HttpServletRequest request) {
        VerificationMethod verificationMethod;
        if (method.equalsIgnoreCase("QR")) {
            verificationMethod = VerificationMethod.QR;
        } else if (method.equalsIgnoreCase("CERTIFICATE_ID")) {
            verificationMethod = VerificationMethod.CERTIFICATE_ID;
        } else {
            throw new BadRequestException("Verification method must be CERTIFICATE_ID or QR.");
        }
        return ResponseEntity.ok(verificationService.verifyByCertificateId(
                certificateId, verificationMethod, clientInfo(request)));
    }

    @GetMapping(value = "/{certificateId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable String certificateId) {
        byte[] png = certificateService.qrCodeForCertificate(certificateId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(png);
    }

    private VerificationService.ClientInfo clientInfo(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return new VerificationService.ClientInfo(ip, request.getHeader("User-Agent"));
    }
}
