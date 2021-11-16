BEGIN;

ALTER TABLE billing_data_aggregated ADD COLUMN IF NOT EXISTS orgidentifier  TEXT NULL;
ALTER TABLE billing_data_aggregated ADD COLUMN IF NOT EXISTS projectidentifier TEXT NULL;

ALTER TABLE billing_data_hourly_aggregated ADD COLUMN IF NOT EXISTS orgidentifier TEXT NULL;
ALTER TABLE billing_data_hourly_aggregated ADD COLUMN IF NOT EXISTS projectidentifier TEXT NULL;

ALTER TABLE billing_data ADD COLUMN IF NOT EXISTS orgidentifier TEXT NULL;
ALTER TABLE billing_data ADD COLUMN IF NOT EXISTS projectidentifier TEXT NULL;

ALTER TABLE billing_data_hourly ADD COLUMN IF NOT EXISTS orgidentifier TEXT NULL;
ALTER TABLE billing_data_hourly ADD COLUMN IF NOT EXISTS projectidentifier TEXT NULL;

COMMIT;