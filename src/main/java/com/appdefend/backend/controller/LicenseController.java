package com.appdefend.backend.controller;

import com.appdefend.backend.dto.LicenseDtos.InstallEncryptedLicenseBundleRequest;
import com.appdefend.backend.dto.LicenseDtos.InstallLicenseRequest;
import com.appdefend.backend.dto.LicenseDtos.CreateEncryptedLicenseBundleRequest;
import com.appdefend.backend.dto.LicenseDebugDtos.VerifyLicenseRequest;
import com.appdefend.backend.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/licenses")
@Tag(name = "Licenses", description = "Offline air-gapped license installation and status APIs")
public class LicenseController {
    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('LICENSE_READ')")
    @Operation(summary = "Get installed license status")
    public Object status() {
        return licenseService.currentStatus();
    }

    @PostMapping("/install")
    @PreAuthorize("hasAuthority('LICENSE_WRITE')")
    @Operation(summary = "Install signed offline license")
    public Object install(@Valid @RequestBody InstallLicenseRequest request) {
        return licenseService.install(request);
    }

    @PostMapping("/install/encrypted")
    @PreAuthorize("hasAuthority('LICENSE_WRITE')")
    @Operation(summary = "Install encrypted offline license bundle")
    public Object installEncrypted(@Valid @RequestBody InstallEncryptedLicenseBundleRequest request) {
        return licenseService.installEncryptedBundle(request);
    }

    @PostMapping("/bundles/encrypt")
    @PreAuthorize("hasAuthority('LICENSE_WRITE')")
    @Operation(summary = "Create encrypted license import bundle")
    public Object createEncryptedBundle(@Valid @RequestBody CreateEncryptedLicenseBundleRequest request) {
        return licenseService.createEncryptedBundlePreview(request.payloadJson(), request.signature(), request.passphrase());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('LICENSE_AUDIT_READ')")
    @Operation(summary = "Get recent license audit trail")
    public Object audit(@RequestParam(defaultValue = "50") int limit) {
        return licenseService.recentAudit(limit);
    }

    @GetMapping("/reports/capacity")
    @PreAuthorize("hasAuthority('LICENSE_REPORT_READ')")
    @Operation(summary = "Get license utilization report")
    public Object capacityReport() {
        return licenseService.capacityReport();
    }

    @GetMapping("/debug/key-info")
    @Operation(summary = "Temporary debug: show loaded public key fingerprint")
    public Object debugKeyInfo() {
        return licenseService.debugKeyInfo();
    }

    @PostMapping("/debug/verify")
    @Operation(summary = "Temporary debug: verify payload/signature against loaded key")
    public Object debugVerify(@Valid @RequestBody VerifyLicenseRequest request) {
        return licenseService.debugVerification(request);
    }
}
