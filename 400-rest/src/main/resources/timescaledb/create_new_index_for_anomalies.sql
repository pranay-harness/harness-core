BEGIN;

CREATE UNIQUE INDEX ANOMALIES_PKEY ON ANOMALIES (ID, ANOMALYTIME);

COMMIT;