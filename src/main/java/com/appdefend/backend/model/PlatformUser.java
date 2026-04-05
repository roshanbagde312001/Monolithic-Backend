package com.appdefend.backend.model;

import java.time.LocalDateTime;
import java.util.List;

public record PlatformUser(
    Long id,
    String fullName,
    String email,
    String passwordHash,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> roles,
    List<String> permissions,
    List<String> views
) {
}
