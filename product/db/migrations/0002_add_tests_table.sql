CREATE TABLE IF NOT EXISTS tests(
  time        TIMESTAMPTZ       NOT NULL,
  account_id TEXT NOT NULL,
  org_id TEXT NOT NULL,
  project_id TEXT NOT NULL,
  pipeline_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  stage_id TEXT NOT NULL,
  step_id TEXT NOT NULL,
  report TEXT NOT NULL,
  name TEXT NOT NULL,
  suite_name TEXT NOT NULL,
  class_name TEXT,
  duration_ms INT,
  status TEXT,
  message TEXT,
  description TEXT,
  type TEXT,
  stdout TEXT,
  stderr TEXT
);

comment on column tests.time is 'time when the test was run';
comment on column tests.account_id is 'The unique id of the customer';
comment on column tests.org_id is 'Organization ID';
comment on column tests.project_id is 'Project ID';
comment on column tests.pipeline_id is 'Pipeline ID';
comment on column tests.build_id is 'The unique Build number across the pipeline';
comment on column tests.stage_id is 'stage ID';
comment on column tests.step_id is 'step ID';
comment on column tests.report is '';
comment on column tests.name is '';
comment on column tests.suite_name is '';
comment on column tests.class_name is 'class name. Not applicable to all programming languages';
comment on column tests.duration_ms is 'time taken to run the test in millisecond';
comment on column tests.status is 'could be one of passed/skipped/failed/error';
comment on column tests.message is 'message';
comment on column tests.description is 'description';
comment on column tests.type is 'type';
comment on column tests.stdout is 'stdout';
comment on column tests.stderr is 'stderr';


-- distributed hypertable is supported only in 2.0. As we are using TSDB 1.7, using create_hypertable for now
--SELECT create_hypertable('tests', 'time');

CREATE INDEX IF NOT EXISTS tests_idx1 ON tests(account_id, org_id, project_id, pipeline_id, build_id,time DESC);


