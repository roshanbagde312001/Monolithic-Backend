INSERT INTO roles (id, code, name, description)
VALUES
    (1, 'SUPER_ADMIN', 'Super Admin', 'Full platform access'),
    (2, 'SECURITY_ANALYST', 'Security Analyst', 'Security operations access'),
    (3, 'VIEWER', 'Viewer', 'Read-only access')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (id, code, name, description, module_name)
VALUES
    (1, 'USER_READ', 'Read Users', 'View users and profiles', 'USER'),
    (2, 'USER_WRITE', 'Manage Users', 'Create and update users', 'USER'),
    (3, 'ROLE_READ', 'Read Roles', 'View roles', 'RBAC'),
    (4, 'ROLE_WRITE', 'Manage Roles', 'Create and update roles', 'RBAC'),
    (5, 'PERMISSION_READ', 'Read Permissions', 'View permissions', 'RBAC'),
    (6, 'PERMISSION_WRITE', 'Manage Permissions', 'Create permissions', 'RBAC'),
    (7, 'VIEW_READ', 'Read Views', 'View routes and modules', 'RBAC'),
    (8, 'VIEW_WRITE', 'Manage Views', 'Create route definitions', 'RBAC'),
    (9, 'INTEGRATION_READ', 'Read Integrations', 'View OEM integrations', 'INTEGRATION'),
    (10, 'INTEGRATION_WRITE', 'Manage Integrations', 'Create and update OEM integrations', 'INTEGRATION'),
    (11, 'LICENSE_READ', 'Read Licenses', 'View installed license details and usage', 'LICENSE'),
    (12, 'LICENSE_WRITE', 'Manage Licenses', 'Install and update offline licenses', 'LICENSE'),
    (13, 'LICENSE_AUDIT_READ', 'Read License Audit', 'View license audit trail', 'LICENSE'),
    (14, 'LICENSE_REPORT_READ', 'Read License Reports', 'View license utilization reports', 'LICENSE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO views (id, code, name, route, description)
VALUES
    (1, 'DASHBOARD', 'Dashboard', '/dashboard', 'Main dashboard'),
    (2, 'USER_MANAGEMENT', 'User Management', '/users', 'User administration'),
    (3, 'RBAC_MANAGEMENT', 'RBAC Management', '/rbac', 'Roles, permissions and views'),
    (4, 'INTEGRATION_MANAGEMENT', 'Integration Management', '/integrations', 'OEM integration management'),
    (5, 'LICENSE_MANAGEMENT', 'License Management', '/licenses', 'Offline license administration')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14),
    (2, 1), (2, 3), (2, 5), (2, 7), (2, 9), (2, 11), (2, 14),
    (3, 1), (3, 3), (3, 5), (3, 7), (3, 9)
ON CONFLICT DO NOTHING;

INSERT INTO role_views (role_id, view_id)
VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
    (2, 1), (2, 4), (2, 5),
    (3, 1)
ON CONFLICT DO NOTHING;
