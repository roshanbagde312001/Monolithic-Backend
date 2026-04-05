package com.appdefend.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.license")
public class LicenseProperties {
    private String publicKey;
    private String expectedDeploymentId = "AIRGAP-DEFAULT";

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getExpectedDeploymentId() {
        return expectedDeploymentId;
    }

    public void setExpectedDeploymentId(String expectedDeploymentId) {
        this.expectedDeploymentId = expectedDeploymentId;
    }
}
