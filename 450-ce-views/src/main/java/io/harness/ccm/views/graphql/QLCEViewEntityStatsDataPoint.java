/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.ClusterData;
import io.harness.ccm.views.entities.InstanceDetails;
import io.harness.ccm.views.entities.StorageDetails;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@OwnedBy(CE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewEntityStatsDataPoint {
  String name;
  String id;
  String pricingSource;
  Number cost;
  Number costTrend;
  boolean isClusterPerspective;
  ClusterData clusterData;
  InstanceDetails instanceDetails;
  StorageDetails storageDetails;
}
