-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

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
