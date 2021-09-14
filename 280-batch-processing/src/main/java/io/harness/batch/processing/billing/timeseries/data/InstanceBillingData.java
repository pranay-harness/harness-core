/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.timeseries.data;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
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
  private String pricingSource;
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
  private double networkCost;
  private double maxCpuUtilization;
  private double maxMemoryUtilization;
  private double avgCpuUtilization;
  private double avgMemoryUtilization;
  private double maxCpuUtilizationValue;
  private double maxMemoryUtilizationValue;
  private double avgCpuUtilizationValue;
  private double avgMemoryUtilizationValue;
  private double cpuRequest;
  private double cpuLimit;
  private double memoryRequest;
  private double memoryLimit;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
  private double usageDurationSeconds;
  private long endTimestamp;
  private long startTimestamp;

  private BigDecimal storageBillingAmount;
  private BigDecimal storageActualIdleCost;
  private BigDecimal storageUnallocatedCost;
  private double storageUtilizationValue;
  private double storageRequest;
  private double maxStorageUtilizationValue;
  private double maxStorageRequest;
  private double storageMbSeconds;
}
