CREATE TABLE hosts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    ip                   VARCHAR(100) NOT NULL,
    domain               VARCHAR(255),
    deployment_command   TEXT,
    generate_command     TEXT,
    deliver_command      TEXT,
    default_timeout      INTEGER,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP
);

CREATE TABLE user_host_permissions (
    user_id     UUID NOT NULL REFERENCES users(id),
    host_id     UUID NOT NULL REFERENCES hosts(id),
    can_deploy  BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, host_id)
);
