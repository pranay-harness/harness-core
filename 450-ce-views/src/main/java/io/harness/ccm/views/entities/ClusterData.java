package io.harness.ccm.views.entities;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterData {
  String id;
  String name;
  String type;
  Double totalCost;
  Double idleCost;
  Double networkCost;
  Double cpuIdleCost;
  Double memoryIdleCost;
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
