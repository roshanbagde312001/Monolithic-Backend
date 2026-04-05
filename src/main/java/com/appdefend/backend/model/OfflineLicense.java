package com.appdefend.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OfflineLicense(
    Long id,
    String licenseId,
    String customerName,
    String customerEmail,
    String deploymentId,
    String licenseTier,
    LocalDate validFrom,
    LocalDate validUntil,
    int gracePeriodDays,
    int maxNamedUsers,
    String featuresJson,
    String metadataJson,
    String payloadJson,
    String signature,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime installedAt
) {
}
