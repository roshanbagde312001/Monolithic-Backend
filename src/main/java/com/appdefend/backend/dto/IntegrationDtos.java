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
        String deploymentMode,
        String namespacePath,
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

    public record GitLabNamespaceResponse(
        Long id,
        String name,
        String fullPath,
        String kind,
        String webUrl,
        String rawJson
    ) {
    }

    public record GitLabGroupResponse(
        Long gitlabGroupId,
        String name,
        String path,
        String fullPath,
        String visibility,
        String webUrl,
        String rawJson
    ) {
    }

    public record GitLabProjectResponse(
        Long gitlabProjectId,
        String name,
        String path,
        String pathWithNamespace,
        String namespaceFullPath,
        String defaultBranch,
        String visibility,
        String webUrl,
        String httpUrlToRepo,
        String sshUrlToRepo,
        Boolean archived,
        Boolean emptyRepo,
        String rawJson
    ) {
    }
}
