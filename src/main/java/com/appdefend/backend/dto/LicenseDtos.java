package com.appdefend.backend.dto;

import com.appdefend.backend.model.LicenseStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public final class LicenseDtos {
    private LicenseDtos() {
    }

    public record InstallLicenseRequest(
        @NotBlank String payloadJson,
        @NotBlank String signature
    ) {
    }

    public record InstallEncryptedLicenseBundleRequest(
        @NotBlank String encryptedPayloadBase64,
        @NotBlank String ivBase64,
        @NotBlank String saltBase64,
        @NotBlank String signature,
        @NotBlank String passphrase
    ) {
    }

    public record CreateEncryptedLicenseBundleRequest(
        @NotBlank String payloadJson,
        @NotBlank String signature,
        @NotBlank String passphrase
    ) {
    }

    public record LicenseStatusResponse(
        String licenseId,
        String customerName,
        String deploymentId,
        String tier,
        LocalDate validFrom,
        LocalDate validUntil,
        int gracePeriodDays,
        int maxNamedUsers,
        int currentEnabledUsers,
        int remainingNamedUsers,
        LicenseStatus status,
        boolean loginAllowed,
        boolean provisioningAllowed
    ) {
    }
}
