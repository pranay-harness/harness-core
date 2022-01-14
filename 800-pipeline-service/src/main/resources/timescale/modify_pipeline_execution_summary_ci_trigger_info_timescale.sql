BEGIN;

ALTER TABLE pipeline_execution_summary_ci ADD COLUMN IF NOT EXISTS trigger_type TEXT, ADD COLUMN IF NOT EXISTS source_branch TEXT;

COMMIT;