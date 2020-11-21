package io.harness.ccm.health;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CEHealthStatus {
  boolean isHealthy;
  boolean isCEConnector;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
