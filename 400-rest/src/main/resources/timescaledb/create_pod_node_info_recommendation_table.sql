-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

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
