package com.appdefend.backend.repository;

import com.appdefend.backend.model.AppView;
import com.appdefend.backend.model.Permission;
import com.appdefend.backend.model.Role;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RbacRepository {
    private final JdbcTemplate jdbcTemplate;

    public RbacRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Role> findRoles() {
        return jdbcTemplate.query("SELECT id, code, name, description FROM roles ORDER BY id",
            (rs, rowNum) -> new Role(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getString("description")));
    }

    public List<Permission> findPermissions() {
        return jdbcTemplate.query("SELECT id, code, name, description, module_name FROM permissions ORDER BY id",
            (rs, rowNum) -> new Permission(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("description"), rs.getString("module_name")));
    }

    public List<AppView> findViews() {
        return jdbcTemplate.query("SELECT id, code, name, route, description FROM views ORDER BY id",
            (rs, rowNum) -> new AppView(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("route"), rs.getString("description")));
    }

    public Long createRole(String code, String name, String description) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                "INSERT INTO roles (code, name, description) VALUES (?, ?, ?)", new String[]{"id"});
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, description);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Long createPermission(String code, String name, String description, String moduleName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                "INSERT INTO permissions (code, name, description, module_name) VALUES (?, ?, ?, ?)", new String[]{"id"});
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, moduleName);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Long createView(String code, String name, String route, String description) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                "INSERT INTO views (code, name, route, description) VALUES (?, ?, ?, ?)", new String[]{"id"});
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, route);
            ps.setString(4, description);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void assignPermissionToRole(Long roleId, Long permissionId) {
        jdbcTemplate.update("""
            INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """, roleId, permissionId);
    }

    public void assignViewToRole(Long roleId, Long viewId) {
        jdbcTemplate.update("""
            INSERT INTO role_views (role_id, view_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """, roleId, viewId);
    }

    public void updateRole(Long id, String code, String name, String description) {
        jdbcTemplate.update("UPDATE roles SET code = ?, name = ?, description = ? WHERE id = ?", code, name, description, id);
    }

    public void updatePermission(Long id, String code, String name, String description, String moduleName) {
        jdbcTemplate.update("UPDATE permissions SET code = ?, name = ?, description = ?, module_name = ? WHERE id = ?",
            code, name, description, moduleName, id);
    }

    public void updateView(Long id, String code, String name, String route, String description) {
        jdbcTemplate.update("UPDATE views SET code = ?, name = ?, route = ?, description = ? WHERE id = ?",
            code, name, route, description, id);
    }

    public void deleteRole(Long id) {
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", id);
    }

    public void deletePermission(Long id) {
        jdbcTemplate.update("DELETE FROM permissions WHERE id = ?", id);
    }

    public void deleteView(Long id) {
        jdbcTemplate.update("DELETE FROM views WHERE id = ?", id);
    }
}
