---------- BILLING_DATA_AGGREGATED TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS BILLING_DATA_AGGREGATED (
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
    CLOUDSERVICENAME TEXT
);
COMMIT;
SELECT CREATE_HYPERTABLE('BILLING_DATA_AGGREGATED','starttime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_ACCOUNTID_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_APPID_COMPOSITE_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, APPID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_WORKLOADNAME_COMPOSITE_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, CLUSTERID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_NAMESPACE_COMPOSITE_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, CLUSTERID, NAMESPACE, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_CLUSTERID_COMPOSITE_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, CLUSTERID, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_WORKLOADNAME_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, WORKLOADNAME, STARTTIME DESC);
CREATE INDEX IF NOT EXISTS BILLING_DATA_AGGREGATED_NAMESPACE_WITHOUT_CLUSTER_INDEX ON BILLING_DATA_AGGREGATED(ACCOUNTID, NAMESPACE, STARTTIME DESC);
COMMIT;
---------- BILLING_DATA_AGGREGATED TABLE END ------------