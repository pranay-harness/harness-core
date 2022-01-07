-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

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
