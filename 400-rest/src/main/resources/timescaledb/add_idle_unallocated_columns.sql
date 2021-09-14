-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

BEGIN;
ALTER TABLE BILLING_DATA ADD COLUMN ACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN UNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUUNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYUNALLOCATEDCOST DOUBLE PRECISION;
COMMIT;
