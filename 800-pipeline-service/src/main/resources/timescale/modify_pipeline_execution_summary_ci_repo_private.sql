BEGIN;

ALTER TABLE pipeline_execution_summary_ci ADD COLUMN IF NOT EXISTS moduleinfo_is_private boolean DEFAULT false;
comment on column pipeline_execution_summary_ci.moduleinfo_is_private is 'Is the cloned repo private';

COMMIT;