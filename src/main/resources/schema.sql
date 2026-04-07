CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    module_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS views (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    route VARCHAR(200) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS role_views (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    view_id BIGINT NOT NULL REFERENCES views(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, view_id)
);

CREATE TABLE IF NOT EXISTS integrations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    deployment_mode VARCHAR(40),
    namespace_path VARCHAR(255),
    base_url VARCHAR(255) NOT NULL,
    credentials_json TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE integrations ADD COLUMN IF NOT EXISTS deployment_mode VARCHAR(40);
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS namespace_path VARCHAR(255);

CREATE TABLE IF NOT EXISTS gitlab_groups (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
    gitlab_group_id BIGINT NOT NULL,
    full_path VARCHAR(255),
    name VARCHAR(255),
    path VARCHAR(255),
    visibility VARCHAR(40),
    web_url VARCHAR(500),
    parent_id BIGINT,
    raw_json TEXT NOT NULL,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (integration_id, gitlab_group_id)
);

CREATE TABLE IF NOT EXISTS gitlab_projects (
    id BIGSERIAL PRIMARY KEY,
    integration_id BIGINT NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
    gitlab_project_id BIGINT NOT NULL,
    namespace_id BIGINT,
    namespace_full_path VARCHAR(255),
    name VARCHAR(255),
    path VARCHAR(255),
    path_with_namespace VARCHAR(500),
    default_branch VARCHAR(255),
    visibility VARCHAR(40),
    web_url VARCHAR(500),
    http_url_to_repo VARCHAR(500),
    ssh_url_to_repo VARCHAR(500),
    archived BOOLEAN,
    empty_repo BOOLEAN,
    raw_json TEXT NOT NULL,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (integration_id, gitlab_project_id)
);

CREATE TABLE IF NOT EXISTS licenses (
    id BIGSERIAL PRIMARY KEY,
    license_id VARCHAR(120) NOT NULL UNIQUE,
    customer_name VARCHAR(200) NOT NULL,
    customer_email VARCHAR(150),
    deployment_id VARCHAR(150) NOT NULL,
    license_tier VARCHAR(80) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    grace_period_days INTEGER NOT NULL DEFAULT 0,
    max_named_users INTEGER NOT NULL,
    features_json TEXT NOT NULL,
    metadata_json TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    signature VARCHAR(4096) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS license_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    event_status VARCHAR(40) NOT NULL,
    license_id VARCHAR(120),
    actor VARCHAR(150) NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    details_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permissions_module_name ON permissions(module_name);
CREATE INDEX IF NOT EXISTS idx_integrations_provider_type ON integrations(provider_type);
CREATE INDEX IF NOT EXISTS idx_integrations_namespace_path ON integrations(namespace_path);
CREATE INDEX IF NOT EXISTS idx_gitlab_groups_integration_id ON gitlab_groups(integration_id);
CREATE INDEX IF NOT EXISTS idx_gitlab_projects_integration_id ON gitlab_projects(integration_id);
CREATE INDEX IF NOT EXISTS idx_licenses_active ON licenses(active);
CREATE INDEX IF NOT EXISTS idx_license_audit_created_at ON license_audit_log(created_at);
