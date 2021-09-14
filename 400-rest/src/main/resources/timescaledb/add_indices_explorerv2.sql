-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_WORKLOADNAME_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_HOURLY(ACCOUNTID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_HOURLY_NAMESPACE_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_HOURLY(ACCOUNTID, NAMESPACE, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_WORKLOADNAME_WITHOUT_CLUSTER_INDEX ON BILLING_DATA(ACCOUNTID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_NAMESPACE_WITHOUT_CLUSTER_INDEX ON BILLING_DATA(ACCOUNTID, NAMESPACE, STARTTIME DESC);
COMMIT;
