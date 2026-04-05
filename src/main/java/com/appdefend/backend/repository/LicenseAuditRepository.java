package com.appdefend.backend.repository;

import com.appdefend.backend.model.LicenseAuditEvent;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LicenseAuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public LicenseAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(String eventType, String eventStatus, String licenseId, String actor, String moduleName, String detailsJson) {
        jdbcTemplate.update("""
            INSERT INTO license_audit_log (event_type, event_status, license_id, actor, module_name, details_json)
            VALUES (?, ?, ?, ?, ?, ?)
            """, eventType, eventStatus, licenseId, actor, moduleName, detailsJson);
    }

    public List<LicenseAuditEvent> findRecent(int limit) {
        return jdbcTemplate.query("""
            SELECT id, event_type, event_status, license_id, actor, module_name, details_json, created_at
            FROM license_audit_log
            ORDER BY created_at DESC
            LIMIT ?
            """, (rs, rowNum) -> new LicenseAuditEvent(
            rs.getLong("id"),
            rs.getString("event_type"),
            rs.getString("event_status"),
            rs.getString("license_id"),
            rs.getString("actor"),
            rs.getString("module_name"),
            rs.getString("details_json"),
            rs.getTimestamp("created_at").toLocalDateTime()
        ), limit);
    }
}
