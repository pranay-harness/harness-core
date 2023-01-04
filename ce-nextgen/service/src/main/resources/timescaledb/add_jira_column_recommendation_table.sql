-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
ALTER TABLE ce_recommendations ADD COLUMN IF NOT EXISTS jiraconnectorref TEXT;
ALTER TABLE ce_recommendations ADD COLUMN IF NOT EXISTS jiraissuekey TEXT;
ALTER TABLE ce_recommendations ADD COLUMN IF NOT EXISTS jirastatus TEXT;
ALTER TABLE ce_recommendations ADD COLUMN IF NOT EXISTS recommendationstate TEXT DEFAULT 'OPEN';
COMMIT;
