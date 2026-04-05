package com.appdefend.backend.license;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record LicensePayload(
    String licenseId,
    String customerName,
    String customerEmail,
    String deploymentId,
    String licenseTier,
    LocalDate validFrom,
    LocalDate validUntil,
    int gracePeriodDays,
    int maxNamedUsers,
    List<String> features,
    Map<String, Object> metadata,
    OffsetDateTime issuedAt,
    String issuer
) {
}
