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
public class StorageDetails {
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
