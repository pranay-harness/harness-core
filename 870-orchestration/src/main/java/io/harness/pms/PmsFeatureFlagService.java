package io.harness.pms;

import io.harness.beans.FeatureName;

import javax.validation.constraints.NotNull;

public interface PmsFeatureFlagService {
  boolean isEnabled(String accountId, @NotNull FeatureName featureName);

  boolean isEnabled(String accountId, @NotNull String featureName);
}
