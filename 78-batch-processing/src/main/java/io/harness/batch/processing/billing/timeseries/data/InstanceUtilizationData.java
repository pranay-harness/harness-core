package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanceUtilizationData {
  private String accountId;
  private String instanceId;
  private String instanceType;
  private String settingId;
  private double cpuUtilizationAvg;
  private double cpuUtilizationMax;
  private double memoryUtilizationAvg;
  private double memoryUtilizationMax;
  private long endTimestamp;
  private long startTimestamp;
}
