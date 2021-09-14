-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

-- This table gives details about files changed in the PR.
-- It contains paths of the files as well as whether they were modified, added or deleted.
-- Information in this table is required for the correct merging of the partial call
-- graph to master, as we don't get data of removed tests in the call graph.
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'status_t') THEN
      CREATE TYPE status_t AS ENUM ('modified','added','deleted');
      COMMENT ON TYPE status_t IS 'status of the file in the PR';
   END IF;
END
$do$;


CREATE TABLE IF NOT EXISTS coverage(
  created_at        TIMESTAMPTZ DEFAULT now() NOT NULL,
  last_updated_at   timestamp with time zone DEFAULT now() NOT NULL,
--  Identification of a test run
  account_id TEXT NOT NULL,
  org_id TEXT NOT NULL,
  project_id TEXT NOT NULL,
  pipeline_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  stage_id TEXT NOT NULL,
  step_id TEXT NOT NULL,

  sha TEXT,
  file_path  TEXT,
  status status_t DEFAULT 'modified'
);



comment on column coverage.created_at is 'time when the selection was run';
comment on column coverage.last_updated_at is 'Time when this entry was last updated';
--  Identification of a test run
comment on column coverage.account_id is 'The unique id of the customer';
comment on column coverage.org_id is 'Organization ID';
comment on column coverage.project_id is 'Project ID';
comment on column coverage.pipeline_id is 'Pipeline ID';
comment on column coverage.build_id is 'The unique Build number across the pipeline';
comment on column coverage.stage_id is 'stage ID';
comment on column coverage.step_id is 'step ID';

comment on column coverage.sha is 'The commit ID where this change was made. Note that the commit id stored here has information on all the files changed uptil that commit in the PR.';
comment on column coverage.file_path is 'Path of the changed file in the PR.';
comment on column coverage.status is 'Whether the file was modified/added/deleted';


-- distributed hypertable is supported only in 2.0 with multi-node. for TSDB 1.7, or 2.0 with single-node use create_hypertable
SELECT create_hypertable('coverage', 'created_at');

CREATE INDEX IF NOT EXISTS coverage_idx1 ON coverage(account_id, org_id, project_id, pipeline_id, build_id, created_at DESC);
