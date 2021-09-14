-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

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
