-- Add audit fields to users
ALTER TABLE users ADD COLUMN created_by UUID;
ALTER TABLE users ADD COLUMN updated_by UUID;
ALTER TABLE users ADD COLUMN deleted_by UUID;

-- Add audit fields to hosts
ALTER TABLE hosts ADD COLUMN created_by UUID;
ALTER TABLE hosts ADD COLUMN updated_by UUID;
ALTER TABLE hosts ADD COLUMN deleted_by UUID;

-- Add audit fields to deployments
ALTER TABLE deployments ADD COLUMN created_by UUID;
ALTER TABLE deployments ADD COLUMN updated_by UUID;
ALTER TABLE deployments ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add audit fields to app_config
ALTER TABLE app_config ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_config ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_config ADD COLUMN created_by UUID;
ALTER TABLE app_config ADD COLUMN updated_by UUID;

-- Add audit fields to user_host_permissions
ALTER TABLE user_host_permissions ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_host_permissions ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_host_permissions ADD COLUMN created_by UUID;
ALTER TABLE user_host_permissions ADD COLUMN updated_by UUID;

-- Add audit fields to password_reset_tokens
ALTER TABLE password_reset_tokens ADD COLUMN created_by UUID;
