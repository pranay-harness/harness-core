package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class CEHealthStatus {
  boolean isHealthy;
  boolean isCEConnector;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
