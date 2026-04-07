package com.appdefend.backend.repository;

import com.appdefend.backend.dto.IntegrationDtos.GitLabGroupResponse;
import com.appdefend.backend.dto.IntegrationDtos.GitLabProjectResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GitLabMetadataRepository {
    private final JdbcTemplate jdbcTemplate;

    public GitLabMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceGroups(Long integrationId, List<GitLabGroupResponse> groups) {
        jdbcTemplate.update("DELETE FROM gitlab_groups WHERE integration_id = ?", integrationId);
        for (GitLabGroupResponse group : groups) {
            jdbcTemplate.update("""
                INSERT INTO gitlab_groups (integration_id, gitlab_group_id, full_path, name, path, visibility, web_url, raw_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, integrationId, group.gitlabGroupId(), group.fullPath(), group.name(), group.path(),
                group.visibility(), group.webUrl(), group.rawJson());
        }
    }

    public void replaceProjects(Long integrationId, List<GitLabProjectResponse> projects) {
        jdbcTemplate.update("DELETE FROM gitlab_projects WHERE integration_id = ?", integrationId);
        for (GitLabProjectResponse project : projects) {
            jdbcTemplate.update("""
                INSERT INTO gitlab_projects (
                    integration_id, gitlab_project_id, namespace_full_path, name, path, path_with_namespace,
                    default_branch, visibility, web_url, http_url_to_repo, ssh_url_to_repo, archived, empty_repo, raw_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, integrationId, project.gitlabProjectId(), project.namespaceFullPath(), project.name(), project.path(),
                project.pathWithNamespace(), project.defaultBranch(), project.visibility(), project.webUrl(),
                project.httpUrlToRepo(), project.sshUrlToRepo(), project.archived(), project.emptyRepo(), project.rawJson());
        }
    }

    public List<GitLabGroupResponse> findGroups(Long integrationId) {
        return jdbcTemplate.query("""
            SELECT gitlab_group_id, name, path, full_path, visibility, web_url, raw_json
            FROM gitlab_groups
            WHERE integration_id = ?
            ORDER BY full_path
            """, (rs, rowNum) -> new GitLabGroupResponse(
            rs.getLong("gitlab_group_id"),
            rs.getString("name"),
            rs.getString("path"),
            rs.getString("full_path"),
            rs.getString("visibility"),
            rs.getString("web_url"),
            rs.getString("raw_json")
        ), integrationId);
    }

    public List<GitLabProjectResponse> findProjects(Long integrationId) {
        return jdbcTemplate.query("""
            SELECT gitlab_project_id, name, path, path_with_namespace, namespace_full_path, default_branch,
                   visibility, web_url, http_url_to_repo, ssh_url_to_repo, archived, empty_repo, raw_json
            FROM gitlab_projects
            WHERE integration_id = ?
            ORDER BY path_with_namespace
            """, (rs, rowNum) -> new GitLabProjectResponse(
            rs.getLong("gitlab_project_id"),
            rs.getString("name"),
            rs.getString("path"),
            rs.getString("path_with_namespace"),
            rs.getString("namespace_full_path"),
            rs.getString("default_branch"),
            rs.getString("visibility"),
            rs.getString("web_url"),
            rs.getString("http_url_to_repo"),
            rs.getString("ssh_url_to_repo"),
            rs.getObject("archived") == null ? null : rs.getBoolean("archived"),
            rs.getObject("empty_repo") == null ? null : rs.getBoolean("empty_repo"),
            rs.getString("raw_json")
        ), integrationId);
    }

    public void deleteByIntegrationId(Long integrationId) {
        jdbcTemplate.update("DELETE FROM gitlab_projects WHERE integration_id = ?", integrationId);
        jdbcTemplate.update("DELETE FROM gitlab_groups WHERE integration_id = ?", integrationId);
    }
}
