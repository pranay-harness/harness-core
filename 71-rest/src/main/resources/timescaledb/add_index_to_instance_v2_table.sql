---------- INSTANCE TABLE START ------------
BEGIN;
CREATE INDEX IF NOT EXISTS INSTANCE_INSTANCEID_INDEX ON INSTANCE(INSTANCEID,CREATEDAT DESC);
COMMIT;
---------- INSTANCE TABLE END ------------