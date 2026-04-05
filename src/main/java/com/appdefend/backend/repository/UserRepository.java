package com.appdefend.backend.repository;

import com.appdefend.backend.model.PlatformUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PlatformUser> rowMapper = this::mapUser;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PlatformUser> findByEmail(String email) {
        List<PlatformUser> users = jdbcTemplate.query("""
            SELECT id, full_name, email, password_hash, enabled, created_at, updated_at
            FROM app_users
            WHERE email = ?
            """, rowMapper, email);
        return users.stream().findFirst().map(this::enrichUser);
    }

    public Optional<PlatformUser> findById(Long id) {
        List<PlatformUser> users = jdbcTemplate.query("""
            SELECT id, full_name, email, password_hash, enabled, created_at, updated_at
            FROM app_users
            WHERE id = ?
            """, rowMapper, id);
        return users.stream().findFirst().map(this::enrichUser);
    }

    public List<PlatformUser> findAll() {
        return jdbcTemplate.query("""
            SELECT id, full_name, email, password_hash, enabled, created_at, updated_at
            FROM app_users
            ORDER BY id
            """, rowMapper).stream().map(this::enrichUser).toList();
    }

    public Long create(String fullName, String email, String passwordHash, boolean enabled) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO app_users (full_name, email, password_hash, enabled)
                VALUES (?, ?, ?, ?)
                """, new String[]{"id"});
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setBoolean(4, enabled);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void assignRole(Long userId, Long roleId) {
        jdbcTemplate.update("""
            INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """, userId, roleId);
    }

    public void clearRoles(Long userId) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
    }

    public void update(Long userId, String fullName, boolean enabled) {
        jdbcTemplate.update("""
            UPDATE app_users
            SET full_name = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, fullName, enabled, userId);
    }

    public void delete(Long userId) {
        jdbcTemplate.update("DELETE FROM app_users WHERE id = ?", userId);
    }

    public List<String> findRoleCodesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
            SELECT r.code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
            ORDER BY r.code
            """, String.class, userId);
    }

    public List<String> findPermissionCodesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT p.code
            FROM permissions p
            JOIN role_permissions rp ON rp.permission_id = p.id
            JOIN user_roles ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = ?
            ORDER BY p.code
            """, String.class, userId);
    }

    public List<String> findViewCodesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT v.code
            FROM views v
            JOIN role_views rv ON rv.view_id = v.id
            JOIN user_roles ur ON ur.role_id = rv.role_id
            WHERE ur.user_id = ?
            ORDER BY v.code
            """, String.class, userId);
    }

    public int countEnabledUsers() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_users WHERE enabled = true", Integer.class);
        return count == null ? 0 : count;
    }

    private PlatformUser enrichUser(PlatformUser user) {
        return new PlatformUser(
            user.id(),
            user.fullName(),
            user.email(),
            user.passwordHash(),
            user.enabled(),
            user.createdAt(),
            user.updatedAt(),
            findRoleCodesByUserId(user.id()),
            findPermissionCodesByUserId(user.id()),
            findViewCodesByUserId(user.id())
        );
    }

    private PlatformUser mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new PlatformUser(
            rs.getLong("id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("enabled"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime(),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
