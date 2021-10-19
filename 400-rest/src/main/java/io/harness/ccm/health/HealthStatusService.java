package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CE)
public interface HealthStatusService {
  CEHealthStatus getHealthStatus(String cloudProviderId);
  CEHealthStatus getHealthStatus(String cloudProviderId, boolean cloudCostEnabled);
}
