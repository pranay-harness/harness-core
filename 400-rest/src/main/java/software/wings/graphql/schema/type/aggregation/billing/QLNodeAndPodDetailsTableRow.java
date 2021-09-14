/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLNodeAndPodDetailsTableRow {
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
  double storageCost;
  double storageUtilizationValue;
  double storageRequest;
  double storageActualIdleCost;
  double memoryBillingAmount;
  double cpuBillingAmount;
  double storageUnallocatedCost;
  double memoryUnallocatedCost;
  double cpuUnallocatedCost;
  double memoryIdleCost;
  double cpuIdleCost;
}
