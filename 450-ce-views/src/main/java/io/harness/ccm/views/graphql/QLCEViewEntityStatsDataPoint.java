/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
