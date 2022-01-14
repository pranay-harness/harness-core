package io.harness.ff;

import io.harness.beans.FeatureName;

import javax.validation.constraints.NotNull;

public interface CIFeatureFlagService {
  boolean isEnabled(@NotNull FeatureName featureName, String accountId);
}
