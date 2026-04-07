package com.appdefend.backend.controller;

import com.appdefend.backend.dto.IntegrationDtos.CreateIntegrationRequest;
import com.appdefend.backend.model.OemIntegration;
import com.appdefend.backend.service.GitLabSyncService;
import com.appdefend.backend.service.IntegrationService;
import com.appdefend.backend.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations", description = "OEM integration management APIs for GitLab, GitHub Actions, Jenkins and Bamboo")
public class IntegrationController {
    private final IntegrationService integrationService;
    private final GitLabSyncService gitLabSyncService;
    private final LicenseService licenseService;

    public IntegrationController(IntegrationService integrationService,
                                 GitLabSyncService gitLabSyncService,
                                 LicenseService licenseService) {
        this.integrationService = integrationService;
        this.gitLabSyncService = gitLabSyncService;
        this.licenseService = licenseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "List integrations")
    public Object list() {
        licenseService.assertAnyFeatureEnabled(java.util.List.of("GITLAB", "GITHUB_ACTIONS", "JENKINS", "BAMBOO"), "INTEGRATION");
        return integrationService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    @Operation(summary = "Create OEM integration")
    public OemIntegration create(@Valid @RequestBody CreateIntegrationRequest request) {
        licenseService.assertFeatureEnabled(request.providerType().name(), "INTEGRATION");
        return integrationService.create(new OemIntegration(
            null,
            request.name(),
            request.providerType(),
            request.deploymentMode(),
            request.namespacePath(),
            request.baseUrl(),
            request.credentialsJson(),
            request.active(),
            null,
            null
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    @Operation(summary = "Update OEM integration")
    public OemIntegration update(@PathVariable Long id, @Valid @RequestBody CreateIntegrationRequest request) {
        licenseService.assertFeatureEnabled(request.providerType().name(), "INTEGRATION");
        return integrationService.update(id, new OemIntegration(
            id,
            request.name(),
            request.providerType(),
            request.deploymentMode(),
            request.namespacePath(),
            request.baseUrl(),
            request.credentialsJson(),
            request.active(),
            null,
            null
        ));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "Test OEM integration connectivity")
    public Object test(@PathVariable Long id) {
        licenseService.assertAnyFeatureEnabled(java.util.List.of("GITLAB", "GITHUB_ACTIONS", "JENKINS", "BAMBOO"), "INTEGRATION");
        return integrationService.test(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    @Operation(summary = "Delete OEM integration")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        integrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/providers")
    @Operation(summary = "List supported OEM providers")
    public Object providers() {
        return Map.of("supportedProviders", java.util.Arrays.stream(com.appdefend.backend.model.IntegrationProviderType.values()).toList());
    }

    @GetMapping("/{id}/gitlab/namespaces")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "List GitLab namespaces")
    public Object gitLabNamespaces(@PathVariable Long id) {
        licenseService.assertFeatureEnabled("GITLAB", "INTEGRATION");
        return gitLabSyncService.fetchNamespaces(id);
    }

    @PostMapping("/{id}/gitlab/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    @Operation(summary = "Sync GitLab groups and projects")
    public ResponseEntity<Void> syncGitLab(@PathVariable Long id) {
        licenseService.assertFeatureEnabled("GITLAB", "INTEGRATION");
        gitLabSyncService.sync(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/gitlab/groups")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "Get stored GitLab groups")
    public Object gitLabGroups(@PathVariable Long id) {
        licenseService.assertFeatureEnabled("GITLAB", "INTEGRATION");
        return gitLabSyncService.storedGroups(id);
    }

    @GetMapping("/{id}/gitlab/projects")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    @Operation(summary = "Get stored GitLab projects")
    public Object gitLabProjects(@PathVariable Long id) {
        licenseService.assertFeatureEnabled("GITLAB", "INTEGRATION");
        return gitLabSyncService.storedProjects(id);
    }
}
