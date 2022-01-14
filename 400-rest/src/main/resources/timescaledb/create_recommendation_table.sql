---------- CE_RECOMMENDATIONS TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS ce_recommendations (
                                                  id text PRIMARY KEY,
                                                  name text,
                                                  namespace text,
                                                  monthlycost double precision,
                                                  monthlysaving double precision,
                                                  clustername text,
                                                  resourcetype text NOT NULL,
                                                  accountid text NOT NULL,
                                                  isvalid boolean,
                                                  lastprocessedat timestamp with time zone,
                                                  updatedat timestamp with time zone NOT NULL DEFAULT now()
);
COMMIT;
---------- CE_RECOMMENDATIONS TABLE END ------------