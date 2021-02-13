CREATE TABLE IF NOT EXISTS evaluation(
  created_at        TIMESTAMPTZ       NOT NULL,
  last_updated_at   timestamp with time zone DEFAULT now() NOT NULL,
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

comment on column evaluation.created_at is 'time when the test was run';
comment on column evaluation.last_updated_at is 'Time when this entry was last updated';
comment on column evaluation.account_id is 'The unique id of the customer';
comment on column evaluation.org_id is 'Organization ID';
comment on column evaluation.project_id is 'Project ID';
comment on column evaluation.pipeline_id is 'Pipeline ID';
comment on column evaluation.build_id is 'The unique Build number across the pipeline';
comment on column evaluation.stage_id is 'stage ID';
comment on column evaluation.step_id is 'step ID';
comment on column evaluation.report is '';
comment on column evaluation.name is '';
comment on column evaluation.suite_name is '';
comment on column evaluation.class_name is 'class name. Not applicable to all programming languages';
comment on column evaluation.duration_ms is 'time taken to run the test in millisecond';
comment on column evaluation.status is 'could be one of passed/skipped/failed/error';
comment on column evaluation.message is 'message';
comment on column evaluation.description is 'description';
comment on column evaluation.type is 'type';
comment on column evaluation.stdout is 'stdout';
comment on column evaluation.stderr is 'stderr';


-- distributed hypertable is supported only in 2.0. As we are using TSDB 1.7, using create_hypertable for now
SELECT create_hypertable('evaluation', 'created_at');

CREATE INDEX IF NOT EXISTS evaluation_idx1 ON evaluation(account_id, org_id, project_id, pipeline_id, build_id, created_at DESC);


