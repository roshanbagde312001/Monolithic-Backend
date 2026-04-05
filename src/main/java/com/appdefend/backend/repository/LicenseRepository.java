package com.appdefend.backend.repository;

import com.appdefend.backend.model.OfflineLicense;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LicenseRepository {
    private final JdbcTemplate jdbcTemplate;

    public LicenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<OfflineLicense> findActiveLicense() {
        List<OfflineLicense> licenses = jdbcTemplate.query("""
            SELECT id, license_id, customer_name, customer_email, deployment_id, license_tier,
                   valid_from, valid_until, grace_period_days, max_named_users, features_json,
                   metadata_json, payload_json, signature, active, created_at, installed_at
            FROM licenses
            WHERE active = true
            ORDER BY installed_at DESC
            LIMIT 1
            """, (rs, rowNum) -> new OfflineLicense(
            rs.getLong("id"),
            rs.getString("license_id"),
            rs.getString("customer_name"),
            rs.getString("customer_email"),
            rs.getString("deployment_id"),
            rs.getString("license_tier"),
            rs.getDate("valid_from").toLocalDate(),
            rs.getDate("valid_until").toLocalDate(),
            rs.getInt("grace_period_days"),
            rs.getInt("max_named_users"),
            rs.getString("features_json"),
            rs.getString("metadata_json"),
            rs.getString("payload_json"),
            rs.getString("signature"),
            rs.getBoolean("active"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("installed_at").toLocalDateTime()
        ));
        return licenses.stream().findFirst();
    }

    public Long insert(OfflineLicense license) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO licenses (license_id, customer_name, customer_email, deployment_id, license_tier,
                valid_from, valid_until, grace_period_days, max_named_users, features_json, metadata_json,
                payload_json, signature, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new String[]{"id"});
            ps.setString(1, license.licenseId());
            ps.setString(2, license.customerName());
            ps.setString(3, license.customerEmail());
            ps.setString(4, license.deploymentId());
            ps.setString(5, license.licenseTier());
            ps.setDate(6, java.sql.Date.valueOf(license.validFrom()));
            ps.setDate(7, java.sql.Date.valueOf(license.validUntil()));
            ps.setInt(8, license.gracePeriodDays());
            ps.setInt(9, license.maxNamedUsers());
            ps.setString(10, license.featuresJson());
            ps.setString(11, license.metadataJson());
            ps.setString(12, license.payloadJson());
            ps.setString(13, license.signature());
            ps.setBoolean(14, true);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void deactivateOtherLicenses(Long activeLicenseId) {
        jdbcTemplate.update("UPDATE licenses SET active = false WHERE id <> ?", activeLicenseId);
    }
}
