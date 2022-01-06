-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
ALTER TABLE BILLING_DATA ADD COLUMN ACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYACTUALIDLECOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN UNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUUNALLOCATEDCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYUNALLOCATEDCOST DOUBLE PRECISION;
COMMIT;
