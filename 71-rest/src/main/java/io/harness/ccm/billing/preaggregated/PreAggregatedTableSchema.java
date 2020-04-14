package io.harness.ccm.billing.preaggregated;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.Value;
import lombok.experimental.UtilityClass;

@Value
@UtilityClass
public class PreAggregatedTableSchema {
  public static final String defaultTableName = "<Project>.<DataSet>.<TableName>";
  public static final DbSpec spec;
  public static final DbSchema schema;
  public static final DbTable table;

  public static final DbColumn blendedRate;
  public static final DbColumn blendedCost;
  public static final DbColumn unBlendedRate;
  public static final DbColumn unBlendedCost;
  public static final DbColumn serviceCode;
  public static final DbColumn region;
  public static final DbColumn availabilityZone;
  public static final DbColumn usageAccountId;
  public static final DbColumn instanceType;
  public static final DbColumn usageType;
  public static final DbColumn startTime;

  static {
    spec = new DbSpec();
    schema = spec.addDefaultSchema();
    table = schema.addTable("`" + defaultTableName + "`");

    blendedRate = table.addColumn("blendedRate");
    blendedCost = table.addColumn("blendedCost");
    unBlendedRate = table.addColumn("unblendedRate");
    unBlendedCost = table.addColumn("unblendedCost");
    serviceCode = table.addColumn("servicecode");
    region = table.addColumn("region");
    availabilityZone = table.addColumn("availabilityzone");
    usageAccountId = table.addColumn("usageaccountid");
    instanceType = table.addColumn("instancetype");
    usageType = table.addColumn("usagetype");
    startTime = table.addColumn("startTime");
  }
}
