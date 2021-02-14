DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'result_t') THEN
      CREATE TYPE result_t AS ENUM ('passed','skipped','failed','error');
      COMMENT ON TYPE result_t IS 'represents an evaluated result of a test';
   END IF;
END
$do$;


DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'test_type_t') THEN
      CREATE TYPE test_type_t AS ENUM ('unit','functional','integration','e2e');
      COMMENT ON TYPE test_type_t IS 'type of a test';
   END IF;
END
$do$;

DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'selection_criterion_t') THEN
      CREATE TYPE selection_criterion_t AS ENUM ('source_code_changes','new_test','updated_test','flaky_test','tiignore');
      COMMENT ON TYPE selection_criterion_t IS 'why was this test selected/not_selected to run?';
   END IF;
END
$do$;


CREATE TABLE IF NOT EXISTS evaluation(
  created_at        TIMESTAMPTZ  DEFAULT now()  NOT NULL,
  last_updated_at   timestamp with time zone DEFAULT now() NOT NULL,

  --  Identification of a test run
  account_id TEXT NOT NULL,
  org_id TEXT NOT NULL,
  project_id TEXT NOT NULL,
  pipeline_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  stage_id TEXT NOT NULL,
  step_id TEXT NOT NULL,

  -- test execution
  report TEXT NOT NULL,
  suite_name TEXT NOT NULL,
  class_name TEXT,
  name TEXT NOT NULL,
  duration_ms INT,
  result result_t,
  message TEXT,
  description TEXT,
  type test_type_t,
  stdout TEXT,
  stderr TEXT,
  selected boolean DEFAULT true NOT NULL,
  criterion selection_criterion_t
);

comment on column evaluation.created_at is 'time when the test was run';
comment on column evaluation.last_updated_at is 'Time when this entry was last updated';

--  Identification of a test run
comment on column evaluation.account_id is 'The unique id of the customer';
comment on column evaluation.org_id is 'Organization ID';
comment on column evaluation.project_id is 'Project ID';
comment on column evaluation.pipeline_id is 'Pipeline ID';
comment on column evaluation.build_id is 'The unique Build number across the pipeline';
comment on column evaluation.stage_id is 'stage ID';
comment on column evaluation.step_id is 'step ID';

-- test execution
comment on column evaluation.report is 'Report type. eg. junit';
comment on column evaluation.name is 'Name of the test. It can be method name in case of unit test';
comment on column evaluation.suite_name is 'suite name';
comment on column evaluation.class_name is 'class name. Not applicable to all programming languages';
comment on column evaluation.duration_ms is 'time taken to run the test in millisecond';
comment on column evaluation.result is 'represents an evaluated result of a test. It could be one of passed/skipped/failed/error';
comment on column evaluation.message is 'message';
comment on column evaluation.description is 'description';
comment on column evaluation.type is 'type of the test. it can be unit/integration/functional/e2e';
comment on column evaluation.stdout is 'stdout';
comment on column evaluation.stderr is 'stderr';
comment on column evaluation.criterion is 'why was this test selected/not_selected to run?. It could be one of source_code_changes/new_test/updated_test/flaky_test/tiignore';


-- distributed hypertable is supported only in 2.0. As we are using TSDB 1.7, using create_hypertable for now
SELECT create_hypertable('evaluation', 'created_at');

CREATE INDEX IF NOT EXISTS evaluation_idx1 ON evaluation(account_id, org_id, project_id, pipeline_id, build_id, created_at DESC);


