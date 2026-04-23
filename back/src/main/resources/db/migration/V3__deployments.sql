CREATE TABLE deployments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id         UUID NOT NULL REFERENCES hosts(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    logs            TEXT,
    log_file_path   VARCHAR(500),
    timeout         INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deployments_host_id ON deployments(host_id);
CREATE INDEX idx_deployments_user_id ON deployments(user_id);
CREATE INDEX idx_deployments_status ON deployments(status);
CREATE INDEX idx_deployments_created_at ON deployments(created_at DESC);
