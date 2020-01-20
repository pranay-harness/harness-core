package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanceUtilizationData {
  private String accountId;
  private String instanceId;
  private String instanceType;
  private String clusterId;
  private double cpuUtilizationAvg;
  private double cpuUtilizationMax;
  private double memoryUtilizationAvg;
  private double memoryUtilizationMax;
  private double cpuUtilizationAvgValue;
  private double cpuUtilizationMaxValue;
  private double memoryUtilizationAvgValue;
  private double memoryUtilizationMaxValue;
  private long endTimestamp;
  private long startTimestamp;
}
