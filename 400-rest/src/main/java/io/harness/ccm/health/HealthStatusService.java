package io.harness.ccm.health;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.IdentifierKeys;
@TargetModule(Module._490_CE_COMMONS)
public interface HealthStatusService {
  String CLUSTER_ID_IDENTIFIER = IdentifierKeys.PREFIX + "clusterId";
  CEHealthStatus getHealthStatus(String cloudProviderId);
  CEHealthStatus getHealthStatus(String cloudProviderId, boolean cloudCostEnabled);
}
