-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

---------- DEPLOYMENT TABLE START ------------
BEGIN;
CREATE INDEX IF NOT EXISTS ACCOUNTID_STATUS_INDEX ON DEPLOYMENT(ACCOUNTID,STATUS);
COMMIT;
---------- DEPLOYMENT TABLE END ------------
