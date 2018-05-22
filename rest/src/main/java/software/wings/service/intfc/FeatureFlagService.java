package software.wings.service.intfc;

import software.wings.beans.FeatureName;

public interface FeatureFlagService {
  boolean isEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();

  boolean isEnabledRelaodCache(FeatureName featureName, String accountId);
}
