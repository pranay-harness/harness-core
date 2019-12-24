package io.harness.ccm.health;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CEHealthStatus {
  boolean isHealthy;
  List<ClusterRecord> clusterRecords;
  Map<String, List<CEError>> taskErrorMap;
}
