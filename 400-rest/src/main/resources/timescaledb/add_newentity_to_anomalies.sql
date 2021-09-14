-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

ALTER TABLE ANOMALIES ADD COLUMN IF NOT EXISTS NEWENTITY BOOLEAN DEFAULT FALSE;

COMMIT;
