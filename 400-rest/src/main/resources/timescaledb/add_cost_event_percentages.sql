-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;
ALTER TABLE COST_EVENT_DATA ADD COLUMN IF NOT EXISTS COST_CHANGE_PERCENT DOUBLE PRECISION;
COMMIT;
