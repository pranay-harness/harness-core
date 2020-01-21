package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Data
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLEntityTableData implements QLData {
  String id;
  String name;
  String type;
  Double totalCost;
  Double idleCost;
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
  int totalNamespaces;
  int totalWorkloads;
  Double maxCpuUtilization;
  Double maxMemoryUtilization;
  Double avgCpuUtilization;
  Double avgMemoryUtilization;
  Double unallocatedCost;
}