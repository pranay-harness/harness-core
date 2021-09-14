-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

ALTER TABLE pipeline_execution_summary_ci ADD COLUMN IF NOT EXISTS trigger_type TEXT, ADD COLUMN IF NOT EXISTS source_branch TEXT;

COMMIT;
