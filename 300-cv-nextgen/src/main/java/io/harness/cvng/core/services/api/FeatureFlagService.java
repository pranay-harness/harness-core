package io.harness.cvng.core.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CV)
public interface FeatureFlagService {
  boolean isFeatureFlagEnabled(String accountId, String name);
}
