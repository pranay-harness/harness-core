-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

DO
$do$
    BEGIN
        BEGIN
            ALTER TABLE selection ADD COLUMN time_saved_ms integer DEFAULT 0;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column time_saved_ms already exists in evaluation.';
        END;
    END;
$do$;

DO
$do$
    BEGIN
        BEGIN
            ALTER TABLE selection ADD COLUMN time_taken_ms integer DEFAULT 0;
        EXCEPTION
            WHEN duplicate_column THEN RAISE NOTICE 'column time_taken_ms already exists in evaluation.';
        END;
    END;
$do$;

comment on column selection.time_saved_ms is 'time saved in the selection because of test intelligence';
comment on column selection.time_taken_ms is 'time taken in the test intelligence step run';

CREATE INDEX IF NOT EXISTS evaluation_idx2 ON evaluation(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, created_at DESC);

CREATE INDEX IF NOT EXISTS selection_idx2 ON selection(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, created_at DESC);
CREATE INDEX IF NOT EXISTS selection_idx3 ON selection(account_id, org_id, project_id, pipeline_id, stage_id, step_id, created_at DESC);
