CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    action          VARCHAR(50) NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    user_id         UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
