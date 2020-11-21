package software.wings.graphql.datafetcher.anomaly;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import java.util.Arrays;
import java.util.List;
import lombok.Value;
import lombok.experimental.UtilityClass;

@Value
@UtilityClass
public class AnomaliesDataTableSchema {
  enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE, BOOLEAN }

  public List<fields> getFields() {
    return Arrays.asList(fields.class.getEnumConstants());
  }

  public enum fields {
    ID("id", DataType.STRING),
    ACCOUNT_ID("accountid", DataType.STRING),
    ACTUAL_COST("actualcost", DataType.DOUBLE),
    EXPECTED_COST("expectedcost", DataType.DOUBLE),
    REGION("region", DataType.STRING),
    NOTE("note", DataType.STRING),
    ANOMALY_TIME("anomalytime", DataType.TIMESTAMP),
    TIME_GRANULARITY("timegranularity", DataType.STRING),
    CLUSTER_ID("clsuterid", DataType.STRING),
    CLUSTER_NAME("clsutername", DataType.STRING),
    NAMESPACE("namespace", DataType.STRING),
    WORKLOAD_TYPE("workloadtype", DataType.STRING),
    WORKLOAD_NAME("workloadname", DataType.STRING),
    GCP_PROJECT("gcpproject", DataType.STRING),
    GCP_PRODUCT("gcpproduct", DataType.STRING),
    GCP_SKU_ID("gcpskuid", DataType.STRING),
    GCP_SKU_DESCRIPTION("gcpskudescription", DataType.STRING),
    AWS_ACCOUNT("awsaccount", DataType.STRING),
    AWS_SERVICE("awsservice", DataType.STRING),
    AWS_USAGE_TYPE("awsusagetype", DataType.STRING),
    AWS_INSTANCE_TYPE("awsinstancetype", DataType.STRING),
    ANOMALY_SCORE("anomalyScore", DataType.DOUBLE),
    REPORTED_BY("reportedby", DataType.STRING);

    private AnomaliesDataTableSchema.DataType dataType;
    private String fieldName;

    fields(String fieldName, AnomaliesDataTableSchema.DataType dataType) {
      this.fieldName = fieldName;
      this.dataType = dataType;
    }

    public AnomaliesDataTableSchema.DataType getDataType() {
      return dataType;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public DbSpec spec;
  public DbSchema schema;
  public DbTable table;

  public DbColumn id;
  public DbColumn accountId;

  public DbColumn actualCost;
  public DbColumn expectedCost;

  public DbColumn anomalyTime;
  public DbColumn timeGranularity;

  public DbColumn note;

  public DbColumn clusterId;
  public DbColumn clusterName;

  public DbColumn workloadName;
  public DbColumn workloadType;

  public DbColumn namespace;

  public DbColumn region;

  public DbColumn gcpProject;
  public DbColumn gcpProduct;
  public DbColumn gcpSkuId;
  public DbColumn gcpSkuDescription;

  public DbColumn awsAccount;
  public DbColumn awsService;
  public DbColumn awsUsageType;
  public DbColumn awsInstanceType;

  public DbColumn anomalyScore;
  public DbColumn reportedBy;

  static {
    spec = new DbSpec();
    schema = spec.addDefaultSchema();
    table = schema.addTable("anomalies");

    id = table.addColumn("id");
    accountId = table.addColumn("accountid");

    actualCost = table.addColumn("actualcost");
    expectedCost = table.addColumn("expectedcost");

    anomalyTime = table.addColumn("anomalytime");
    timeGranularity = table.addColumn("timegranularity");

    note = table.addColumn("note");
    region = table.addColumn("region");

    clusterId = table.addColumn("clusterid");
    clusterName = table.addColumn("clustername");
    namespace = table.addColumn("namespace");
    workloadType = table.addColumn("workloadtype");
    workloadName = table.addColumn("workloadname");

    gcpProject = table.addColumn("gcpproject");
    gcpProduct = table.addColumn("gcpproduct");
    gcpSkuId = table.addColumn("gcpskuid");
    gcpSkuDescription = table.addColumn("gcpskudescription");

    awsAccount = table.addColumn("awsaccount");
    awsService = table.addColumn("awsservice");
    awsUsageType = table.addColumn("awsusagetype");
    awsInstanceType = table.addColumn("awsinstancetype");

    anomalyScore = table.addColumn("anomalyscore");
    reportedBy = table.addColumn("reportedby");
  }
}
