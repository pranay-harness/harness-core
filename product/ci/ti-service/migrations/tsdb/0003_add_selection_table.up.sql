-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

-- This table gives a snapshot of selection stats.
-- The TI service responds to an API by returning the list of tests to be run after running selection algorithm
-- It should also populate this table with high level stats for analytics purpose


CREATE TABLE IF NOT EXISTS selection(
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
--  Test execution overview
  type test_type_t DEFAULT 'unit',
  test_count integer,
  test_selected integer,
-- Test selection breakdown
  source_code_test integer,
  new_test integer,
  updated_test integer
);



comment on column selection.created_at is 'time when the selection was run';
comment on column selection.last_updated_at is 'Time when this entry was last updated';
--  Identification of a test run
comment on column selection.account_id is 'The unique id of the customer';
comment on column selection.org_id is 'Organization ID';
comment on column selection.project_id is 'Project ID';
comment on column selection.pipeline_id is 'Pipeline ID';
comment on column selection.build_id is 'The unique Build number across the pipeline';
comment on column selection.stage_id is 'stage ID';
comment on column selection.step_id is 'step ID';
--  Test execution overview
comment on column selection.type is 'type of the test. it can be unit/integration/functional/e2e';
comment on column selection.test_count is 'Total count of tests';
comment on column selection.test_selected is 'Total count of selected tests';
-- Test selection breakdown
comment on column selection.source_code_test is 'Count of the selected tests that correlated with source code changes';
comment on column selection.new_test is 'Total count of new tests';
comment on column selection.updated_test is 'Total count of updated tests';



-- distributed hypertable is supported only in 2.0 with multi-node. for TSDB 1.7, or 2.0 with single-node use create_hypertable
SELECT create_hypertable('selection', 'created_at');

CREATE INDEX IF NOT EXISTS selection_idx1 ON selection(account_id, org_id, project_id, pipeline_id, build_id, created_at DESC);
