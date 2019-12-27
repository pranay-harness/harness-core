package software.wings.service.intfc;

import software.wings.beans.FeatureName;

public interface FeatureFlagService {
  boolean isGlobalEnabled(FeatureName featureName);
  boolean isEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();

  boolean isEnabledReloadCache(FeatureName featureName, String accountId);

  void enableAccount(FeatureName featureName, String accountId);
}
