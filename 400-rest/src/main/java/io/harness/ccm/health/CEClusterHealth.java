package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.cluster.entities.ClusterRecord;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public class CEClusterHealth {
  boolean isHealthy;
  String clusterId;
  String clusterName;
  ClusterRecord clusterRecord;
  List<String> messages;
  List<String> errors;
  Long lastEventTimestamp;
}
