ALTER TABLE audit_logs ADD COLUMN context_id UUID;
CREATE INDEX idx_audit_logs_context_id ON audit_logs(context_id);
