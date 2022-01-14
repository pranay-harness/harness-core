package io.harness.pms;

import io.harness.beans.FeatureName;

public class NoopFeatureFlagServiceImpl implements PmsFeatureFlagService {
  @Override
  public boolean isEnabled(String accountId, FeatureName featureName) {
    return false;
  }

  @Override
  public boolean isEnabled(String accountId, String featureName) {
    return false;
  }
}
