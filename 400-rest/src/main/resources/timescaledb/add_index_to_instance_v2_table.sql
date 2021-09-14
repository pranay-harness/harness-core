-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

---------- INSTANCE TABLE START ------------
BEGIN;
CREATE INDEX IF NOT EXISTS INSTANCE_INSTANCEID_INDEX ON INSTANCE(INSTANCEID,CREATEDAT DESC);
COMMIT;
---------- INSTANCE TABLE END ------------
