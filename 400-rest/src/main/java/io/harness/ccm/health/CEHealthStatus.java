package io.harness.ccm.health;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class CEHealthStatus {
  boolean isHealthy;
  boolean isCEConnector;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
