package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ClusterData;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewEntityStatsDataPoint {
  String name;
  String id;
  Number cost;
  Number costTrend;
  boolean isClusterPerspective;
  ClusterData clusterData;
}
