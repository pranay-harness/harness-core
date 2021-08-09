package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import io.harness.ccm.anomaly.entities.TimeGranularity;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PythonInput {
  String id;
  String accountId;
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

  APITimeSeries data;

  @Builder
  static class APITimeSeries {
    List<DataPoint> train;
    List<DataPoint> test;
  }

  @Builder
  static class DataPoint {
    Long time;
    Double y;
  }
}
