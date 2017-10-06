package software.wings.service.intfc;

import software.wings.beans.FeatureFlag.FeatureName;

public interface FeatureFlagService {
  boolean isEnabled(FeatureName name, String accountId);

  void initializeMissingFlags();
}
