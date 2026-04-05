package com.appdefend.backend.dto;

import java.time.LocalDate;
import java.util.List;

public final class LicenseReportDtos {
    private LicenseReportDtos() {
    }

    public record LicenseAuditResponse(
        Long id,
        String eventType,
        String eventStatus,
        String licenseId,
        String actor,
        String moduleName,
        String detailsJson,
        java.time.LocalDateTime createdAt
    ) {
    }

    public record LicenseCapacityReport(
        String licenseId,
        String tier,
        String deploymentId,
        int maxNamedUsers,
        int currentEnabledUsers,
        int remainingNamedUsers,
        double utilizationPercent,
        LocalDate validUntil,
        long daysUntilExpiry,
        List<String> enabledFeatures
    ) {
    }
}
