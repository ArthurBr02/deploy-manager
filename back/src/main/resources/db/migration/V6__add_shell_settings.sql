INSERT INTO app_config (key, value) VALUES ('shell_linux_bin', '/bin/sh') ON CONFLICT (key) DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('shell_linux_arg', '-c') ON CONFLICT (key) DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('shell_windows_bin', 'cmd.exe') ON CONFLICT (key) DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('shell_windows_arg', '/c') ON CONFLICT (key) DO NOTHING;
