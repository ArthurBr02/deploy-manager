-- Configuration par défaut pour la commande tlog
INSERT INTO app_config (key, value) VALUES ('default_tlog_command', 'ssh root@{domain} tlog');

-- Ajout de la colonne tlog_command personnalisable sur l'hôte
ALTER TABLE hosts ADD COLUMN tlog_command TEXT;
