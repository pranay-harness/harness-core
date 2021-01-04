package software.wings.graphql.datafetcher.ce.activePods;

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
