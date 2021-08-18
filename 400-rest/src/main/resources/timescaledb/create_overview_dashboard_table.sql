---------- KUBERNETES_UTILIZATION_DATA TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS OVERVIEW_DASHBOARD (
                                                           STARTTIME TIMESTAMPTZ NOT NULL,
                                                           ENDTIME TIMESTAMPTZ NOT NULL,
                                                           ACCOUNTID TEXT NOT NULL,
                                                           SETTINGID TEXT NOT NULL,
                                                           INSTANCEID TEXT NOT NULL,
                                                           INSTANCETYPE TEXT NOT NULL,
                                                           CPU DOUBLE PRECISION  NOT NULL,
                                                           MEMORY DOUBLE PRECISION  NOT NULL
);
COMMIT;
SELECT CREATE_HYPERTABLE('OVERVIEW_DASHBOARD','starttime',if_not_exists => TRUE);
---------- KUBERNETES_UTILIZATION_DATA TABLE END ------------