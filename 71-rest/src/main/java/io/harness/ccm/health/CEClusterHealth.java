package io.harness.ccm.health;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CEClusterHealth {
  boolean isHealthy;
  String clusterId;
  ClusterRecord clusterRecord;
  List<String> messages;
  List<String> errors;
  Long lastEventTimestamp;
}
