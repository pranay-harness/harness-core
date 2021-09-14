-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS INSTANCES_DEPLOYED INT;

COMMIT ;
