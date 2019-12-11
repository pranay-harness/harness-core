package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sGranularUtilizationData {
  private String accountId;
  private String instanceId;
  private String instanceType;
  private String settingId;
  private long cpu;
  private long memory;
  private long endTimestamp;
  private long startTimestamp;
}
