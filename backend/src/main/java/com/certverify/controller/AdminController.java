package com.certverify.controller;

import com.certverify.dto.AdminDashboardDto;
import com.certverify.dto.CertificateDto;
import com.certverify.dto.IssuerStatusRequest;
import com.certverify.dto.PageResponse;
import com.certverify.dto.UserDto;
import com.certverify.dto.VerificationLogDto;
import com.certverify.service.AdminService;
import com.certverify.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CertificateService certificateService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> dashboard() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/issuers")
    public ResponseEntity<PageResponse<UserDto>> issuers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.listIssuers(status, page, size));
    }

    @PatchMapping("/issuers/{issuerId}/status")
    public ResponseEntity<UserDto> updateIssuerStatus(@PathVariable Long issuerId,
                                                      @Valid @RequestBody IssuerStatusRequest request) {
        return ResponseEntity.ok(adminService.updateIssuerStatus(issuerId, request));
    }

    @GetMapping("/certificates")
    public ResponseEntity<PageResponse<CertificateDto>> certificates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(certificateService.listAllCertificates(search, status, page, size));
    }

    @GetMapping("/logs")
    public ResponseEntity<PageResponse<VerificationLogDto>> logs(
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.listLogs(result, page, size));
    }
}
