-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;

CREATE INDEX IF NOT EXISTS kubernetes_utilization_data_accid_clusterid_acinstanceid ON kubernetes_utilization_data(accountid, clusterid, actualinstanceid, starttime DESC);
CREATE INDEX IF NOT EXISTS node_info_accid_clusterid_poolname ON node_info(accountid, clusterid, nodepoolname);

ALTER TABLE pod_info drop constraint if exists pod_info_unique_record_index;
CREATE UNIQUE INDEX IF NOT EXISTS pod_info_starttime_unique_record_index ON pod_info(accountid, clusterid, instanceid, starttime DESC);
SELECT CREATE_HYPERTABLE('pod_info','starttime',if_not_exists => TRUE, migrate_data => TRUE);

CREATE INDEX IF NOT EXISTS pod_info_kubesystem_namespace_pindex ON pod_info(accountid, clusterid, namespace, starttime DESC) WHERE namespace <> 'kube-system';
COMMIT;
