package software.wings.graphql.datafetcher.execution;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeploymentTableSchema {
  /**
   *  EXECUTIONID TEXT NOT NULL,
   * 	STARTTIME TIMESTAMP NOT NULL,
   * 	ENDTIME TIMESTAMP NOT NULL,
   * 	ACCOUNTID TEXT NOT NULL,
   * 	APPID TEXT NOT NULL,
   * 	TRIGGERED_BY TEXT,
   * 	TRIGGER_ID TEXT,
   * 	STATUS VARCHAR(20),
   * 	SERVICES TEXT[],
   * 	WORKFLOWS TEXT[],
   * 	CLOUDPROVIDERS TEXT[],
   * 	ENVIRONMENTS TEXT[],
   * 	PIPELINE TEXT,
   * 	DURATION BIGINT NOT NULL,
   * 	ARTIFACTS TEXT[]
   * 	ENVTYPE TEXT[]
   * 	PARENT_EXECUTION TEXT
   * 	STAGENAME TEXT
   * 	ROLLBACK_DURATION BIGINT
   * 	INSTANCES_DEPLOYED INT
   *    TAGS HSTORE
   */
  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable deploymentTable;
  DbColumn executionId;
  DbColumn startTime;
  DbColumn endTime;
  DbColumn accountId;
  DbColumn appId;
  DbColumn triggeredBy;
  DbColumn triggerId;
  DbColumn status;
  DbColumn services;
  DbColumn workflows;
  DbColumn cloudProviders;
  DbColumn environments;
  DbColumn pipeline;
  DbColumn duration;
  DbColumn artifacts;
  DbColumn envTypes;
  DbColumn parentExecution;
  DbColumn stageName;
  DbColumn rollbackDuration;
  DbColumn instancesDeployed;
  DbColumn tags;

  public DeploymentTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    deploymentTable = dbSchema.addTable("DEPLOYMENT");
    executionId = deploymentTable.addColumn("EXECUTIONID", "text", null);
    startTime = deploymentTable.addColumn("STARTTIME", "timestamp", null);
    endTime = deploymentTable.addColumn("ENDTIME", "timestamp", null);
    accountId = deploymentTable.addColumn("ACCOUNTID", "text", null);
    appId = deploymentTable.addColumn("APPID", "text", null);
    triggeredBy = deploymentTable.addColumn("TRIGGERED_BY", "text", null);
    triggerId = deploymentTable.addColumn("TRIGGER_ID", "text", null);
    status = deploymentTable.addColumn("STATUS", "varchar(20)", null);
    services = deploymentTable.addColumn("SERVICES", "text[]", null);
    workflows = deploymentTable.addColumn("WORKFLOWS", "text[]", null);
    cloudProviders = deploymentTable.addColumn("CLOUDPROVIDERS", "text[]", null);
    environments = deploymentTable.addColumn("ENVIRONMENTS", "text[]", null);
    pipeline = deploymentTable.addColumn("PIPELINE", "text", null);
    duration = deploymentTable.addColumn("DURATION", "bigint", null);
    artifacts = deploymentTable.addColumn("ARTIFACTS", "text[]", null);
    envTypes = deploymentTable.addColumn("ENVTYPES", "text[]", null);
    parentExecution = deploymentTable.addColumn("PARENT_EXECUTION", "text", null);
    stageName = deploymentTable.addColumn("STAGENAME", "text", null);
    rollbackDuration = deploymentTable.addColumn("ROLLBACK_DURATION", "bigint", null);
    instancesDeployed = deploymentTable.addColumn("INSTANCES_DEPLOYED", "int", null);
    tags = deploymentTable.addColumn("TAGS", "hstore", null);
  }
}
