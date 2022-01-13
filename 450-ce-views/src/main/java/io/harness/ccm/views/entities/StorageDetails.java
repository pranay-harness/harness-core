/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
