package software.wings.graphql.datafetcher.billing;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillingDataTableSchema {
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
   */
  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable billingDataTable;
  DbColumn startTime;
  DbColumn endTime;
  DbColumn accountId;
  DbColumn billingAccountId;
  DbColumn instanceId;
  DbColumn instanceType;
  DbColumn serviceId;
  DbColumn appId;
  DbColumn clusterName;
  DbColumn cloudProviderId;
  DbColumn envId;
  DbColumn clusterId;
  DbColumn parentInstanceId;
  DbColumn region;
  DbColumn launchType;
  DbColumn clusterType;
  DbColumn workloadName;
  DbColumn workloadType;
  DbColumn cloudProvider;
  DbColumn billingAmount;
  DbColumn cpuBillingAmount;
  DbColumn memoryBillingAmount;
  DbColumn usageDurationSeconds;
  DbColumn cpuUnitSeconds;
  DbColumn memoryMbSeconds;
  DbColumn cloudServiceName;
  DbColumn taskId;
  DbColumn namespace;
  DbColumn idleCost;
  DbColumn cpuIdleCost;
  DbColumn memoryIdleCost;
  DbColumn maxCpuUtilization;
  DbColumn maxMemoryUtilization;
  DbColumn avgCpuUtilization;
  DbColumn avgMemoryUtilization;

  private static String varcharType = "varchar(40)";
  private static String doubleType = "double";

  public BillingDataTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    billingDataTable = dbSchema.addTable("billing_data");
    startTime = billingDataTable.addColumn("starttime", "timestamp", null);
    endTime = billingDataTable.addColumn("endtime", "timestamp", null);
    accountId = billingDataTable.addColumn("accountid", "text", null);
    billingAccountId = billingDataTable.addColumn("billingaccountid", "text", null);
    instanceId = billingDataTable.addColumn("instanceid", "text", null);
    instanceType = billingDataTable.addColumn("instancetype", varcharType, null);
    serviceId = billingDataTable.addColumn("serviceid", "text", null);
    appId = billingDataTable.addColumn("appid", "text", null);
    clusterName = billingDataTable.addColumn("clustername", "text", null);
    cloudProviderId = billingDataTable.addColumn("cloudproviderid", "text", null);
    envId = billingDataTable.addColumn("envid", "text", null);
    clusterId = billingDataTable.addColumn("clusterid", "text", null);
    parentInstanceId = billingDataTable.addColumn("parentinstanceid", "text", null);
    region = billingDataTable.addColumn("region", varcharType, null);
    launchType = billingDataTable.addColumn("launchtype", varcharType, null);
    clusterType = billingDataTable.addColumn("clustertype", varcharType, null);
    workloadName = billingDataTable.addColumn("workloadname", "text", null);
    workloadType = billingDataTable.addColumn("workloadtype", "text", null);
    cloudProvider = billingDataTable.addColumn("cloudprovider", varcharType, null);
    billingAmount = billingDataTable.addColumn("billingamount", doubleType, null);
    cpuBillingAmount = billingDataTable.addColumn("cpubillingamount", doubleType, null);
    memoryBillingAmount = billingDataTable.addColumn("memorybillingamount", doubleType, null);
    usageDurationSeconds = billingDataTable.addColumn("usagedurationseconds", doubleType, null);
    cpuUnitSeconds = billingDataTable.addColumn("cpuunitseconds", doubleType, null);
    memoryMbSeconds = billingDataTable.addColumn("memorymbseconds", doubleType, null);
    cloudServiceName = billingDataTable.addColumn("cloudServiceName", "text", null);
    taskId = billingDataTable.addColumn("taskId", "text", null);
    namespace = billingDataTable.addColumn("namespace", "text", null);
    idleCost = billingDataTable.addColumn("idlecost", doubleType, null);
    cpuIdleCost = billingDataTable.addColumn("cpuidlecost", doubleType, null);
    memoryIdleCost = billingDataTable.addColumn("memoryidlecost", doubleType, null);
    maxCpuUtilization = billingDataTable.addColumn("maxcpuutilization", doubleType, null);
    maxMemoryUtilization = billingDataTable.addColumn("maxmemoryutilization", doubleType, null);
    avgCpuUtilization = billingDataTable.addColumn("avgcpuutilization", doubleType, null);
    avgMemoryUtilization = billingDataTable.addColumn("avgmemoryutilization", doubleType, null);
  }
}
