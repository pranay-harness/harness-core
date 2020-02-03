package io.harness.ccm.health;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CEClusterHealth {
  String clusterId;
  ClusterRecord clusterRecord;
  List<String> errors;
  Long lastEventTimestamp;
}
