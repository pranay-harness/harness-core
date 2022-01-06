-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;

DELETE FROM ANOMALIES where ID IN (select  ID from anomalies group by ID, ANOMALYTIME having count(*) > 1);

COMMIT;


BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS ANOMALIES_PKEY ON ANOMALIES (ID, ANOMALYTIME);

COMMIT;
