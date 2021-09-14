-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;
ALTER TABLE DEPLOYMENT ALTER endtime TYPE timestamptz USING endtime AT TIME ZONE 'UTC';
ALTER TABLE DEPLOYMENT ALTER starttime TYPE timestamptz USING starttime AT TIME ZONE 'UTC';

ALTER TABLE INSTANCE_STATS ALTER reportedat TYPE timestamptz USING reportedat AT TIME ZONE 'UTC';

ALTER TABLE VERIFICATION_WORKFLOW_STATS ALTER start_time TYPE timestamptz USING start_time AT TIME ZONE 'UTC';
ALTER TABLE VERIFICATION_WORKFLOW_STATS ALTER end_time TYPE timestamptz USING end_time AT TIME ZONE 'UTC';
COMMIT;

---DROP OLD TABLE----

BEGIN;
DROP TABLE IF EXISTS INSTANCE;
COMMIT;
-----
