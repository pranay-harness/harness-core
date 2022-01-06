/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudefficiencyevents;

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
@FieldNameConstants(innerTypeName = "CEEventsTableKeys")
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CEEventsTableSchema {
  /**
   STARTTIME TIMESTAMPTZ NOT NULL,
   ACCOUNTID TEXT NOT NULL,
   SETTINGID TEXT,
   CLUSTERID TEXT,
   CLUSTERTYPE TEXT,
   INSTANCEID TEXT,
   INSTANCETYPE TEXT,
   APPID TEXT,
   SERVICEID TEXT,
   ENVID TEXT,
   CLOUDPROVIDERID TEXT,
   DEPLOYMENTID TEXT,
   CLOUDPROVIDER TEXT,
   EVENTDESCRIPTION TEXT,
   COSTEVENTTYPE TEXT,
   COSTEVENTSOURCE TEXT,
   NAMESPACE TEXT,
   WORKLOADNAME TEXT,
   WORKLOADTYPE TEXT,
   CLOUDSERVICENAME TEXT,
   TASKID TEXT,
   LAUNCHTYPE TEXT,
   BILLINGAMOUNT DOUBLE PRECISION
   );
   */
  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable ceEventsTable;
  DbColumn startTime;
  DbColumn accountId;
  DbColumn settingId;
  DbColumn clusterId;
  DbColumn clusterType;
  DbColumn instanceId;
  DbColumn instanceType;
  DbColumn serviceId;
  DbColumn appId;
  DbColumn cloudProviderId;
  DbColumn envId;
  DbColumn deploymentId;
  DbColumn cloudProvider;
  DbColumn namespace;
  DbColumn workloadName;
  DbColumn workloadType;
  DbColumn cloudServiceName;
  DbColumn taskId;
  DbColumn launchType;
  DbColumn billingAmount;
  DbColumn eventDescription;
  DbColumn costEventType;
  DbColumn costEventSource;
  DbColumn oldYamlRef;
  DbColumn newYamlRef;
  DbColumn cost_change_percent;

  private static String doubleType = "double";

  public CEEventsTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    ceEventsTable = dbSchema.addTable("cost_event_data");
    startTime = ceEventsTable.addColumn("starttime", "timestamp", null);
    accountId = ceEventsTable.addColumn("accountid", "text", null);
    settingId = ceEventsTable.addColumn("settingid", "text", null);
    instanceId = ceEventsTable.addColumn("instanceid", "text", null);
    instanceType = ceEventsTable.addColumn("instancetype", "text", null);
    serviceId = ceEventsTable.addColumn("serviceid", "text", null);
    appId = ceEventsTable.addColumn("appid", "text", null);
    cloudProviderId = ceEventsTable.addColumn("cloudproviderid", "text", null);
    envId = ceEventsTable.addColumn("envid", "text", null);
    clusterId = ceEventsTable.addColumn("clusterid", "text", null);
    launchType = ceEventsTable.addColumn("launchtype", "text", null);
    clusterType = ceEventsTable.addColumn("clustertype", "text", null);
    workloadName = ceEventsTable.addColumn("workloadname", "text", null);
    workloadType = ceEventsTable.addColumn("workloadtype", "text", null);
    cloudProvider = ceEventsTable.addColumn("cloudprovider", "text", null);
    billingAmount = ceEventsTable.addColumn("billingamount", doubleType, null);
    cloudServiceName = ceEventsTable.addColumn("cloudServiceName", "text", null);
    taskId = ceEventsTable.addColumn("taskId", "text", null);
    namespace = ceEventsTable.addColumn("namespace", "text", null);
    deploymentId = ceEventsTable.addColumn("namespace", "text", null);
    eventDescription = ceEventsTable.addColumn("eventdescription", "text", null);
    costEventSource = ceEventsTable.addColumn("costeventsource", "text", null);
    costEventType = ceEventsTable.addColumn("costeventtype", "text", null);
    oldYamlRef = ceEventsTable.addColumn("oldyamlref", "text", null);
    newYamlRef = ceEventsTable.addColumn("newyamlref", "text", null);
    cost_change_percent = ceEventsTable.addColumn("cost_change_percent", doubleType, null);
  }
}
