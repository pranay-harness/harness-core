BEGIN;
ALTER TABLE BILLING_DATA ADD COLUMN CPUREQUEST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYREQUEST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN CPULIMIT DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MEMORYLIMIT DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MAXCPUUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN MAXMEMORYUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN AVGCPUUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA ADD COLUMN AVGMEMORYUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN CPUREQUEST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN MEMORYREQUEST DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN CPULIMIT DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN MEMORYLIMIT DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN MAXCPUUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN MAXMEMORYUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN AVGCPUUTILIZATIONVALUE DOUBLE PRECISION;
ALTER TABLE BILLING_DATA_HOURLY ADD COLUMN AVGMEMORYUTILIZATIONVALUE DOUBLE PRECISION;
COMMIT;