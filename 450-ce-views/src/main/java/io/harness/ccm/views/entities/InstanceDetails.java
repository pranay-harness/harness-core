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
public class InstanceDetails {
  String name;
  String id;
  String nodeId;
  String namespace;
  String workload;
  String clusterName;
  String clusterId;
  String node;
  String nodePoolName;
  String cloudProviderInstanceId;
  String podCapacity;
  double totalCost;
  double idleCost;
  double systemCost;
  double networkCost;
  double unallocatedCost;
  double cpuAllocatable;
  double memoryAllocatable;
  double cpuRequested;
  double memoryRequested;
  double cpuUnitPrice;
  double memoryUnitPrice;
  String instanceCategory;
  String machineType;
  long createTime;
  long deleteTime;
  String qosClass;
  double memoryBillingAmount;
  double cpuBillingAmount;
  double storageUnallocatedCost;
  double memoryUnallocatedCost;
  double cpuUnallocatedCost;
  double memoryIdleCost;
  double cpuIdleCost;
  double storageCost;
  double storageActualIdleCost;
  double storageUtilizationValue;
  double storageRequest;
}
