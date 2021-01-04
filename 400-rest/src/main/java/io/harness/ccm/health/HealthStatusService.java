package io.harness.ccm.health;

import io.harness.grpc.IdentifierKeys;

public interface HealthStatusService {
  String CLUSTER_ID_IDENTIFIER = IdentifierKeys.PREFIX + "clusterId";
  CEHealthStatus getHealthStatus(String cloudProviderId);
  CEHealthStatus getHealthStatus(String cloudProviderId, boolean cloudCostEnabled);
}
