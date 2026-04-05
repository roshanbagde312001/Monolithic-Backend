package com.appdefend.backend.dto;

import jakarta.validation.constraints.NotBlank;

public final class LicenseDebugDtos {
    private LicenseDebugDtos() {
    }

    public record VerifyLicenseRequest(
        @NotBlank String payloadJson,
        @NotBlank String signature
    ) {
    }
}
