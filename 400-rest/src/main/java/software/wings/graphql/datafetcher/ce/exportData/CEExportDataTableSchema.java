/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "CEExportDataTableKeys")
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CEExportDataTableSchema {
  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable billingDataTable;
  DbColumn startTime;
  DbColumn endTime;
  DbColumn accountId;
  DbColumn instanceId;
  DbColumn instanceName;
  DbColumn instanceType;
  DbColumn serviceId;
  DbColumn appId;
  DbColumn clusterName;
  DbColumn envId;
  DbColumn clusterId;
  DbColumn parentInstanceId;
  DbColumn region;
  DbColumn launchType;
  DbColumn clusterType;
  DbColumn workloadName;
  DbColumn workloadType;
  DbColumn billingAmount;
  DbColumn cpuBillingAmount;
  DbColumn memoryBillingAmount;
  DbColumn cloudServiceName;
  DbColumn taskId;
  DbColumn namespace;
  DbColumn idleCost;
  DbColumn cpuIdleCost;
  DbColumn memoryIdleCost;
  DbColumn unallocatedCost;
  DbColumn systemCost;
  DbColumn maxCpuUtilization;
  DbColumn maxMemoryUtilization;
  DbColumn avgCpuUtilization;
  DbColumn avgMemoryUtilization;
  DbColumn cpuRequest;
  DbColumn memoryRequest;
  DbColumn cpuLimit;
  DbColumn memoryLimit;
  DbColumn effectiveCpuRequest;
  DbColumn effectiveMemoryRequest;
  DbColumn effectiveCpuLimit;
  DbColumn effectiveMemoryLimit;
  DbColumn effectiveCpuUtilizationValue;
  DbColumn effectiveMemoryUtilizationValue;

  private static String varcharType = "varchar(40)";
  private static String doubleType = "double";

  public CEExportDataTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    billingDataTable = dbSchema.addTable("billing_data");
    startTime = billingDataTable.addColumn("starttime", "timestamp", null);
    endTime = billingDataTable.addColumn("endtime", "timestamp", null);
    accountId = billingDataTable.addColumn("accountid", "text", null);
    instanceId = billingDataTable.addColumn("instanceid", "text", null);
    instanceName = billingDataTable.addColumn("instancename", "text", null);
    instanceType = billingDataTable.addColumn("instancetype", varcharType, null);
    serviceId = billingDataTable.addColumn("serviceid", "text", null);
    appId = billingDataTable.addColumn("appid", "text", null);
    clusterName = billingDataTable.addColumn("clustername", "text", null);
    envId = billingDataTable.addColumn("envid", "text", null);
    clusterId = billingDataTable.addColumn("clusterid", "text", null);
    parentInstanceId = billingDataTable.addColumn("parentinstanceid", "text", null);
    region = billingDataTable.addColumn("region", varcharType, null);
    launchType = billingDataTable.addColumn("launchtype", varcharType, null);
    clusterType = billingDataTable.addColumn("clustertype", varcharType, null);
    workloadName = billingDataTable.addColumn("workloadname", "text", null);
    workloadType = billingDataTable.addColumn("workloadtype", "text", null);
    billingAmount = billingDataTable.addColumn("billingamount", doubleType, null);
    cpuBillingAmount = billingDataTable.addColumn("cpubillingamount", doubleType, null);
    memoryBillingAmount = billingDataTable.addColumn("memorybillingamount", doubleType, null);
    cloudServiceName = billingDataTable.addColumn("cloudServiceName", "text", null);
    taskId = billingDataTable.addColumn("taskId", "text", null);
    namespace = billingDataTable.addColumn("namespace", "text", null);
    idleCost = billingDataTable.addColumn("actualidlecost", doubleType, null);
    cpuIdleCost = billingDataTable.addColumn("cpuactualidlecost", doubleType, null);
    memoryIdleCost = billingDataTable.addColumn("memoryactualidlecost", doubleType, null);
    unallocatedCost = billingDataTable.addColumn("unallocatedcost", doubleType, null);
    systemCost = billingDataTable.addColumn("systemcost", doubleType, null);
    maxCpuUtilization = billingDataTable.addColumn("maxcpuutilization", doubleType, null);
    maxMemoryUtilization = billingDataTable.addColumn("maxmemoryutilization", doubleType, null);
    avgCpuUtilization = billingDataTable.addColumn("avgcpuutilization", doubleType, null);
    avgMemoryUtilization = billingDataTable.addColumn("avgmemoryutilization", doubleType, null);
    cpuRequest = billingDataTable.addColumn("cpurequest", doubleType, null);
    memoryRequest = billingDataTable.addColumn("memoryrequest", doubleType, null);
    cpuLimit = billingDataTable.addColumn("cpulimit", doubleType, null);
    memoryLimit = billingDataTable.addColumn("memorylimit", doubleType, null);
    effectiveCpuRequest = billingDataTable.addColumn("cpurequest*usagedurationseconds", doubleType, null);
    effectiveMemoryRequest = billingDataTable.addColumn("memoryrequest*usagedurationseconds", doubleType, null);
    effectiveCpuLimit = billingDataTable.addColumn("cpulimit*usagedurationseconds", doubleType, null);
    effectiveMemoryLimit = billingDataTable.addColumn("memorylimit*usagedurationseconds", doubleType, null);
    effectiveCpuUtilizationValue =
        billingDataTable.addColumn("avgcpuutilizationvalue*usagedurationseconds", doubleType, null);
    effectiveMemoryUtilizationValue =
        billingDataTable.addColumn("avgmemoryutilizationvalue*usagedurationseconds", doubleType, null);
  }
}
