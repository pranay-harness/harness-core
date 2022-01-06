-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- NODE_INFO TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS node_info (
                           accountid text NOT NULL,
                           clusterid text NOT NULL,
                           instanceid text NOT NULL,
                           starttime timestamp with time zone NOT NULL,
                           stoptime timestamp with time zone,
                           nodepoolname text,
                           createdat timestamp with time zone DEFAULT now(),
                           updatedat timestamp with time zone DEFAULT now(),
                           CONSTRAINT node_info_unique_record_index UNIQUE (accountid, clusterid, instanceid)
);
COMMIT;
---------- NODE_INFO TABLE END ------------
------------------------------------------------------------------------------------------------------------------------
---------- POD_INFO TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS pod_info (
                          accountid text NOT NULL,
                          clusterid text NOT NULL,
                          instanceid text NOT NULL,
                          starttime timestamp with time zone NOT NULL,
                          stoptime timestamp with time zone,
                          parentnodeid text,
                          namespace text,
                          name text,
                          cpurequest double precision,
                          memoryrequest double precision,
                          workloadid text,
                          createdat timestamp with time zone DEFAULT now(),
                          updatedat timestamp with time zone DEFAULT now(),
                          CONSTRAINT pod_info_unique_record_index UNIQUE (accountid, clusterid, instanceid)
);

COMMIT;
---------- POD_INFO TABLE END ------------
------------------------------------------------------------------------------------------------------------------------
---------- WORKLOAD_INFO TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS workload_info (
                               accountid text NOT NULL,
                               clusterid text NOT NULL,
                               workloadid text NOT NULL,
                               namespace text,
                               name text,
                               replicas integer NOT NULL DEFAULT 1,
                               createdat timestamp with time zone DEFAULT now(),
                               updatedat timestamp with time zone DEFAULT now(),
                               type text,
                               CONSTRAINT workload_info_unique_record_index UNIQUE (accountid, clusterid, workloadid)
);
COMMIT;
---------- WORKLOAD_INFO TABLE END ------------
