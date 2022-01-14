package io.harness.ff;

import io.harness.beans.FeatureName;

public class CIFeatureFlagNoopServiceImpl implements CIFeatureFlagService {
  @Override
  public boolean isEnabled(FeatureName featureName, String accountId) {
    return false;
  }
}
