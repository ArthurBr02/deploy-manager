ALTER TABLE hosts ADD COLUMN dump_folder VARCHAR(255);

INSERT INTO app_config (key, value) VALUES ('mcp_enabled', 'true') ON CONFLICT (key) DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('default_dump_folder', '/var/www/dumps') ON CONFLICT (key) DO NOTHING;
