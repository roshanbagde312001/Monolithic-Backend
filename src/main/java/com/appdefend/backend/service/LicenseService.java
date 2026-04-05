package com.appdefend.backend.service;

import com.appdefend.backend.config.LicenseProperties;
import com.appdefend.backend.dto.LicenseDtos.InstallLicenseRequest;
import com.appdefend.backend.dto.LicenseDtos.InstallEncryptedLicenseBundleRequest;
import com.appdefend.backend.dto.LicenseDtos.LicenseStatusResponse;
import com.appdefend.backend.dto.LicenseDebugDtos.VerifyLicenseRequest;
import com.appdefend.backend.dto.LicenseReportDtos.LicenseAuditResponse;
import com.appdefend.backend.dto.LicenseReportDtos.LicenseCapacityReport;
import com.appdefend.backend.license.EncryptedLicenseBundle;
import com.appdefend.backend.exception.ApiException;
import com.appdefend.backend.license.LicenseCryptoService;
import com.appdefend.backend.license.LicensePayload;
import com.appdefend.backend.license.LicenseUsageSnapshot;
import com.appdefend.backend.model.LicenseAuditEvent;
import com.appdefend.backend.model.LicenseStatus;
import com.appdefend.backend.model.OfflineLicense;
import com.appdefend.backend.repository.LicenseAuditRepository;
import com.appdefend.backend.repository.LicenseRepository;
import com.appdefend.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseService {
    private final LicenseRepository licenseRepository;
    private final LicenseAuditRepository licenseAuditRepository;
    private final UserRepository userRepository;
    private final LicenseCryptoService licenseCryptoService;
    private final LicenseProperties licenseProperties;
    private final ObjectMapper objectMapper;

    public LicenseService(LicenseRepository licenseRepository,
                          LicenseAuditRepository licenseAuditRepository,
                          UserRepository userRepository,
                          LicenseCryptoService licenseCryptoService,
                          LicenseProperties licenseProperties,
                          ObjectMapper objectMapper) {
        this.licenseRepository = licenseRepository;
        this.licenseAuditRepository = licenseAuditRepository;
        this.userRepository = userRepository;
        this.licenseCryptoService = licenseCryptoService;
        this.licenseProperties = licenseProperties;
        this.objectMapper = objectMapper;
    }

    public OfflineLicense install(InstallLicenseRequest request) {
        return installInternal(request.payloadJson(), request.signature(), "system-api");
    }

    public OfflineLicense installEncryptedBundle(InstallEncryptedLicenseBundleRequest request) {
        String payloadJson = licenseCryptoService.decryptBundle(
            request.encryptedPayloadBase64(),
            request.ivBase64(),
            request.saltBase64(),
            request.passphrase()
        );
        return installInternal(payloadJson, request.signature(), "encrypted-bundle-api");
    }

    public LicenseUsageSnapshot currentSnapshot() {
        return licenseRepository.findActiveLicense()
            .map(this::buildSnapshot)
            .orElseGet(this::unlicensedSnapshot);
    }

    public LicenseStatusResponse currentStatus() {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        audit("LICENSE_STATUS_VIEWED", "SUCCESS", snapshot.payload().licenseId(), "system-api", "LICENSE",
            Map.of("status", snapshot.status().name(), "currentEnabledUsers", snapshot.currentEnabledUsers()));
        return new LicenseStatusResponse(
            snapshot.payload().licenseId(),
            snapshot.payload().customerName(),
            snapshot.payload().deploymentId(),
            snapshot.payload().licenseTier(),
            snapshot.payload().validFrom(),
            snapshot.payload().validUntil(),
            snapshot.payload().gracePeriodDays(),
            snapshot.payload().maxNamedUsers(),
            snapshot.currentEnabledUsers(),
            snapshot.remainingNamedUsers(),
            snapshot.status(),
            snapshot.loginAllowed(),
            snapshot.provisioningAllowed()
        );
    }

    public void assertLoginAllowed() {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        if (!snapshot.loginAllowed()) {
            audit("LOGIN_BLOCKED_BY_LICENSE", "DENIED", snapshot.payload().licenseId(), "system", "AUTH",
                Map.of("status", snapshot.status().name()));
            throw new ApiException(HttpStatus.FORBIDDEN, "Installed license is expired or invalid");
        }
    }

    public void assertProvisioningAllowed(int projectedEnabledUsers) {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        if (!snapshot.provisioningAllowed()) {
            audit("USER_PROVISIONING_BLOCKED", "DENIED", snapshot.payload().licenseId(), "system", "USER",
                Map.of("status", snapshot.status().name(), "projectedEnabledUsers", projectedEnabledUsers));
            throw new ApiException(HttpStatus.FORBIDDEN, "Installed license does not allow user provisioning");
        }
        if (projectedEnabledUsers > snapshot.payload().maxNamedUsers()) {
            audit("USER_CAPACITY_EXCEEDED", "DENIED", snapshot.payload().licenseId(), "system", "USER",
                Map.of("licensedUsers", snapshot.payload().maxNamedUsers(), "projectedEnabledUsers", projectedEnabledUsers));
            throw new ApiException(HttpStatus.FORBIDDEN, "Enabled user count exceeds licensed capacity");
        }
    }

    public void assertFeatureEnabled(String featureName, String moduleName) {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        boolean enabled = snapshot.payload().features().stream().anyMatch(featureName::equalsIgnoreCase);
        if (!enabled) {
            audit("FEATURE_ACCESS_BLOCKED", "DENIED", snapshot.payload().licenseId(), "system", moduleName,
                Map.of("feature", featureName, "status", snapshot.status().name()));
            throw new ApiException(HttpStatus.FORBIDDEN, "Feature " + featureName + " is not enabled in the installed license");
        }
    }

    public void assertAnyFeatureEnabled(List<String> featureNames, String moduleName) {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        boolean enabled = snapshot.payload().features().stream()
            .anyMatch(feature -> featureNames.stream().anyMatch(feature::equalsIgnoreCase));
        if (!enabled) {
            audit("FEATURE_ACCESS_BLOCKED", "DENIED", snapshot.payload().licenseId(), "system", moduleName,
                Map.of("requiredFeatures", featureNames, "status", snapshot.status().name()));
            throw new ApiException(HttpStatus.FORBIDDEN, "None of the required licensed features are enabled: " + featureNames);
        }
    }

    public List<LicenseAuditResponse> recentAudit(int limit) {
        return licenseAuditRepository.findRecent(limit).stream()
            .map(this::toAuditResponse)
            .toList();
    }

    public LicenseCapacityReport capacityReport() {
        LicenseUsageSnapshot snapshot = currentSnapshot();
        double utilizationPercent = snapshot.payload().maxNamedUsers() <= 0
            ? 0.0
            : (snapshot.currentEnabledUsers() * 100.0) / snapshot.payload().maxNamedUsers();
        return new LicenseCapacityReport(
            snapshot.payload().licenseId(),
            snapshot.payload().licenseTier(),
            snapshot.payload().deploymentId(),
            snapshot.payload().maxNamedUsers(),
            snapshot.currentEnabledUsers(),
            snapshot.remainingNamedUsers(),
            utilizationPercent,
            snapshot.payload().validUntil(),
            ChronoUnit.DAYS.between(LocalDate.now(), snapshot.payload().validUntil()),
            snapshot.payload().features()
        );
    }

    public EncryptedLicenseBundle createEncryptedBundlePreview(String payloadJson, String signature, String passphrase) {
        LicensePayload payload = licenseCryptoService.readPayload(payloadJson);
        String canonicalPayload = licenseCryptoService.canonicalize(payload);
        return licenseCryptoService.encryptBundle(canonicalPayload, signature, passphrase);
    }

    public Map<String, Object> debugVerification(VerifyLicenseRequest request) {
        String rawPayload = request.payloadJson() == null ? "" : request.payloadJson().trim();
        LicensePayload payload = licenseCryptoService.readPayload(rawPayload);
        String canonicalPayload = licenseCryptoService.canonicalize(payload);
        boolean rawValid = licenseCryptoService.verify(rawPayload, request.signature());
        boolean canonicalValid = licenseCryptoService.verify(canonicalPayload, request.signature());
        return Map.of(
            "deploymentId", licenseProperties.getExpectedDeploymentId(),
            "loadedPublicKeyFingerprint", licenseCryptoService.fingerprint(),
            "licenseId", payload.licenseId(),
            "rawSignatureValid", rawValid,
            "canonicalSignatureValid", canonicalValid
        );
    }

    public Map<String, Object> debugKeyInfo() {
        return Map.of(
            "deploymentId", licenseProperties.getExpectedDeploymentId(),
            "loadedPublicKeyFingerprint", licenseCryptoService.fingerprint()
        );
    }

    private LicenseUsageSnapshot buildSnapshot(OfflineLicense license) {
        LicensePayload payload = licenseCryptoService.readPayload(license.payloadJson());
        String canonicalPayload = licenseCryptoService.canonicalize(payload);
        String rawPayload = license.payloadJson() == null ? "" : license.payloadJson().trim();
        LicenseStatus status;
        boolean signatureValid = licenseCryptoService.verify(rawPayload, license.signature())
            || licenseCryptoService.verify(canonicalPayload, license.signature());
        if (!signatureValid) {
            status = LicenseStatus.INVALID_SIGNATURE;
        } else if (!licenseProperties.getExpectedDeploymentId().equals(payload.deploymentId())) {
            status = LicenseStatus.DEPLOYMENT_MISMATCH;
        } else {
            LocalDate today = LocalDate.now();
            LocalDate graceEnd = payload.validUntil().plusDays(payload.gracePeriodDays());
            if (today.isBefore(payload.validFrom()) || today.isAfter(graceEnd)) {
                status = LicenseStatus.EXPIRED;
            } else if (today.isAfter(payload.validUntil())) {
                status = LicenseStatus.GRACE_PERIOD;
            } else {
                status = LicenseStatus.VALID;
            }
        }

        int enabledUsers = userRepository.countEnabledUsers();
        int remaining = payload.maxNamedUsers() - enabledUsers;
        boolean loginAllowed = status == LicenseStatus.VALID || status == LicenseStatus.GRACE_PERIOD;
        boolean provisioningAllowed = status == LicenseStatus.VALID && enabledUsers <= payload.maxNamedUsers();
        return new LicenseUsageSnapshot(payload, status, enabledUsers, remaining, loginAllowed, provisioningAllowed);
    }

    private LicenseUsageSnapshot unlicensedSnapshot() {
        int enabledUsers = userRepository.countEnabledUsers();
        LicensePayload payload = new LicensePayload(
            "UNLICENSED",
            "Unlicensed Deployment",
            null,
            licenseProperties.getExpectedDeploymentId(),
            "UNLICENSED",
            LocalDate.now(),
            LocalDate.now(),
            0,
            0,
            java.util.List.of(),
            Map.of(),
            java.time.OffsetDateTime.now(),
            "N/A"
        );
        return new LicenseUsageSnapshot(payload, LicenseStatus.NOT_INSTALLED, enabledUsers, 0 - enabledUsers, true, false);
    }

    @Transactional
    private OfflineLicense installInternal(String payloadJson, String signature, String actor) {
        String rawPayload = payloadJson == null ? "" : payloadJson.trim();
        LicensePayload payload = licenseCryptoService.readPayload(rawPayload);
        String canonicalPayload = licenseCryptoService.canonicalize(payload);
        boolean signatureValid = licenseCryptoService.verify(rawPayload, signature)
            || licenseCryptoService.verify(canonicalPayload, signature);
        if (!signatureValid) {
            audit("LICENSE_INSTALL", "FAILED", payload.licenseId(), actor, "LICENSE", Map.of("reason", "INVALID_SIGNATURE"));
            throw new ApiException(HttpStatus.BAD_REQUEST, "License signature is invalid");
        }
        if (!licenseProperties.getExpectedDeploymentId().equals(payload.deploymentId())) {
            audit("LICENSE_INSTALL", "FAILED", payload.licenseId(), actor, "LICENSE", Map.of("reason", "DEPLOYMENT_MISMATCH"));
            throw new ApiException(HttpStatus.BAD_REQUEST, "License deployment id does not match this installation");
        }
        if (payload.maxNamedUsers() < userRepository.countEnabledUsers()) {
            audit("LICENSE_INSTALL", "FAILED", payload.licenseId(), actor, "LICENSE", Map.of("reason", "CAPACITY_BELOW_CURRENT_USAGE"));
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installed license is below current enabled user count");
        }
        Long id = licenseRepository.insert(new OfflineLicense(
            null,
            payload.licenseId(),
            payload.customerName(),
            payload.customerEmail(),
            payload.deploymentId(),
            payload.licenseTier(),
            payload.validFrom(),
            payload.validUntil(),
            payload.gracePeriodDays(),
            payload.maxNamedUsers(),
            toJson(payload.features()),
            toJson(payload.metadata()),
            rawPayload,
            signature,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        ));
        licenseRepository.deactivateOtherLicenses(id);
        audit("LICENSE_INSTALL", "SUCCESS", payload.licenseId(), actor, "LICENSE",
            Map.of("tier", payload.licenseTier(), "maxNamedUsers", payload.maxNamedUsers(), "validUntil", payload.validUntil().toString()));
        return licenseRepository.findActiveLicense()
            .filter(license -> license.id().equals(id))
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to install license"));
    }

    private void audit(String eventType, String status, String licenseId, String actor, String moduleName, Map<String, Object> details) {
        try {
            licenseAuditRepository.create(eventType, status, licenseId, actor, moduleName, objectMapper.writeValueAsString(details));
        } catch (Exception ignored) {
        }
    }

    private LicenseAuditResponse toAuditResponse(LicenseAuditEvent event) {
        return new LicenseAuditResponse(
            event.id(),
            event.eventType(),
            event.eventStatus(),
            event.licenseId(),
            event.actor(),
            event.moduleName(),
            event.detailsJson(),
            event.createdAt()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize license metadata");
        }
    }
}
