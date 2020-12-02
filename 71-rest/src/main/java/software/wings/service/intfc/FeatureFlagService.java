package software.wings.service.intfc;

import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FeatureFlagService {
  boolean isGlobalEnabled(FeatureName featureName);
  boolean isNotGlobalEnabled(FeatureName featureName);
  boolean isEnabled(FeatureName featureName, String accountId);
  boolean isNotEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags();

  List<FeatureFlag> getAllFeatureFlags();

  boolean isEnabledReloadCache(FeatureName featureName, String accountId);

  void enableAccount(FeatureName featureName, String accountId);

  FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled);

  void removeAccountReferenceFromAllFeatureFlags(String accountId);

  Optional<FeatureFlag> getFeatureFlag(FeatureName featureName);

  void enableGlobally(FeatureName featureName);

  Set<String> getAccountIds(FeatureName featureName);

  Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag);
}
