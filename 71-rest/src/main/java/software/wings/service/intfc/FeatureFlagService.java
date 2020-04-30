package software.wings.service.intfc;

import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FeatureFlagService {
  boolean isGlobalEnabled(FeatureName featureName);
  boolean isEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();

  List<FeatureFlag> getAllFeatureFlags();

  boolean isEnabledReloadCache(FeatureName featureName, String accountId);

  void enableAccount(FeatureName featureName, String accountId);

  FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled);

  Optional<FeatureFlag> getFeatureFlag(FeatureName featureName);

  void enableGlobally(FeatureName featureName);

  Set<String> getAccountIds(FeatureName featureName);

  Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag);
}
