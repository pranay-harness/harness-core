package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InstanceBillingData {
  private String appId;
  private String envId;
  private String region;
  private String serviceId;
  private String accountId;
  private String instanceId;
  private String launchType;
  private String clusterName;
  private String clusterType;
  private String instanceType;
  private String workloadName;
  private String workloadType;
  private String billingAccountId;
  private String parentInstanceId;
  private String cloudProviderId;
  private String cloudProvider;
  private BigDecimal billingAmount;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
  private double usageDurationSeconds;
  private long endTimestamp;
  private long startTimestamp;
}
