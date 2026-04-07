package com.appdefend.backend.model;

import java.time.LocalDateTime;

public record OemIntegration(
    Long id,
    String name,
    IntegrationProviderType providerType,
    String deploymentMode,
    String namespacePath,
    String baseUrl,
    String credentialsJson,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
