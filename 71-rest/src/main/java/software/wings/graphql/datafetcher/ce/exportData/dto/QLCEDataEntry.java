package software.wings.graphql.datafetcher.ce.exportData.dto;

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
public class QLCEDataEntry implements QLData {
  Double totalCost;
  Double idleCost;
  Double unallocatedCost;
  Double systemCost;
  Double maxCpuUtilization;
  Double maxMemoryUtilization;
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
  String instanceType;
  Long startTime;
  String labelName;
  String labelValue;
  String tagName;
  String tagValue;
}
