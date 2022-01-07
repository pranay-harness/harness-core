/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLEntityTableData implements QLData {
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
  String workloadName;
  String workloadType;
  String namespace;
  String clusterType;
  String clusterId;
  String environment;
  String cloudProvider;
  String label;
  int totalNamespaces;
  int totalWorkloads;
  Double maxCpuUtilization;
  Double maxMemoryUtilization;
  Double avgCpuUtilization;
  Double avgMemoryUtilization;
  Double unallocatedCost;
  Double prevBillingAmount;
  String appName;
  String appId;
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
