package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.IdentifierKeys;

@OwnedBy(CE)
public interface HealthStatusService {
  String CLUSTER_ID_IDENTIFIER = IdentifierKeys.PREFIX + "clusterId";
  String UID = IdentifierKeys.PREFIX + "uid";
  CEHealthStatus getHealthStatus(String cloudProviderId);
  CEHealthStatus getHealthStatus(String cloudProviderId, boolean cloudCostEnabled);
}
