-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile

---------- AWS TRUTH TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS awstruthtable (
                            uuid text  NOT NULL,
                            accountid text  NOT NULL,
                            infraaccountid text  NOT NULL,
                            accountname text  NOT NULL);
COMMIT;
---------- AWS TRUTH TABLE END ------------
------------------------------------------------------------------------------------------------------------------------
---------- PIPELINE EXECUTION SUMMARY CI TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS pipeline_execution_summary_ci (
    id TEXT PRIMARY KEY,
    accountId TEXT,
    orgIdentifier TEXT,
    projectIdentifier  TEXT,
    pipelineIdentifier  TEXT,
    name  TEXT,
    status  TEXT,
    moduleInfo_type TEXT,
    moduleInfo_event TEXT,
    moduleInfo_author_id TEXT,
    author_name TEXT,
    author_avatar TEXT,
    moduleInfo_repository TEXT,
    moduleInfo_branch_name TEXT,
    moduleInfo_branch_commit_id TEXT,
    moduleInfo_branch_commit_message TEXT,
    startTs bigInt,
    endTs bigInt
  );

COMMIT;
---------- PIPELINE EXECUTION SUMMARY CI TABLE END ------------
------------------------------------------------------------------------------------------------------------------------
---------- PIPELINE EXECUTION SUMMARY CD TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS pipeline_execution_summary_cd (
    id TEXT PRIMARY KEY,
    accountId TEXT ,
    orgIdentifier TEXT ,
    projectIdentifier  TEXT ,
    pipelineIdentifier  TEXT ,
    name  TEXT ,
    status  TEXT ,
    moduleInfo_type TEXT ,
    startTs BIGINT,
    endTs BIGINT
  );
COMMIT;
---------- PIPELINE EXECUTION SUMMARY CD TABLE END ---------------
------------------------------------------------------------------------------------------------------------------------
---------- SERVICE INFRA INFO TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS service_infra_info (
    id TEXT PRIMARY KEY,
    service_name TEXT,
    service_id TEXT ,
    tag  TEXT ,
    env_name  TEXT ,
    env_id  TEXT ,
    service_status TEXT,
    service_startts BIGINT,
    service_endts BIGINT,
    env_type  TEXT ,
    deployment_type TEXT,
    pipeline_execution_summary_cd_id TEXT,
    accountId TEXT,
    orgIdentifier TEXT,
    projectIdentifier  TEXT
  );
COMMIT;
---------- SERVICE INFRA INFO TABLE END ------------