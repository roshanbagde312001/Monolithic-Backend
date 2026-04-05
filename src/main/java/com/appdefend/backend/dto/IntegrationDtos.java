package com.appdefend.backend.dto;

import com.appdefend.backend.model.IntegrationProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class IntegrationDtos {
    private IntegrationDtos() {
    }

    public record CreateIntegrationRequest(
        @NotBlank String name,
        @NotNull IntegrationProviderType providerType,
        @NotBlank String baseUrl,
        @NotBlank String credentialsJson,
        boolean active
    ) {
    }

    public record TestIntegrationResponse(
        boolean success,
        String provider,
        String message
    ) {
    }
}
