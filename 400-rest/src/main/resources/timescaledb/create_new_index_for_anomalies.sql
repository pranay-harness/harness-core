-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;

DELETE FROM ANOMALIES where ID IN (select  ID from anomalies group by ID, ANOMALYTIME having count(*) > 1);

COMMIT;


BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS ANOMALIES_PKEY ON ANOMALIES (ID, ANOMALYTIME);

COMMIT;
