package io.harness.ccm.anomaly.entities;

import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyFeedback;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnomalyEntity {
  String id;
  String accountId;

  Instant anomalyTime;

  Double actualCost;
  Double expectedCost;
  Double anomalyScore;

  QLAnomalyFeedback feedback;

  AnomalyDetectionModel reportedBy;

  String note;
  TimeGranularity timeGranularity;

  String region;
  String cloudProvider;

  String clusterId;
  String clusterName;
  String workloadName;
  String workloadType;
  String namespace;

  String gcpProject;
  String gcpSKUId;
  String gcpSKUDescription;
  String gcpProduct;

  String awsAccount;
  String awsService;
  String awsInstanceType;
  String awsUsageType;

  public static class AnomaliesDataTableSchema {
    enum DataType { STRING, INTEGER, TIMESTAMP, DOUBLE, BOOLEAN }

    public static List<fields> getFields() {
      return Arrays.asList(fields.class.getEnumConstants());
    }

    public enum fields {
      ID("id", DataType.STRING),
      ACCOUNT_ID("accountid", DataType.STRING),
      ACTUAL_COST("actualcost", DataType.DOUBLE),
      EXPECTED_COST("expectedcost", DataType.DOUBLE),
      REGION("region", DataType.STRING),
      NOTE("note", DataType.STRING),
      FEED_BACK("feedback", DataType.STRING),
      ANOMALY_TIME("anomalytime", DataType.TIMESTAMP),
      TIME_GRANULARITY("timegranularity", DataType.STRING),
      CLUSTER_ID("clusterid", DataType.STRING),
      CLUSTER_NAME("clustername", DataType.STRING),
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

      private DataType dataType;
      private String fieldName;

      fields(String fieldName, DataType dataType) {
        this.fieldName = fieldName;
        this.dataType = dataType;
      }

      public DataType getDataType() {
        return dataType;
      }

      public String getFieldName() {
        return fieldName;
      }
    }

    public static DbSpec spec;
    public static DbSchema schema;
    public static DbTable table;

    public static DbColumn id;
    public static DbColumn accountId;

    public static DbColumn actualCost;
    public static DbColumn expectedCost;

    public static DbColumn anomalyTime;
    public static DbColumn timeGranularity;

    public static DbColumn note;
    public static DbColumn feedBack;

    public static DbColumn clusterId;
    public static DbColumn clusterName;

    public static DbColumn workloadName;
    public static DbColumn workloadType;

    public static DbColumn namespace;

    public static DbColumn region;

    public static DbColumn gcpProject;
    public static DbColumn gcpProduct;
    public static DbColumn gcpSkuId;
    public static DbColumn gcpSkuDescription;

    public static DbColumn awsAccount;
    public static DbColumn awsService;
    public static DbColumn awsUsageType;
    public static DbColumn awsInstanceType;

    public static DbColumn anomalyScore;
    public static DbColumn reportedBy;

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
      feedBack = table.addColumn("feedback");
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
}