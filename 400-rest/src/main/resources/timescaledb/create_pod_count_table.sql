-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- ACTIVE_POD_COUNT TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS ACTIVE_POD_COUNT (
  STARTTIME TIMESTAMPTZ NOT NULL,
  ENDTIME TIMESTAMPTZ NOT NULL,
	ACCOUNTID TEXT NOT NULL,
	CLUSTERID TEXT NOT NULL,
	INSTANCEID TEXT NOT NULL,
	PODCOUNT DOUBLE PRECISION
);
COMMIT;
SELECT CREATE_HYPERTABLE('ACTIVE_POD_COUNT','starttime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS ACTIVE_POD_COUNT_ACCOUNTID_INDEX ON ACTIVE_POD_COUNT(ACCOUNTID, CLUSTERID, INSTANCEID, STARTTIME DESC);
COMMIT;

---------- ACTIVE_POD_COUNT TABLE END ------------
