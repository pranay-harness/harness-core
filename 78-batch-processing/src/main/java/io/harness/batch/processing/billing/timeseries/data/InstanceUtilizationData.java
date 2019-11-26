package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceUtilizationData {
  private String instanceId;
  private String instanceType;
  private String clusterName;
  private String clusterArn;
  private String serviceName;
  private String serviceArn;
  private String settingId;
  private double cpuUtilizationAvg;
  private double cpuUtilizationMax;
  private double memoryUtilizationAvg;
  private double memoryUtilizationMax;

  private long endTimestamp;
  private long startTimestamp;
}
