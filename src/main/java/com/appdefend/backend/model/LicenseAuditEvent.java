package com.appdefend.backend.model;

import java.time.LocalDateTime;

public record LicenseAuditEvent(
    Long id,
    String eventType,
    String eventStatus,
    String licenseId,
    String actor,
    String moduleName,
    String detailsJson,
    LocalDateTime createdAt
) {
}
