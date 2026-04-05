package com.appdefend.backend.controller;

import com.appdefend.backend.dto.RbacDtos.CreatePermissionRequest;
import com.appdefend.backend.dto.RbacDtos.CreateRoleRequest;
import com.appdefend.backend.dto.RbacDtos.CreateViewRequest;
import com.appdefend.backend.service.LicenseService;
import com.appdefend.backend.service.RbacService;
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
@RequestMapping("/api/v1/rbac")
@Tag(name = "RBAC", description = "Role, permission and view management APIs")
public class RbacController {
    private final RbacService rbacService;
    private final LicenseService licenseService;

    public RbacController(RbacService rbacService, LicenseService licenseService) {
        this.rbacService = rbacService;
        this.licenseService = licenseService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "List roles")
    public Object listRoles() {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return rbacService.findRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Create role")
    public Map<String, Long> createRole(@Valid @RequestBody CreateRoleRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return Map.of("id", rbacService.createRole(request.code(), request.name(), request.description()));
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Update role")
    public ResponseEntity<Void> updateRole(@PathVariable Long roleId, @Valid @RequestBody CreateRoleRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.updateRole(roleId, request.code(), request.name(), request.description());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Assign permission to role")
    public void assignPermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.assignPermissionToRole(roleId, permissionId);
    }

    @PostMapping("/roles/{roleId}/views/{viewId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Assign view to role")
    public void assignView(@PathVariable Long roleId, @PathVariable Long viewId) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.assignViewToRole(roleId, viewId);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "List permissions")
    public Object listPermissions() {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return rbacService.findPermissions();
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Create permission")
    public Map<String, Long> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return Map.of("id", rbacService.createPermission(
            request.code(), request.name(), request.description(), request.moduleName()));
    }

    @PutMapping("/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Update permission")
    public ResponseEntity<Void> updatePermission(@PathVariable Long permissionId, @Valid @RequestBody CreatePermissionRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.updatePermission(permissionId, request.code(), request.name(), request.description(), request.moduleName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/views")
    @PreAuthorize("hasAuthority('VIEW_READ')")
    @Operation(summary = "List views")
    public Object listViews() {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return rbacService.findViews();
    }

    @PostMapping("/views")
    @PreAuthorize("hasAuthority('VIEW_WRITE')")
    @Operation(summary = "Create view")
    public Map<String, Long> createView(@Valid @RequestBody CreateViewRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        return Map.of("id", rbacService.createView(
            request.code(), request.name(), request.route(), request.description()));
    }

    @PutMapping("/views/{viewId}")
    @PreAuthorize("hasAuthority('VIEW_WRITE')")
    @Operation(summary = "Update view")
    public ResponseEntity<Void> updateView(@PathVariable Long viewId, @Valid @RequestBody CreateViewRequest request) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.updateView(viewId, request.code(), request.name(), request.route(), request.description());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    @Operation(summary = "Delete role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('PERMISSION_WRITE')")
    @Operation(summary = "Delete permission")
    public ResponseEntity<Void> deletePermission(@PathVariable Long permissionId) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/views/{viewId}")
    @PreAuthorize("hasAuthority('VIEW_WRITE')")
    @Operation(summary = "Delete view")
    public ResponseEntity<Void> deleteView(@PathVariable Long viewId) {
        licenseService.assertFeatureEnabled("RBAC", "RBAC");
        rbacService.deleteView(viewId);
        return ResponseEntity.noContent().build();
    }
}
