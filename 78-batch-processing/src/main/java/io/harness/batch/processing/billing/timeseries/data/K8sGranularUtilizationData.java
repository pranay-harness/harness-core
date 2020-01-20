package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sGranularUtilizationData {
  private String accountId;
  private String instanceId;
  private String instanceType;
  private String clusterId;
  private double cpu;
  private double memory;
  private long endTimestamp;
  private long startTimestamp;
}
