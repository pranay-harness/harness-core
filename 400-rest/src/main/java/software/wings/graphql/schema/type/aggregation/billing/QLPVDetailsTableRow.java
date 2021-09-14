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
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
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
