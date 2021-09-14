-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

---------- INSTANCE_STATS TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS INSTANCE_STATS (
	REPORTEDAT TIMESTAMP NOT NULL,
	ACCOUNTID TEXT,
	APPID TEXT,
	SERVICEID TEXT,
	ENVID TEXT,
	CLOUDPROVIDERID TEXT,
	INSTANCETYPE TEXT,
	INSTANCECOUNT INTEGER,
	ARTIFACTID TEXT
);
COMMIT;
SELECT CREATE_HYPERTABLE('INSTANCE_STATS','reportedat',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_APPID_INDEX ON INSTANCE_STATS(APPID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_ACCOUNTID_INDEX ON INSTANCE_STATS(ACCOUNTID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_SERVICEID_INDEX ON INSTANCE_STATS(SERVICEID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_ENVID_INDEX ON INSTANCE_STATS(ENVID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_CLOUDPROVIDERID_INDEX ON INSTANCE_STATS(CLOUDPROVIDERID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_INSTANCECOUNT_INDEX ON INSTANCE_STATS(INSTANCECOUNT,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_ARTIFACTID_INDEX ON INSTANCE_STATS(ARTIFACTID,REPORTEDAT DESC);
COMMIT;

---------- INSTANCE_STATS TABLE END ------------
