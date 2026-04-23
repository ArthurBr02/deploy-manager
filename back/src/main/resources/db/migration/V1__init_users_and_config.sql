CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',
    avatar      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE TABLE app_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT
);

INSERT INTO app_config (key, value) VALUES
    ('default_deploy_command', 'sh /root/{host}/liv.sh'),
    ('default_timeout', '10'),
    ('smtp_host', ''),
    ('smtp_port', ''),
    ('smtp_username', ''),
    ('smtp_password', ''),
    ('smtp_from', '');

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
