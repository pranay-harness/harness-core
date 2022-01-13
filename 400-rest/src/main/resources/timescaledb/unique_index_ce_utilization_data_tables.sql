-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS UTILIZATION_DATA_UNIQUE_INDEX ON UTILIZATION_DATA(ACCOUNTID, SETTINGID, CLUSTERID, INSTANCEID, INSTANCETYPE, STARTTIME DESC);
COMMIT;

BEGIN;
CREATE UNIQUE INDEX IF NOT EXISTS KUBERNETES_UTILIZATION_DATA_UNIQUE_INDEX ON KUBERNETES_UTILIZATION_DATA(ACCOUNTID, SETTINGID, CLUSTERID, INSTANCEID, INSTANCETYPE, STARTTIME DESC);
COMMIT;
