/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CEDataEntryKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLCEDataEntry implements QLData {
  Double totalCost;
  Double idleCost;
  Double unallocatedCost;
  Double systemCost;
  Double avgCpuUtilization;
  Double avgMemoryUtilization;
  Double cpuRequest;
  Double memoryRequest;
  Double cpuLimit;
  Double memoryLimit;
  String region;
  QLCEK8sEntity k8s;
  QLCEEcsEntity ecs;
  QLCEHarnessEntity harness;
  String clusterType;
  String cluster;
  String clusterId;
  String instanceType;
  Long startTime;
  String labelName;
  String labelValue;
  String tagName;
  String tagValue;
}
