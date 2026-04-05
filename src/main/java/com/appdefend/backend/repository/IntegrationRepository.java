package com.appdefend.backend.repository;

import com.appdefend.backend.model.IntegrationProviderType;
import com.appdefend.backend.model.OemIntegration;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class IntegrationRepository {
    private final JdbcTemplate jdbcTemplate;

    public IntegrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OemIntegration> findAll() {
        return jdbcTemplate.query("""
            SELECT id, name, provider_type, base_url, credentials_json, active, created_at, updated_at
            FROM integrations
            ORDER BY id
            """, (rs, rowNum) -> new OemIntegration(
            rs.getLong("id"),
            rs.getString("name"),
            IntegrationProviderType.valueOf(rs.getString("provider_type")),
            rs.getString("base_url"),
            rs.getString("credentials_json"),
            rs.getBoolean("active"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    public Optional<OemIntegration> findById(Long id) {
        List<OemIntegration> list = jdbcTemplate.query("""
            SELECT id, name, provider_type, base_url, credentials_json, active, created_at, updated_at
            FROM integrations
            WHERE id = ?
            """, (rs, rowNum) -> new OemIntegration(
            rs.getLong("id"),
            rs.getString("name"),
            IntegrationProviderType.valueOf(rs.getString("provider_type")),
            rs.getString("base_url"),
            rs.getString("credentials_json"),
            rs.getBoolean("active"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        ), id);
        return list.stream().findFirst();
    }

    public Long create(String name, IntegrationProviderType providerType, String baseUrl, String credentialsJson, boolean active) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO integrations (name, provider_type, base_url, credentials_json, active)
                VALUES (?, ?, ?, ?, ?)
                """, new String[]{"id"});
            ps.setString(1, name);
            ps.setString(2, providerType.name());
            ps.setString(3, baseUrl);
            ps.setString(4, credentialsJson);
            ps.setBoolean(5, active);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(Long id, String name, IntegrationProviderType providerType, String baseUrl, String credentialsJson, boolean active) {
        jdbcTemplate.update("""
            UPDATE integrations
            SET name = ?, provider_type = ?, base_url = ?, credentials_json = ?, active = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, name, providerType.name(), baseUrl, credentialsJson, active, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM integrations WHERE id = ?", id);
    }
}
