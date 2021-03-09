package io.harness.ccm.health;

import io.harness.ccm.cluster.entities.ClusterRecord;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CEClusterHealth {
  boolean isHealthy;
  String clusterId;
  String clusterName;
  ClusterRecord clusterRecord;
  List<String> messages;
  List<String> errors;
  Long lastEventTimestamp;
}
