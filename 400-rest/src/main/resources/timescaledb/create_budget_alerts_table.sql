-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

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
