---------- UTILIZATION_DATA TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS UTILIZATION_DATA (
	STARTTIME TIMESTAMPTZ NOT NULL,
	ENDTIME TIMESTAMPTZ NOT NULL,
	ACCOUNTID TEXT NOT NULL,
	SETTINGID TEXT NOT NULL,
	INSTANCEID TEXT NOT NULL,
	INSTANCETYPE TEXT NOT NULL,
	MAXCPU DOUBLE PRECISION  NOT NULL,
	MAXMEMORY DOUBLE PRECISION  NOT NULL,
	AVGCPU DOUBLE PRECISION  NOT NULL,
	AVGMEMORY DOUBLE PRECISION  NOT NULL,
	MAXCPUVALUE DOUBLE PRECISION,
	MAXMEMORYVALUE DOUBLE PRECISION,
	AVGCPUVALUE DOUBLE PRECISION,
	AVGMEMORYVALUE DOUBLE PRECISION
);
COMMIT;
SELECT CREATE_HYPERTABLE('UTILIZATION_DATA','starttime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS UTILIZATION_DATA_INSTANCEID_INDEX ON UTILIZATION_DATA(INSTANCEID, STARTTIME DESC);
COMMIT;
---------- UTILIZATION_DATA TABLE END ------------