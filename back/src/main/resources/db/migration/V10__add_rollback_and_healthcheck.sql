ALTER TABLE hosts ADD COLUMN rollback_command TEXT;
ALTER TABLE hosts ADD COLUMN healthcheck_url VARCHAR(500);

-- Commentaire pour expliquer le défaut dynamique souhaité (géré côté Backend)
-- Par défaut, le healthcheck_url sera "https://{domain}" si non spécifié.
