package com.appdefend.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {
    }

    public record LogoutRequest(
        @NotBlank String accessToken,
        @NotBlank String refreshToken
    ) {
    }

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserSummary user
    ) {
    }

    public record UserSummary(
        Long id,
        String fullName,
        String email,
        List<String> roles,
        List<String> permissions,
        List<String> views
    ) {
    }

    public record CreateUserRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        boolean enabled,
        List<Long> roleIds
    ) {
    }

    public record UpdateUserRequest(
        @NotBlank String fullName,
        boolean enabled,
        List<Long> roleIds
    ) {
    }
}
