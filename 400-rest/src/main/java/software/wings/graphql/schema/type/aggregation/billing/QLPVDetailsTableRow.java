package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLPVDetailsTableRow {
  String id;
  String instanceId;
  String instanceName;
  String claimName;
  String claimNamespace;
  String clusterName;
  String clusterId;
  String storageClass;
  String volumeType;
  String cloudProvider;
  String region;
  double storageCost;
  double storageActualIdleCost;
  double storageUnallocatedCost;
  double capacity;
  double storageRequest;
  double storageUtilizationValue;
  long createTime;
  long deleteTime;
}
