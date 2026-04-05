package com.appdefend.backend.license;

import com.appdefend.backend.model.LicenseStatus;

public record LicenseUsageSnapshot(
    LicensePayload payload,
    LicenseStatus status,
    int currentEnabledUsers,
    int remainingNamedUsers,
    boolean loginAllowed,
    boolean provisioningAllowed
) {
}
