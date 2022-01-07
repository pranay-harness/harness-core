-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- DEPLOYMENT TABLE START -----------
BEGIN;
CREATE TABLE IF NOT EXISTS DEPLOYMENT (
    EXECUTIONID TEXT NOT NULL,
    STARTTIME TIMESTAMP NOT NULL,
    ENDTIME TIMESTAMP NOT NULL,
    ACCOUNTID TEXT NOT NULL,
    APPID TEXT NOT NULL,
    TRIGGERED_BY TEXT,
    TRIGGER_ID TEXT,
    STATUS VARCHAR(20),
    SERVICES TEXT[],
    WORKFLOWS TEXT[],
    CLOUDPROVIDERS TEXT[],
    ENVIRONMENTS TEXT[],
    PIPELINE TEXT,
    DURATION BIGINT NOT NULL,
    ARTIFACTS TEXT[]
);
COMMIT;


SELECT CREATE_HYPERTABLE('DEPLOYMENT','endtime',if_not_exists => TRUE);
BEGIN;
CREATE INDEX IF NOT EXISTS DEPLOYMENT_EXECUTIONID_INDEX ON DEPLOYMENT(EXECUTIONID,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_TRIGGERED_BY_INDEX ON DEPLOYMENT(TRIGGERED_BY,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_TRIGGER_ID_INDEX ON DEPLOYMENT(TRIGGER_ID,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_APPID_INDEX ON DEPLOYMENT(APPID,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_ACCOUNTID_INDEX ON DEPLOYMENT(ACCOUNTID,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_PIPELINE_INDEX ON DEPLOYMENT(PIPELINE,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_STATUS_INDEX ON DEPLOYMENT(STATUS,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_STARTTIME_INDEX ON DEPLOYMENT(STARTTIME,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_DURATION_INDEX ON DEPLOYMENT(DURATION,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_SERVICES_GIN_INDEX ON DEPLOYMENT USING GIN(SERVICES);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_WORKFLOWS_GIN_INDEX ON DEPLOYMENT USING GIN(WORKFLOWS);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_ARTIFACTS_GIN_INDEX ON DEPLOYMENT USING GIN(ARTIFACTS);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_CLOUDPROVIDERS_GIN_INDEX ON DEPLOYMENT USING GIN(CLOUDPROVIDERS);
CREATE INDEX IF NOT EXISTS DEPLOYMENT_ENVIRONMENTS_GIN_INDEX ON DEPLOYMENT USING GIN(ENVIRONMENTS);
COMMIT;

---------- DEPLOYMENT TABLE END -----------


---------- INSTANCE TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS INSTANCE (
	"TIME" TIMESTAMP NOT NULL,
	ACCOUNTID TEXT,
	APPID TEXT,
	SERVICEID TEXT,
	ENVID TEXT,
	CLOUDPROVIDERID TEXT,
	INSTANCECOUNT INTEGER,
	ARTIFACTID TEXT
);
COMMIT;
SELECT CREATE_HYPERTABLE('INSTANCE','TIME',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS INSTANCE_APPID_INDEX ON INSTANCE(APPID,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_ACCOUNTID_INDEX ON INSTANCE(ACCOUNTID,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_SERVICEID_INDEX ON INSTANCE(SERVICEID,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_ENVID_INDEX ON INSTANCE(ENVID,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_CLOUDPROVIDERID_INDEX ON INSTANCE(CLOUDPROVIDERID,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_INSTANCECOUNT_INDEX ON INSTANCE(INSTANCECOUNT,"TIME" DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_ARTIFACTID_INDEX ON INSTANCE(ARTIFACTID,"TIME" DESC);
COMMIT;

---------- INSTANCE TABLE END ------------
