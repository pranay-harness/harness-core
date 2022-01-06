-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

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
