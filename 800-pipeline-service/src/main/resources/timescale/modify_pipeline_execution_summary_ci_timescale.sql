BEGIN;

ALTER TABLE pipeline_execution_summary_ci ADD COLUMN IF NOT EXISTS planExecutionId TEXT, ADD COLUMN IF NOT EXISTS errorMessage TEXT;

COMMIT;