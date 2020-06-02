package io.harness.ccm.health;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CEHealthStatus {
  boolean isHealthy;
  boolean isCEConnector;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
