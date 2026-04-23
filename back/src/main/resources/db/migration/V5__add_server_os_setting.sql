INSERT INTO app_config (key, value) VALUES ('server_os', 'linux') ON CONFLICT (key) DO NOTHING;
