-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- BILLING_DATA_HOURLY_AGGREGATED TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED (
                                                       STARTTIME TIMESTAMPTZ NOT NULL,
                                                       ENDTIME TIMESTAMPTZ NOT NULL,
                                                       ACCOUNTID TEXT NOT NULL,
                                                       INSTANCETYPE TEXT NOT NULL,
                                                       CLUSTERNAME TEXT,
                                                       BILLINGAMOUNT DOUBLE PRECISION  NOT NULL,
                                                       ACTUALIDLECOST DOUBLE PRECISION,
                                                       UNALLOCATEDCOST DOUBLE PRECISION,
                                                       SYSTEMCOST DOUBLE PRECISION,
                                                       CLUSTERID TEXT,
                                                       CLUSTERTYPE TEXT,
                                                       REGION TEXT,
                                                       WORKLOADNAME TEXT,
                                                       WORKLOADTYPE TEXT,
                                                       NAMESPACE TEXT,
                                                       APPID TEXT,
                                                       SERVICEID TEXT,
                                                       ENVID TEXT,
                                                       CLOUDPROVIDERID TEXT,
                                                       LAUNCHTYPE TEXT,
                                                       CLOUDSERVICENAME TEXT,
                                                       STORAGECOST DOUBLE PRECISION,
                                                       MEMORYBILLINGAMOUNT DOUBLE PRECISION,
                                                       CPUBILLINGAMOUNT DOUBLE PRECISION,
                                                       STORAGEACTUALIDLECOST DOUBLE PRECISION,
                                                       CPUACTUALIDLECOST DOUBLE PRECISION,
                                                       MEMORYACTUALIDLECOST DOUBLE PRECISION,
                                                       STORAGEUNALLOCATEDCOST DOUBLE PRECISION,
                                                       MEMORYUNALLOCATEDCOST DOUBLE PRECISION,
                                                       CPUUNALLOCATEDCOST DOUBLE PRECISION,
                                                       STORAGEREQUEST DOUBLE PRECISION,
                                                       STORAGEUTILIZATIONVALUE DOUBLE PRECISION,
                                                       INSTANCEID TEXT
);
COMMIT;
SELECT CREATE_HYPERTABLE('BILLING_DATA_HOURLY_AGGREGATED','starttime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_ACCOUNTID_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_APPID_COMPOSITE_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, APPID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_WORKLOADNAME_COMPOSITE_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, CLUSTERID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_NAMESPACE_COMPOSITE_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, CLUSTERID, NAMESPACE, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_CLUSTERID_COMPOSITE_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, CLUSTERID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_WORKLOADNAME_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_AGGREGATED_NAMESPACE_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_HOURLY_AGGREGATED(ACCOUNTID, NAMESPACE, STARTTIME DESC);
COMMIT;
---------- BILLING_DATA_HOURLY_AGGREGATED TABLE END ------------

BEGIN;
ALTER TABLE BILLING_DATA_AGGREGATED ADD COLUMN IF NOT EXISTS INSTANCEID TEXT;
COMMIT;
