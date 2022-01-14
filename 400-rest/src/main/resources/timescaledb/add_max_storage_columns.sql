BEGIN;

ALTER TABLE utilization_data ADD COLUMN IF NOT EXISTS maxstoragerequestvalue double precision DEFAULT '0';
ALTER TABLE utilization_data ADD COLUMN IF NOT EXISTS maxstorageusagevalue double precision DEFAULT '0';

ALTER TABLE billing_data_aggregated ADD COLUMN IF NOT EXISTS maxstoragerequest double precision DEFAULT '0';
ALTER TABLE billing_data_aggregated ADD COLUMN IF NOT EXISTS maxstorageutilizationvalue double precision DEFAULT '0';

ALTER TABLE billing_data_hourly_aggregated ADD COLUMN IF NOT EXISTS maxstoragerequest double precision DEFAULT '0';
ALTER TABLE billing_data_hourly_aggregated ADD COLUMN IF NOT EXISTS maxstorageutilizationvalue double precision DEFAULT '0';

ALTER TABLE billing_data ADD COLUMN IF NOT EXISTS maxstoragerequest double precision DEFAULT '0';
ALTER TABLE billing_data ADD COLUMN IF NOT EXISTS maxstorageutilizationvalue double precision DEFAULT '0';

ALTER TABLE billing_data_hourly ADD COLUMN IF NOT EXISTS maxstoragerequest double precision DEFAULT '0';
ALTER TABLE billing_data_hourly ADD COLUMN IF NOT EXISTS maxstorageutilizationvalue double precision DEFAULT '0';

COMMIT;