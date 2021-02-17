-- This table gives a snapshot of source method code coverage.
-- It lists all source methods that don't have any test coverage
-- The TI service responds to an API by returning the list of tests to be run after running selection algorithm
-- It should also populate this table with high level stats for analytics purpose

DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'code_type_t') THEN
      CREATE TYPE code_type_t AS ENUM ('source','test','conf','resource','unknown');
      COMMENT ON TYPE code_type_t IS 'type of a source code';
   END IF;
END
$do$;

DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_type  -- SELECT list can be empty for this
      WHERE  typname = 'skip_criterion_t') THEN
      CREATE TYPE skip_criterion_t AS ENUM ('missing_test','tiignore','unknown');
      COMMENT ON TYPE skip_criterion_t IS 'why was this source skipped from testing?';
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

  filename  TEXT,
  suite_name TEXT NOT NULL,
  class_name TEXT,
  name TEXT NOT NULL,
  type code_type_t DEFAULT 'source',
  criterion skip_criterion_t DEFAULT 'missing_test'
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

comment on column coverage.filename is 'filename';
comment on column coverage.suite_name is 'suite name';
comment on column coverage.class_name is 'class name. Not applicable to all programming languages';
comment on column coverage.name is 'Name of the source/test method';
comment on column coverage.type is 'type of code. it could be source/test';
comment on column coverage.criterion is 'indicates why a source method was not tested';



-- distributed hypertable is supported only in 2.0. As we are using TSDB 1.7, using create_hypertable for now
SELECT create_hypertable('coverage', 'created_at');

CREATE INDEX IF NOT EXISTS coverage_idx1 ON coverage(account_id, org_id, project_id, pipeline_id, build_id, created_at DESC);