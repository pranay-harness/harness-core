package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Data
@Builder
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
  String clusterName;
  String instanceType;
  Long startTime;
}
