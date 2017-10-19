package software.wings.service.intfc;

import software.wings.beans.FeatureFlag.FeatureName;

public interface FeatureFlagService {
  boolean isEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();
}
