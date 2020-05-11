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
  private String cloudServiceName;
  private String accountId;
  private String instanceId;
  private String instanceName;
  private String clusterId;
  private String settingId;
  private String launchType;
  private String taskId;
  private String namespace;
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
  private BigDecimal cpuBillingAmount;
  private BigDecimal memoryBillingAmount;
  private BigDecimal idleCost;
  private BigDecimal cpuIdleCost;
  private BigDecimal memoryIdleCost;
  private BigDecimal systemCost;
  private BigDecimal cpuSystemCost;
  private BigDecimal memorySystemCost;
  private BigDecimal actualIdleCost;
  private BigDecimal unallocatedCost;
  private BigDecimal cpuActualIdleCost;
  private BigDecimal memoryActualIdleCost;
  private BigDecimal cpuUnallocatedCost;
  private BigDecimal memoryUnallocatedCost;
  private double maxCpuUtilization;
  private double maxMemoryUtilization;
  private double avgCpuUtilization;
  private double avgMemoryUtilization;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
  private double usageDurationSeconds;
  private long endTimestamp;
  private long startTimestamp;
}
