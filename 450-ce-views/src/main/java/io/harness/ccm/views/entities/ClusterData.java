/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@OwnedBy(CE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterData {
  String id;
  String name;
  String type;
  Double totalCost;
  Double idleCost;
  Double networkCost;
  Double systemCost;
  Double cpuIdleCost;
  Double cpuActualIdleCost;
  Double memoryIdleCost;
  Double memoryActualIdleCost;
  Double costTrend;
  String trendType;
  String region;
  String launchType;
  String cloudServiceName;
  String taskId;
  String workloadName;
  String workloadType;
  String namespace;
  String clusterType;
  String clusterId;
  String instanceId;
  String instanceName;
  String instanceType;
  String environment;
  String cloudProvider;
  Double maxCpuUtilization;
  Double maxMemoryUtilization;
  Double avgCpuUtilization;
  Double avgMemoryUtilization;
  Double unallocatedCost;
  Double prevBillingAmount;
  String appName;
  String appId;
  String serviceName;
  String serviceId;
  String envId;
  String envName;
  String cloudProviderId;
  String clusterName;
  Double storageCost;
  Double memoryBillingAmount;
  Double cpuBillingAmount;
  Double storageUnallocatedCost;
  Double memoryUnallocatedCost;
  Double cpuUnallocatedCost;
  Double storageRequest;
  Double storageUtilizationValue;
  Double storageActualIdleCost;
  int efficiencyScore;
  int efficiencyScoreTrendPercentage;
}
