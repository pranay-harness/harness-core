-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

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
