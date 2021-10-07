BEGIN;

ALTER TABLE pipeline_execution_summary_ci ADD COLUMN IF NOT EXISTS pr integer;
comment on column pipeline_execution_summary_ci.pr is 'The Pull Request number';

COMMIT;