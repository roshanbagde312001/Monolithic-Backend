package com.appdefend.backend.service;

import com.appdefend.backend.dto.IntegrationDtos.TestIntegrationResponse;
import com.appdefend.backend.exception.ApiException;
import com.appdefend.backend.model.OemIntegration;
import com.appdefend.backend.repository.GitLabMetadataRepository;
import com.appdefend.backend.repository.IntegrationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class IntegrationService {
    private final IntegrationRepository integrationRepository;
    private final GitLabMetadataRepository gitLabMetadataRepository;
    private final RestClient restClient;

    public IntegrationService(IntegrationRepository integrationRepository,
                              GitLabMetadataRepository gitLabMetadataRepository) {
        this.integrationRepository = integrationRepository;
        this.gitLabMetadataRepository = gitLabMetadataRepository;
        this.restClient = RestClient.builder().build();
    }

    public List<OemIntegration> findAll() {
        return integrationRepository.findAll();
    }

    public OemIntegration create(OemIntegration integration) {
        Long id = integrationRepository.create(
            integration.name(),
            integration.providerType(),
            integration.deploymentMode(),
            integration.namespacePath(),
            integration.baseUrl(),
            integration.credentialsJson(),
            integration.active());
        return integrationRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create integration"));
    }

    public TestIntegrationResponse test(Long id) {
        OemIntegration integration = integrationRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Integration not found"));
        try {
            restClient.get().uri(integration.baseUrl()).retrieve().toBodilessEntity();
            return new TestIntegrationResponse(true, integration.providerType().name(), "Connectivity test completed");
        } catch (Exception ex) {
            return new TestIntegrationResponse(false, integration.providerType().name(), "Connectivity failed: " + ex.getMessage());
        }
    }

    public OemIntegration update(Long id, OemIntegration integration) {
        integrationRepository.update(id, integration.name(), integration.providerType(), integration.deploymentMode(),
            integration.namespacePath(), integration.baseUrl(),
            integration.credentialsJson(), integration.active());
        return integrationRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Integration not found"));
    }

    @Transactional
    public void delete(Long id) {
        gitLabMetadataRepository.deleteByIntegrationId(id);
        integrationRepository.delete(id);
    }
}
