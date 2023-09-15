/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.clickHouse;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ClickHouseConstants {
  public static final String createCCMDBQuery = "create database if not exists ccm;";
  public static final String createAwsCurTableQuery =
      "CREATE TABLE IF NOT EXISTS ccm.awscur ( `resourceid` String NULL, `usagestartdate` DateTime('UTC') NULL, `productname` String NULL, `productfamily` String NULL, `servicecode` String NULL, `servicename` String NULL, `blendedrate` String NULL, `blendedcost` Float NULL, `unblendedrate` String NULL, `unblendedcost` Float NULL, `region` String NULL, `availabilityzone` String NULL, `usageaccountid` String NULL, `instancetype` String NULL, `usagetype` String NULL, `lineitemtype` String NULL, `effectivecost` Float NULL, `usageamount` Float NULL, `billingentity` String NULL, `instanceFamily` String NULL, `marketOption` String NULL, `amortisedCost` Float NULL, `netAmortisedCost` Float NULL, `tags` Map(String, String) ) ENGINE = MergeTree ORDER BY tuple(usagestartdate) SETTINGS allow_nullable_key = 1;";
  public static final String createUnifiedTableTableQuery =
      "CREATE TABLE IF NOT EXISTS ccm.unifiedTable ( `startTime` DateTime('UTC') NOT NULL, `cost` Float NULL, `gcpProduct` String NULL, `gcpSkuId` String NULL, `gcpSkuDescription` String NULL, `gcpProjectId` String NULL, `gcpInvoiceMonth` String NULL, `gcpCostType` String NULL, `region` String NULL, `zone` String NULL, `gcpBillingAccountId` String NULL, `cloudProvider` String NULL, `awsBlendedRate` String NULL, `awsBlendedCost` Float NULL, `awsUnblendedRate` String NULL, `awsUnblendedCost` Float NULL, `awsEffectiveCost` Float NULL, `awsAmortisedCost` Float NULL, `awsNetAmortisedCost` Float NULL, `awsLineItemType` String NULL, `awsServicecode` String NULL, `awsAvailabilityzone` String NULL, `awsUsageaccountid` String NULL, `awsInstancetype` String NULL, `awsUsagetype` String NULL, `awsBillingEntity` String NULL, `discount` Float NULL, `endtime` DateTime('UTC') NULL, `accountid` String NULL, `instancetype` String NULL, `clusterid` String NULL, `clustername` String NULL, `appid` String NULL, `serviceid` String NULL, `envid` String NULL, `cloudproviderid` String NULL, `launchtype` String NULL, `clustertype` String NULL, `workloadname` String NULL, `workloadtype` String NULL, `namespace` String NULL, `cloudservicename` String NULL, `taskid` String NULL, `clustercloudprovider` String NULL, `billingamount` Float NULL, `cpubillingamount` Float NULL, `memorybillingamount` Float NULL, `idlecost` Float NULL, `maxcpuutilization` Float NULL, `avgcpuutilization` Float NULL, `systemcost` Float NULL, `actualidlecost` Float NULL, `unallocatedcost` Float NULL, `networkcost` Float NULL, `product` String NULL, `azureMeterCategory` String NULL, `azureMeterSubcategory` String NULL, `azureMeterId` String NULL, `azureMeterName` String NULL, `azureResourceType` String NULL, `azureServiceTier` String NULL, `azureInstanceId` String NULL, `azureResourceGroup` String NULL, `azureSubscriptionGuid` String NULL, `azureAccountName` String NULL, `azureFrequency` String NULL, `azurePublisherType` String NULL, `azurePublisherName` String NULL, `azureServiceName` String NULL, `azureSubscriptionName` String NULL, `azureReservationId` String NULL, `azureReservationName` String NULL, `azureResource` String NULL, `azureVMProviderId` String NULL, `azureTenantId` String NULL, `azureBillingCurrency` String NULL, `azureCustomerName` String NULL, `azureResourceRate` Float NULL, `orgIdentifier` String NULL, `projectIdentifier` String NULL, `labels` Map(String, String) ) ENGINE = MergeTree ORDER BY tuple(startTime) SETTINGS allow_nullable_key = 1;";
  public static final String createPreAggregatedTableQuery =
      "CREATE TABLE IF NOT EXISTS ccm.preAggregated ( `cost` Float NULL, `gcpProduct` String NULL, `gcpSkuId` String NULL, `gcpSkuDescription` String NULL, `startTime` DateTime('UTC') NULL, `gcpProjectId` String NULL, `region` String NULL, `zone` String NULL, `gcpBillingAccountId` String NULL, `cloudProvider` String NULL, `awsBlendedRate` String NULL, `awsBlendedCost` Float NULL, `awsUnblendedRate` String NULL, `awsUnblendedCost` Float NULL, `awsServicecode` String NULL, `awsAvailabilityzone` String NULL, `awsUsageaccountid` String NULL, `awsInstancetype` String NULL, `awsUsagetype` String NULL, `discount` Float NULL, `azureServiceName` String NULL, `azureResourceRate` Float NULL, `azureSubscriptionGuid` String NULL, `azureTenantId` String NULL ) ENGINE = MergeTree ORDER BY tuple(startTime) SETTINGS allow_nullable_key = 1;";
  public static final String createCostAggregatedTableQuery =
      "CREATE TABLE IF NOT EXISTS ccm.costAggregated ( `accountId` String NULL, `cloudProvider` String NOT NULL, `cost` Float NOT NULL, `day` DateTime('UTC') NOT NULL ) ENGINE = MergeTree ORDER BY tuple(day) SETTINGS allow_nullable_key = 1;";
  public static final String createConnectorDataSyncStatusTableQuery =
      "CREATE TABLE IF NOT EXISTS ccm.connectorDataSyncStatus ( `accountId` String NULL, `connectorId` String NOT NULL, `jobType` String NULL, `cloudProviderId` String NULL, `lastSuccessfullExecutionAt` DateTime('UTC') NOT NULL ) ENGINE = MergeTree ORDER BY tuple(lastSuccessfullExecutionAt) SETTINGS allow_nullable_key = 1;";
  public static final String CLUSTER_DATA_TABLE_CREATION_QUERY =
      "CREATE TABLE IF NOT EXISTS ccm.%s (`starttime` Int64 NOT NULL, `endtime` Int64 NOT NULL, `accountid` String NOT NULL,     `settingid` String NULL,     `instanceid` String NOT NULL,     `instancetype` String NOT NULL,     `billingaccountid` String NULL,     `clusterid` String NULL,     `clustername` String NULL,     `appid` String NULL,     `serviceid` String NULL,     `envid` String NULL,     `appname` String NULL,     `servicename` String NULL,     `envname` String NULL,     `cloudproviderid` String NULL,     `parentinstanceid` String NULL,     `region` String NULL,     `launchtype` String NULL,     `clustertype` String NULL,     `workloadname` String NULL,     `workloadtype` String NULL,     `namespace` String NULL,     `cloudservicename` String NULL,     `taskid` String NULL,     `cloudprovider` String NULL,     `billingamount` Float NOT NULL,     `cpubillingamount` Float NULL,     `memorybillingamount` Float NULL,     `idlecost` Float NULL,     `cpuidlecost` Float NULL,     `memoryidlecost` Float NULL,     `usagedurationseconds` Float NULL,     `cpuunitseconds` Float NULL,     `memorymbseconds` Float NULL,     `maxcpuutilization` Float NULL,     `maxmemoryutilization` Float NULL,     `avgcpuutilization` Float NULL,     `avgmemoryutilization` Float NULL,     `systemcost` Float NULL,     `cpusystemcost` Float NULL,     `memorysystemcost` Float NULL,     `actualidlecost` Float NULL,     `cpuactualidlecost` Float NULL,     `memoryactualidlecost` Float NULL,     `unallocatedcost` Float NULL,     `cpuunallocatedcost` Float NULL,     `memoryunallocatedcost` Float NULL,     `instancename` String NULL,     `cpurequest` Float NULL,     `memoryrequest` Float NULL,     `cpulimit` Float NULL,     `memorylimit` Float NULL,     `maxcpuutilizationvalue` Float NULL,     `maxmemoryutilizationvalue` Float NULL,     `avgcpuutilizationvalue` Float NULL,     `avgmemoryutilizationvalue` Float NULL,     `networkcost` Float NULL,     `pricingsource` String NULL,     `storageactualidlecost` Float NULL,     `storageunallocatedcost` Float NULL,     `storageutilizationvalue` Float NULL,     `storagerequest` Float NULL,     `storagembseconds` Float NULL,     `storagecost` Float NULL,     `maxstorageutilizationvalue` Float NULL,     `maxstoragerequest` Float NULL,     `orgIdentifier` String NULL,     `projectIdentifier` String NULL,     `usagestarttime` Int64 NULL,     `usagestoptime` Int64 NULL,     `labels` Map(String, String) ) ENGINE = MergeTree PARTITION BY toStartOfInterval(toDate(starttime), toIntervalDay(1)) ORDER BY tuple()";
  public static final String CLUSTER_DATA_AGGREGATED_TABLE_CREATION_QUERY =
      "CREATE TABLE IF NOT EXISTS ccm.%s (`starttime` Int64 NOT NULL, `endtime` Int64 NOT NULL, `accountid` String NOT NULL,     `instancetype` String NOT NULL,     `instancename` String NULL,     `clustername` String NULL,     `billingamount` Float NOT NULL,     `actualidlecost` Float NULL,     `unallocatedcost` Float NULL,     `systemcost` Float NULL,     `clusterid` String NULL,     `clustertype` String NULL,     `region` String NULL,     `workloadname` String NULL,     `workloadtype` String NULL,     `namespace` String NULL,     `appid` String NULL,     `serviceid` String NULL,     `envid` String NULL,     `cloudproviderid` String NULL,     `launchtype` String NULL,     `cloudservicename` String NULL,     `storageactualidlecost` Float NULL,     `cpuactualidlecost` Float NULL,     `memoryactualidlecost` Float NULL,     `storageunallocatedcost` Float NULL,     `memoryunallocatedcost` Float NULL,     `cpuunallocatedcost` Float NULL,     `storagecost` Float NULL,     `cpubillingamount` Float NULL,     `memorybillingamount` Float NULL,     `storagerequest` Float NULL,     `storageutilizationvalue` Float NULL,     `instanceid` String NULL,     `networkcost` Float NULL,     `appname` String NULL,     `servicename` String NULL,     `envname` String NULL,     `cloudprovider` String NULL,     `maxstorageutilizationvalue` Float NULL,     `maxstoragerequest` Float NULL,     `orgIdentifier` String NULL,     `projectIdentifier` String NULL,      `usagestarttime` Int64 NULL,     `usagestoptime` Int64 NULL,     `usagedurationseconds` Float NULL,     `pricingsource` String NULL,     `labels` Map(String, String) ) ENGINE = MergeTree PARTITION BY toStartOfInterval(toDate(starttime), toIntervalDay(1)) ORDER BY tuple()";
  public static final String COST_AGGREGATED_INGESTION_QUERY =
      "INSERT INTO ccm.costAggregated (day, cost, cloudProvider, accountId) SELECT date_trunc('day', startTime) AS day, sum(cost) AS cost, concat(clustertype, '_', clustercloudprovider) AS cloudProvider, accountid AS accountId FROM ccm.unifiedTable WHERE (toDate(startTime) = toDate('%s')) AND (clustercloudprovider = 'CLUSTER') AND (clustertype = 'K8S') GROUP BY day, clustertype, accountid, clustercloudprovider";
  public static final String COST_AGGREGATED_DELETION_QUERY =
      "DELETE FROM ccm.costAggregated WHERE toDate(day) = toDate('%s') AND cloudProvider like 'K8S_%%' AND accountId = '%s';";
  public static final String CLUSTER_DATA_INGESTION_QUERY =
      "INSERT INTO ccm.%s ( starttime,  endtime,  accountid,  settingid,  instanceid,  instancetype,  billingaccountid,  clusterid,  clustername,  appid,  serviceid,  envid,  appname,  servicename,  envname,  cloudproviderid,  parentinstanceid,  region,  launchtype,  clustertype,  workloadname,  workloadtype,  namespace,  cloudservicename,  taskid,  cloudprovider,  billingamount,  cpubillingamount,  memorybillingamount,  idlecost,  cpuidlecost,  memoryidlecost,  usagedurationseconds,  cpuunitseconds,  memorymbseconds,  maxcpuutilization,  maxmemoryutilization,  avgcpuutilization,  avgmemoryutilization,  systemcost,  cpusystemcost,  memorysystemcost,  actualidlecost,  cpuactualidlecost,  memoryactualidlecost,  unallocatedcost,  cpuunallocatedcost,  memoryunallocatedcost,  instancename,  cpurequest,  memoryrequest,  cpulimit,  memorylimit,  maxcpuutilizationvalue,  maxmemoryutilizationvalue,  avgcpuutilizationvalue,  avgmemoryutilizationvalue,  networkcost,  pricingsource,  storageactualidlecost,  storageunallocatedcost,  storageutilizationvalue,  storagerequest,  storagembseconds,  storagecost,  maxstorageutilizationvalue,  maxstoragerequest,  orgIdentifier,  projectIdentifier, usagestarttime, usagestoptime, labels) VALUES ( ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?,  ?)";
  public static final String UNIFIED_TABLE_DELETION_QUERY =
      "DELETE FROM ccm.unifiedTable WHERE toDate(startTime) = toDate('%s') AND cloudProvider = 'CLUSTER'";
  public static final String CLUSTER_DATA_DELETION_QUERY = "DELETE FROM ccm.%s WHERE starttime = %d";
  public static final String UNIFIED_TABLE_INGESTION_QUERY =
      "INSERT INTO ccm.unifiedTable (cloudProvider, product, startTime, endtime, cost, cpubillingamount, memorybillingamount, actualidlecost, systemcost, unallocatedcost, networkcost, clustercloudprovider, accountid, clusterid, clustername, clustertype, region, namespace, workloadname, workloadtype, instancetype, appid, serviceid, envid, cloudproviderid, launchtype, cloudservicename, orgIdentifier, projectIdentifier, labels)  SELECT 'CLUSTER' AS cloudProvider, if(clustertype = 'K8S', 'Kubernetes Cluster', 'ECS Cluster') AS product, date_trunc('day', toDateTime64(starttime / 1000, 3, 'UTC')) AS startTime, date_trunc('day', toDateTime64(endtime / 1000, 3, 'UTC')) AS endtime, SUM(billingamount) AS cost, SUM(cpubillingamount) AS cpubillingamount, SUM(memorybillingamount) AS memorybillingamount, SUM(actualidlecost) AS actualidlecost, SUM(systemcost) AS systemcost, SUM(unallocatedcost) AS unallocatedcost, SUM(networkcost) AS networkcost, cloudprovider AS clustercloudprovider, accountid AS accountid, clusterid AS clusterid, clustername AS clustername, clustertype AS clustertype, region AS region, namespace AS namespace, workloadname AS workloadname,      workloadtype AS workloadtype,      instancetype AS instancetype,      appname AS appid,      servicename AS serviceid,      envname AS envid,      cloudproviderid AS cloudproviderid,      launchtype AS launchtype,      cloudservicename AS cloudservicename,      orgIdentifier,      projectIdentifier,      any(labels) AS labels  FROM ccm.%s  WHERE (toDate(date_trunc('day', toDateTime64(starttime / 1000, 3, 'UTC'))) = toDate('%s')) AND (instancetype != 'CLUSTER_UNALLOCATED')  GROUP BY      accountid,      clusterid,      clustername,      clustertype,      region,      namespace,      workloadname,      workloadtype,      instancetype,      appid,      serviceid,      envid,      cloudproviderid,      launchtype,      cloudservicename,      startTime,      endtime,      clustercloudprovider,      orgIdentifier,      projectIdentifier";
  public static final String COST_AGGREGATION_INGESTION_QUERY =
      "INSERT INTO ccm.%s (memoryactualidlecost, cpuactualidlecost, starttime, endtime, billingamount, actualidlecost, unallocatedcost, systemcost, storageactualidlecost, storageunallocatedcost, storageutilizationvalue, storagerequest, storagecost, memoryunallocatedcost, cpuunallocatedcost, cpubillingamount, memorybillingamount, accountid, clusterid, clustername, clustertype, region, namespace, workloadname, workloadtype, instancetype, appid, serviceid, envid, cloudproviderid, launchtype, cloudservicename, instancename, cloudprovider, networkcost, appname, servicename, envname, orgIdentifier, projectIdentifier, usagestarttime, usagestoptime, usagedurationseconds, pricingsource, labels) SELECT     SUM(memoryactualidlecost) AS memoryactualidlecost,     SUM(cpuactualidlecost) AS cpuactualidlecost,     starttime,     max(endtime) AS endtime,     sum(billingamount) AS billingamount,     sum(actualidlecost) AS actualidlecost,     sum(unallocatedcost) AS unallocatedcost,     sum(systemcost) AS systemcost,     SUM(storageactualidlecost) AS storageactualidlecost,     SUM(storageunallocatedcost) AS storageunallocatedcost,     MAX(storageutilizationvalue) AS storageutilizationvalue,     MAX(storagerequest) AS storagerequest,     SUM(storagecost) AS storagecost,     SUM(memoryunallocatedcost) AS memoryunallocatedcost,     SUM(cpuunallocatedcost) AS cpuunallocatedcost,     SUM(cpubillingamount) AS cpubillingamount,     SUM(memorybillingamount) AS memorybillingamount,     accountid,     clusterid,     clustername,     clustertype,     region,     namespace,     workloadname,     workloadtype,     instancetype,     appid,     serviceid,     envid,     cloudproviderid,     launchtype,     cloudservicename,     instancename,     cloudprovider,     SUM(networkcost) AS networkcost,     appname,     servicename,     envname,     orgIdentifier,     projectIdentifier,     min(usagestarttime),     max(usagestoptime),     max(usagedurationseconds),     pricingsource,     any(labels) AS labels FROM ccm.%s WHERE starttime = %d AND (instancetype IN ('K8S_POD', 'ECS_CONTAINER_INSTANCE', 'ECS_TASK_EC2', 'ECS_TASK_FARGATE', 'K8S_POD_FARGATE')) GROUP BY     starttime,     accountid,     clusterid,     clustername,     clustertype,     region,     namespace,     workloadname,     workloadtype,     instancetype,     appid,     serviceid,     envid,     cloudproviderid,     launchtype,     cloudservicename,     instancename,     cloudprovider,     appname,     servicename,     envname,     orgIdentifier,     projectIdentifier,     pricingsource ";
  public static final String CLUSTER_DATA_AGGREGATED_INGESTION_QUERY =
      "INSERT INTO ccm.%s (memoryactualidlecost, cpuactualidlecost, starttime, endtime, billingamount, actualidlecost, unallocatedcost, systemcost, storageactualidlecost, storageunallocatedcost, storageutilizationvalue, storagerequest, storagecost, memoryunallocatedcost, cpuunallocatedcost, cpubillingamount, memorybillingamount, accountid, clusterid, clustername, clustertype, region, namespace, workloadname, workloadtype, instancetype, appid, serviceid, envid, cloudproviderid, launchtype, cloudservicename, instanceid, instancename, cloudprovider, networkcost, appname, servicename, envname, orgIdentifier, projectIdentifier, usagestarttime, usagestoptime, usagedurationseconds, pricingsource, labels) SELECT     SUM(memoryactualidlecost) AS memoryactualidlecost,     SUM(cpuactualidlecost) AS cpuactualidlecost,     starttime,     max(endtime) AS endtime,     sum(billingamount) AS billingamount,     sum(actualidlecost) AS actualidlecost,     sum(unallocatedcost) AS unallocatedcost,     sum(systemcost) AS systemcost,     SUM(storageactualidlecost) AS storageactualidlecost,     SUM(storageunallocatedcost) AS storageunallocatedcost,     MAX(storageutilizationvalue) AS storageutilizationvalue,     MAX(storagerequest) AS storagerequest,     SUM(storagecost) AS storagecost,     SUM(memoryunallocatedcost) AS memoryunallocatedcost,     SUM(cpuunallocatedcost) AS cpuunallocatedcost,     SUM(cpubillingamount) AS cpubillingamount,     SUM(memorybillingamount) AS memorybillingamount,     accountid,     clusterid,     clustername,     clustertype,     region,     namespace,     workloadname,     workloadtype,     instancetype,     appid,     serviceid,     envid,     cloudproviderid,     launchtype,     cloudservicename,     instanceid,     instancename,     cloudprovider,     SUM(networkcost) AS networkcost,     appname,     servicename,     envname,     orgIdentifier,     projectIdentifier,     min(usagestarttime),     max(usagestoptime),     max(usagedurationseconds),     pricingsource,     any(labels) AS labels FROM ccm.%s WHERE starttime = %d AND (instancetype IN ('K8S_NODE', 'K8S_PV')) GROUP BY     starttime,     accountid,     clusterid,     clustername,     clustertype,     region,     namespace,     workloadname,     workloadtype,     instancetype,     appid,     serviceid,     envid,     cloudproviderid,     launchtype,     cloudservicename,     instanceid,     instancename,     cloudprovider,     appname,     servicename,     envname,     orgIdentifier,     projectIdentifier,     pricingsource ";
  public static final String GET_CLUSTER_DATA_ENTRIES =
      "SELECT COUNT(*) as ENTRIESCOUNT, SUM(billingamount) as BILLINGAMOUNTSUM from ccm.clusterData WHERE accountid = '%s' AND starttime = '%s'";
}
