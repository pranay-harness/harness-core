/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.activePods;

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
@FieldNameConstants(innerTypeName = "CeActivePodCountTableKeys")
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CeActivePodCountTableSchema {
  DbSpec dbSpec;
  DbSchema dbSchema;
  DbTable activePodCountTable;
  DbColumn startTime;
  DbColumn endTime;
  DbColumn accountId;
  DbColumn clusterId;
  DbColumn instanceId;
  DbColumn podCount;

  public CeActivePodCountTableSchema() {
    dbSpec = new DbSpec();
    dbSchema = dbSpec.addDefaultSchema();
    activePodCountTable = dbSchema.addTable("active_pod_count");
    startTime = activePodCountTable.addColumn("starttime", "timestamp", null);
    endTime = activePodCountTable.addColumn("endtime", "timestamp", null);
    accountId = activePodCountTable.addColumn("accountid", "text", null);
    clusterId = activePodCountTable.addColumn("clusterid", "text", null);
    instanceId = activePodCountTable.addColumn("instanceId", "text", null);
    podCount = activePodCountTable.addColumn("podcount", "double", null);
  }
}
