-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

ALTER TABLE pipeline_execution_summary_cd ADD COLUMN IF NOT EXISTS trigger_type TEXT, ADD COLUMN IF NOT EXISTS author_name TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_author_id TEXT, ADD COLUMN IF NOT EXISTS author_avatar TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_repository TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_branch_name TEXT, ADD COLUMN IF NOT EXISTS source_branch TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_event TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_branch_commit_id TEXT, ADD COLUMN IF NOT EXISTS moduleinfo_branch_commit_message TEXT;

COMMIT;
