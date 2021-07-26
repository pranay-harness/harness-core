BEGIN;

ALTER TABLE pipeline_execution_summary_cd ADD COLUMN IF NOT EXISTS planExecutionId TEXT;

COMMIT;

BEGIN;

ALTER TABLE service_infra_info ADD COLUMN IF NOT EXISTS artifact_image TEXT;

COMMIT;