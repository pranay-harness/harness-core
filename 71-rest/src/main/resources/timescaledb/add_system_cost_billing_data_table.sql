BEGIN;
ALTER TABLE BILLING_DATA ADD COLUMN SYSTEMCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPUSYSTEMCOST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYSYSTEMCOST DOUBLE PRECISION;
COMMIT;