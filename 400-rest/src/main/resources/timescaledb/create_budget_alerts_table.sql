-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

---------- BUDGET_ALERTS TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS BUDGET_ALERTS (
	BUDGETID TEXT NOT NULL,
	ACCOUNTID TEXT NOT NULL,
	ALERTTHRESHOLD DOUBLE PRECISION,
	ACTUALCOST DOUBLE PRECISION,
	BUDGETEDCOST DOUBLE PRECISION,
	ALERTTIME TIMESTAMPTZ NOT NULL
);
COMMIT;
SELECT CREATE_HYPERTABLE('BUDGET_ALERTS','alerttime',if_not_exists => TRUE);

---------- BUDGET_ALERTS TABLE END ------------
