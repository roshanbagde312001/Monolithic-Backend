package com.appdefend.backend.service;

import com.appdefend.backend.model.AppView;
import com.appdefend.backend.model.Permission;
import com.appdefend.backend.model.Role;
import com.appdefend.backend.repository.RbacRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RbacService {
    private final RbacRepository rbacRepository;

    public RbacService(RbacRepository rbacRepository) {
        this.rbacRepository = rbacRepository;
    }

    public List<Role> findRoles() {
        return rbacRepository.findRoles();
    }

    public List<Permission> findPermissions() {
        return rbacRepository.findPermissions();
    }

    public List<AppView> findViews() {
        return rbacRepository.findViews();
    }

    public Long createRole(String code, String name, String description) {
        return rbacRepository.createRole(code, name, description);
    }

    public Long createPermission(String code, String name, String description, String moduleName) {
        return rbacRepository.createPermission(code, name, description, moduleName);
    }

    public Long createView(String code, String name, String route, String description) {
        return rbacRepository.createView(code, name, route, description);
    }

    public void assignPermissionToRole(Long roleId, Long permissionId) {
        rbacRepository.assignPermissionToRole(roleId, permissionId);
    }

    public void assignViewToRole(Long roleId, Long viewId) {
        rbacRepository.assignViewToRole(roleId, viewId);
    }

    public void updateRole(Long id, String code, String name, String description) {
        rbacRepository.updateRole(id, code, name, description);
    }

    public void updatePermission(Long id, String code, String name, String description, String moduleName) {
        rbacRepository.updatePermission(id, code, name, description, moduleName);
    }

    public void updateView(Long id, String code, String name, String route, String description) {
        rbacRepository.updateView(id, code, name, route, description);
    }

    public void deleteRole(Long id) {
        rbacRepository.deleteRole(id);
    }

    public void deletePermission(Long id) {
        rbacRepository.deletePermission(id);
    }

    public void deleteView(Long id) {
        rbacRepository.deleteView(id);
    }
}
