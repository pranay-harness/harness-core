package software.wings.service.intfc;

import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;

import java.util.Optional;

public interface FeatureFlagService {
  boolean isGlobalEnabled(FeatureName featureName);
  boolean isEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();

  boolean isEnabledReloadCache(FeatureName featureName, String accountId);

  void enableAccount(FeatureName featureName, String accountId);

  Optional<FeatureFlag> getFeatureFlag(FeatureName featureName);
}
