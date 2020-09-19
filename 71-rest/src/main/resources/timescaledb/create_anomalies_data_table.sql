---------- ANOMALIES TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS ANOMALIES (
  ANOMALYTIME TIMESTAMPTZ NOT NULL,
	ACCOUNTID TEXT NOT NULL,
	TIMEGRANULARITY TEXT NOT NULL,
	ENTITYID TEXT NOT NULL,
	ENTITYTYPE TEXT,
	CLUSTERID TEXT,
	CLUSTERNAME TEXT,
	WORKLOADNAME TEXT,
	WORKLOADTYPE TEXT,
	NAMESPACE TEXT,
	CLOUDPROVIDERID TEXT,
	ANOMALYSCORE DOUBLE PRECISION,
	ANOMALYTYPE TEXT NOT NULL,
	REPORTEDBY TEXT,
	ABSOLUTETHRESHOLD BOOLEAN NOT NULL,
	RELATIVETHRESHOLD BOOLEAN NOT NULL,
	PROBABILISTICTHRESHOLD BOOLEAN NOT NULL
);
COMMIT;
SELECT CREATE_HYPERTABLE('ANOMALIES','anomalytime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS ANOMALY_ACCOUNTID_INDEX ON ANOMALIES(ACCOUNTID, ENTITYID ,ANOMALYTIME DESC);
COMMIT;

---------- ANOMALIES TABLE END ------------