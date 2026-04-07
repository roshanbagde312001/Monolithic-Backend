package com.appdefend.backend.service;

import com.appdefend.backend.dto.IntegrationDtos.GitLabGroupResponse;
import com.appdefend.backend.dto.IntegrationDtos.GitLabNamespaceResponse;
import com.appdefend.backend.dto.IntegrationDtos.GitLabProjectResponse;
import com.appdefend.backend.exception.ApiException;
import com.appdefend.backend.model.IntegrationProviderType;
import com.appdefend.backend.model.OemIntegration;
import com.appdefend.backend.repository.GitLabMetadataRepository;
import com.appdefend.backend.repository.IntegrationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GitLabSyncService {
    private final IntegrationRepository integrationRepository;
    private final GitLabMetadataRepository gitLabMetadataRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GitLabSyncService(IntegrationRepository integrationRepository,
                             GitLabMetadataRepository gitLabMetadataRepository,
                             ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.gitLabMetadataRepository = gitLabMetadataRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public List<GitLabNamespaceResponse> fetchNamespaces(Long integrationId) {
        OemIntegration integration = requireGitLabIntegration(integrationId);
        JsonNode credentials = readCredentials(integration.credentialsJson());
        String apiBaseUrl = normalizeGitLabApiBaseUrl(integration.baseUrl());
        List<JsonNode> namespaces = getPagedArray(
            apiBaseUrl + "/namespaces?per_page=100",
            credentials.path("token").asText()
        );
        return namespaces.stream()
            .map(node -> new GitLabNamespaceResponse(
                node.path("id").isMissingNode() ? null : node.path("id").asLong(),
                node.path("name").asText(),
                node.path("full_path").asText(),
                node.path("kind").asText(),
                node.path("web_url").asText(),
                node.toString()
            ))
            .toList();
    }

    @Transactional
    public void sync(Long integrationId) {
        OemIntegration integration = requireGitLabIntegration(integrationId);
        JsonNode credentials = readCredentials(integration.credentialsJson());
        String token = credentials.path("token").asText();
        if (token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GitLab token is required in credentialsJson");
        }

        List<GitLabGroupResponse> groups;
        List<GitLabProjectResponse> projects;

        if ("SAAS".equalsIgnoreCase(integration.deploymentMode())) {
            if (integration.namespacePath() == null || integration.namespacePath().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "namespacePath is required for GitLab SaaS integrations");
            }
            String apiBaseUrl = normalizeGitLabApiBaseUrl(integration.baseUrl());
            JsonNode resolvedGroup = resolveGroupByNamespace(apiBaseUrl, token, integration.namespacePath());
            Long groupId = resolvedGroup.path("id").asLong();
            groups = fetchGroupsForNamespace(apiBaseUrl, token, groupId);
            projects = fetchProjectsForNamespace(apiBaseUrl, token, groupId);
        } else {
            String apiBaseUrl = normalizeGitLabApiBaseUrl(integration.baseUrl());
            groups = fetchAllGroups(apiBaseUrl, token);
            projects = fetchAllProjects(apiBaseUrl, token);
        }

        gitLabMetadataRepository.replaceGroups(integrationId, groups);
        gitLabMetadataRepository.replaceProjects(integrationId, projects);
    }

    public List<GitLabGroupResponse> storedGroups(Long integrationId) {
        requireGitLabIntegration(integrationId);
        return gitLabMetadataRepository.findGroups(integrationId);
    }

    public List<GitLabProjectResponse> storedProjects(Long integrationId) {
        requireGitLabIntegration(integrationId);
        return gitLabMetadataRepository.findProjects(integrationId);
    }

    private List<GitLabGroupResponse> fetchGroupsForNamespace(String baseUrl, String token, Long groupId) {
        List<JsonNode> groups = getPagedArray(
            baseUrl + "/groups/" + groupId + "/descendant_groups?per_page=100",
            token
        );
        List<GitLabGroupResponse> results = new ArrayList<>();
        JsonNode rootGroup = getObject(baseUrl + "/groups/" + groupId, token);
        results.add(toGroupResponse(rootGroup));
        groups.stream()
            .map(this::toGroupResponse)
            .forEach(results::add);
        return results;
    }

    private List<GitLabProjectResponse> fetchProjectsForNamespace(String baseUrl, String token, Long groupId) {
        List<JsonNode> projects = getPagedArray(
            baseUrl + "/groups/" + groupId + "/projects?include_subgroups=true&with_shared=false&per_page=100",
            token
        );
        return projects.stream().map(this::toProjectResponse).toList();
    }

    private List<GitLabGroupResponse> fetchAllGroups(String baseUrl, String token) {
        return getPagedArray(baseUrl + "/groups?all_available=true&per_page=100", token).stream()
            .map(this::toGroupResponse)
            .toList();
    }

    private List<GitLabProjectResponse> fetchAllProjects(String baseUrl, String token) {
        return getPagedArray(baseUrl + "/projects?membership=false&simple=false&per_page=100", token).stream()
            .map(this::toProjectResponse)
            .toList();
    }

    private List<JsonNode> getPagedArray(String firstUrl, String token) {
        List<JsonNode> results = new ArrayList<>();
        for (int page = 1; page <= 50; page++) {
            String url = firstUrl + (firstUrl.contains("?") ? "&page=" : "?page=") + page;
            String body;
            try {
                body = restClient.get()
                    .uri(url)
                    .header("PRIVATE-TOKEN", token)
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            } catch (RestClientResponseException ex) {
                throw mapGitLabResponseException(ex);
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "GitLab request failed: " + ex.getMessage());
            }
            if (body != null && body.trim().startsWith("<")) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "GitLab returned HTML instead of JSON. Check that baseUrl points to the GitLab host or API root and that the token is valid."
                );
            }
            try {
                JsonNode node = objectMapper.readTree(body == null ? "[]" : body);
                if (!node.isArray() || node.isEmpty()) {
                    break;
                }
                node.forEach(results::add);
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to parse GitLab response: " + ex.getMessage());
            }
        }
        return results;
    }

    private JsonNode getObject(String url, String token) {
        String body;
        try {
            body = restClient.get()
                .uri(url)
                .header("PRIVATE-TOKEN", token)
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        } catch (RestClientResponseException ex) {
            throw mapGitLabResponseException(ex);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GitLab request failed: " + ex.getMessage());
        }

        if (body != null && body.trim().startsWith("<")) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "GitLab returned HTML instead of JSON. Check that baseUrl points to the GitLab host or API root and that the token is valid."
            );
        }

        try {
            JsonNode node = objectMapper.readTree(body == null ? "{}" : body);
            if (!node.isObject() || node.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "GitLab returned an empty object for the requested group.");
            }
            return node;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to parse GitLab response: " + ex.getMessage());
        }
    }

    private JsonNode resolveGroupByNamespace(String baseUrl, String token, String namespacePath) {
        List<JsonNode> groups = getPagedArray(
            baseUrl + "/groups?all_available=true&per_page=100&search=" + encode(lastSegment(namespacePath)),
            token
        );
        return groups.stream()
            .filter(node -> namespacePath.equalsIgnoreCase(node.path("full_path").asText()))
            .findFirst()
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_GATEWAY,
                "GitLab group was not found for namespacePath '" + namespacePath + "'. Use the exact GitLab group full path."
            ));
    }

    private GitLabGroupResponse toGroupResponse(JsonNode node) {
        return new GitLabGroupResponse(
            node.path("id").asLong(),
            node.path("name").asText(),
            node.path("path").asText(),
            node.path("full_path").asText(),
            node.path("visibility").asText(),
            node.path("web_url").asText(),
            node.toString()
        );
    }

    private GitLabProjectResponse toProjectResponse(JsonNode node) {
        JsonNode namespace = node.path("namespace");
        return new GitLabProjectResponse(
            node.path("id").asLong(),
            node.path("name").asText(),
            node.path("path").asText(),
            node.path("path_with_namespace").asText(),
            namespace.path("full_path").asText(),
            node.path("default_branch").asText(null),
            node.path("visibility").asText(),
            node.path("web_url").asText(),
            node.path("http_url_to_repo").asText(),
            node.path("ssh_url_to_repo").asText(),
            node.path("archived").isMissingNode() ? null : node.path("archived").asBoolean(),
            node.path("empty_repo").isMissingNode() ? null : node.path("empty_repo").asBoolean(),
            node.toString()
        );
    }

    private JsonNode readCredentials(String credentialsJson) {
        try {
            return objectMapper.readTree(credentialsJson == null ? "{}" : credentialsJson);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid credentialsJson for integration");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String lastSegment(String namespacePath) {
        String normalized = namespacePath == null ? "" : namespacePath.trim().replaceAll("/+$", "");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private ApiException mapGitLabResponseException(RestClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED) {
            return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "GitLab authentication failed. Check the token in credentialsJson and confirm it has API access."
            );
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "GitLab denied access. The token is valid but does not have permission for the requested groups or projects."
            );
        }
        if (status == HttpStatus.NOT_FOUND) {
            return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "GitLab endpoint was not found. Check the baseUrl and namespacePath values."
            );
        }
        return new ApiException(
            HttpStatus.BAD_GATEWAY,
            "GitLab request failed with status " + ex.getStatusCode().value() + "."
        );
    }

    private String normalizeGitLabApiBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "baseUrl is required for GitLab integrations");
        }
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                throw new IllegalArgumentException("Missing scheme or host");
            }
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            if (path.isBlank()) {
                path = "/api/v4";
            } else if (!path.equalsIgnoreCase("/api/v4")) {
                if (path.startsWith("/api/v4/")) {
                    path = "/api/v4";
                } else if (!path.startsWith("/api/")) {
                    path = "/api/v4";
                }
            }
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            return scheme + "://" + host + port + path;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid GitLab baseUrl. Use values like https://gitlab.com or https://gitlab.company.local/api/v4");
        }
    }

    private OemIntegration requireGitLabIntegration(Long integrationId) {
        OemIntegration integration = integrationRepository.findById(integrationId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Integration not found"));
        if (integration.providerType() != IntegrationProviderType.GITLAB) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Integration is not a GitLab integration");
        }
        return integration;
    }
}
