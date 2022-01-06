-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

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
