package com.appdefend.backend.model;

public enum LicenseStatus {
    VALID,
    GRACE_PERIOD,
    EXPIRED,
    INVALID_SIGNATURE,
    DEPLOYMENT_MISMATCH,
    NOT_INSTALLED
}
