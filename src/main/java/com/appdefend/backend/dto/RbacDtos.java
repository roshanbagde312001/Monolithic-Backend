package com.appdefend.backend.dto;

import jakarta.validation.constraints.NotBlank;

public final class RbacDtos {
    private RbacDtos() {
    }

    public record CreateRoleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description
    ) {
    }

    public record CreatePermissionRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotBlank String moduleName
    ) {
    }

    public record CreateViewRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String route,
        String description
    ) {
    }
}
