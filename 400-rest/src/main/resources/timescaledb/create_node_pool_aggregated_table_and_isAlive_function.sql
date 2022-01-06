-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- is_alive FUNCTION START ------------
BEGIN;
CREATE OR REPLACE function is_alive(instanceStartTime TIMESTAMPTZ, instanceStopTime TIMESTAMPTZ, jobStartTime TIMESTAMPTZ, jobStopTime TIMESTAMPTZ)
returns boolean
language plpgsql
as
$$ begin IF (instanceStartTime <= jobStartTime AND ( instanceStopTime IS NULL OR jobStartTime < instanceStopTime) ) OR (jobStartTime <= instanceStartTime AND instanceStartTime < jobStopTime) THEN RETURN TRUE; END IF; RETURN FALSE; end; $$;
COMMIT;
---------- is_alive FUNCTION END ------------


---------- node_pool_aggregated TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS node_pool_aggregated (
                                                                 name text,
                                                                 clusterid text NOT NULL,
                                                                 accountid text NOT NULL,
                                                                 sumcpu double precision,
                                                                 summemory double precision,
                                                                 maxcpu double precision,
                                                                 maxmemory double precision,
                                                                 starttime timestamp with time zone,
                                                                 endtime timestamp with time zone,
                                                                 updatedat timestamp with time zone DEFAULT now(),
    CONSTRAINT node_pool_aggregated_unique_record_index UNIQUE (accountid, clusterid, name, starttime, endtime)
    );

COMMIT;
---------- node_pool_aggregated TABLE END ------------
