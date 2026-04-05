package com.appdefend.backend.license;

public record EncryptedLicenseBundle(
    String encryptedPayloadBase64,
    String ivBase64,
    String saltBase64,
    String signature
) {
}
